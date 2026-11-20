package com.directradio.app

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Minimal text transport over the Wi-Fi Direct group.
 * The group owner accepts one peer; the client connects to the owner's P2P address.
 */
class TcpMessageTransport(
    private val onStatus: (String) -> Unit
) {
    companion object {
        private const val PORT = 8988
        private const val CONNECT_TIMEOUT_MS = 5_000
    }

    private val executor = Executors.newCachedThreadPool()
    @Volatile private var running = false
    @Volatile private var connected = false
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var peerSocket: Socket? = null
    @Volatile private var writer: BufferedWriter? = null

    @Synchronized
    fun start(isGroupOwner: Boolean, ownerAddress: String) {
        if (running) return
        running = true
        executor.execute {
            if (isGroupOwner) acceptPeer() else connectToOwner(ownerAddress)
        }
    }

    private fun acceptPeer() {
        try {
            ServerSocket(PORT).use { server ->
                serverSocket = server
                if (!running) return
                onStatus("Wi-Fi Direct активен. Ожидаем второй телефон…")
                val socket = server.accept()
                if (!running) {
                    socket.close()
                    return
                }
                establish(socket)
            }
        } catch (e: Exception) {
            if (running) onStatus("Ошибка TCP-сервера: ${e.localizedMessage ?: "неизвестная ошибка"}")
        } finally {
            serverSocket = null
        }
    }

    private fun connectToOwner(ownerAddress: String) {
        if (ownerAddress.isBlank()) {
            onStatus("Не получен адрес владельца группы")
            running = false
            return
        }
        while (running && !connected) {
            try {
                onStatus("Подключаем канал данных…")
                val socket = Socket()
                socket.connect(InetSocketAddress(ownerAddress, PORT), CONNECT_TIMEOUT_MS)
                if (!running) {
                    socket.close()
                    return
                }
                establish(socket)
                return
            } catch (e: Exception) {
                if (running) {
                    onStatus("Ожидаем TCP-сервер второго телефона…")
                    try { Thread.sleep(1_500) } catch (_: InterruptedException) { return }
                }
            }
        }
    }

    private fun establish(socket: Socket) {
        peerSocket = socket
        writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
        connected = true
        onStatus("Канал данных установлен")
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            while (running) {
                val line = reader.readLine() ?: break
                onStatus("Получено сообщение: $line")
            }
        } catch (e: Exception) {
            if (running) onStatus("Ошибка приёма данных: ${e.localizedMessage ?: "соединение прервано"}")
        } finally {
            connected = false
            writer = null
            peerSocket = null
            try { socket.close() } catch (_: Exception) { }
            if (running) onStatus("Канал данных разорван")
        }
    }

    @Synchronized
    fun sendTestMessage() {
        if (!connected) {
            onStatus("Канал данных ещё не готов")
            return
        }
        try {
            writer?.apply {
                write("DirectRadio: тест связи")
                newLine()
                flush()
            }
            onStatus("Тестовое сообщение отправлено")
        } catch (e: Exception) {
            onStatus("Не удалось отправить: ${e.localizedMessage ?: "ошибка"}")
        }
    }

    @Synchronized
    fun stop() {
        running = false
        connected = false
        try { serverSocket?.close() } catch (_: Exception) { }
        try { peerSocket?.close() } catch (_: Exception) { }
        serverSocket = null
        peerSocket = null
        writer = null
    }
}
