package com.directradio.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

data class P2pUiState(
    val status: String = "Готов к поиску устройств",
    val devices: List<WifiP2pDevice> = emptyList(),
    val connected: Boolean = false,
    val groupOwner: Boolean = false,
    val groupOwnerAddress: String = ""
)

/** Wi-Fi Direct discovery and connection controller. Audio transport is added in a later stage. */
class P2pController(
    private val activity: ComponentActivity,
    private val onState: (P2pUiState) -> Unit
) {
    private val manager = activity.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = manager.initialize(activity, activity.mainLooper, null)
    private var state = P2pUiState()
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val enabled = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1) == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    update(status = if (enabled) "Wi-Fi Direct включён" else "Включите Wi-Fi на телефоне")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> requestPeers()
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> requestConnectionInfo()
            }
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            activity.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            activity.registerReceiver(receiver, filter)
        }
        registered = true
        update(status = "Wi-Fi Direct готов. Нажмите «Найти устройства»")
    }

    fun discover() {
        if (!hasRequiredPermission()) {
            update(status = "Нужно разрешение на поиск устройств поблизости")
            return
        }
        update(status = "Ищем устройства поблизости…")
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { update(status = "Поиск запущен. Ожидаем устройства…") }
            override fun onFailure(reason: Int) { update(status = "Не удалось начать поиск: ${reasonText(reason)}") }
        })
    }

    fun connect(device: WifiP2pDevice) {
        if (!hasRequiredPermission()) {
            update(status = "Нет разрешения для подключения")
            return
        }
        val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
        update(status = "Подключаемся к ${device.deviceName}…")
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { update(status = "Запрос подключения отправлен") }
            override fun onFailure(reason: Int) { update(status = "Ошибка подключения: ${reasonText(reason)}") }
        })
    }

    private fun requestPeers() {
        if (!hasRequiredPermission()) return
        manager.requestPeers(channel) { list ->
            state = state.copy(devices = list.deviceList.toList())
            onState(state)
            if (list.deviceList.isEmpty() && !state.connected) update(status = "Устройства не найдены. Повторите поиск")
        }
    }

    private fun requestConnectionInfo() {
        if (!hasRequiredPermission()) return
        manager.requestConnectionInfo(channel) { info: WifiP2pInfo ->
            val connected = info.groupFormed
            val address = info.groupOwnerAddress?.hostAddress.orEmpty()
            state = state.copy(
                connected = connected,
                groupOwner = connected && info.isGroupOwner,
                groupOwnerAddress = address
            )
            update(status = if (connected) "Прямое соединение установлено" else "Соединение не установлено")
        }
    }

    private fun hasRequiredPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.NEARBY_WIFI_DEVICES else Manifest.permission.ACCESS_FINE_LOCATION
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun reasonText(reason: Int): String = when (reason) {
        WifiP2pManager.P2P_UNSUPPORTED -> "Wi-Fi Direct не поддерживается"
        WifiP2pManager.BUSY -> "Система занята, попробуйте позже"
        WifiP2pManager.ERROR -> "внутренняя ошибка Wi-Fi Direct"
        else -> "код $reason"
    }

    private fun update(status: String) {
        state = state.copy(status = status)
        onState(state)
    }

    fun stop() {
        if (registered) {
            try { activity.unregisterReceiver(receiver) } catch (_: IllegalArgumentException) { }
            registered = false
        }
    }
}