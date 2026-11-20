package com.directradio.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF101820)
private val Panel = Color(0xFF1B2B36)
private val Green = Color(0xFF25B879)

data class ChatMessage(
    val id: Long,
    val text: String,
    val incoming: Boolean,
    val time: String
)

class MainActivity : ComponentActivity() {
    private var p2pState by mutableStateOf(P2pUiState())
    private var transportStatus by mutableStateOf("Канал данных ещё не запущен")
    private var chatMessages by mutableStateOf(listOf<ChatMessage>())
    private lateinit var p2p: P2pController
    private lateinit var transport: TcpMessageTransport

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) p2p.discover()
        else p2pState = p2pState.copy(status = "Разрешение не предоставлено. Поиск недоступен")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transport = TcpMessageTransport(
            onStatus = { message -> runOnUiThread { transportStatus = message } },
            onMessage = { text ->
                runOnUiThread {
                    chatMessages = chatMessages + ChatMessage(
                        id = System.nanoTime(), text = text, incoming = true, time = now()
                    )
                }
            }
        )
        p2p = P2pController(this) { newState ->
            p2pState = newState
            if (newState.connected) {
                transport.start(newState.groupOwner, newState.groupOwnerAddress)
            } else {
                transport.stop()
                transportStatus = "Ожидание соединения Wi-Fi Direct"
            }
        }
        p2p.start()
        setContent {
            DirectRadioApp(
                state = p2pState,
                onDiscover = { discoverWithPermission() },
                onConnect = { p2p.connect(it) },
                dataStatus = transportStatus,
                messages = chatMessages,
                onSend = { text ->
                    if (transport.sendMessage(text)) {
                        chatMessages = chatMessages + ChatMessage(
                            id = System.nanoTime(), text = text, incoming = false, time = now()
                        )
                        true
                    } else false
                }
            )
        }
    }

    private fun now(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    private fun discoverWithPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.NEARBY_WIFI_DEVICES else Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            p2p.discover()
        } else {
            permissionLauncher.launch(arrayOf(permission))
        }
    }

    override fun onDestroy() {
        p2p.stop()
        transport.stop()
        super.onDestroy()
    }
}

@Composable
private fun DirectRadioApp(
    state: P2pUiState,
    onDiscover: () -> Unit,
    onConnect: (WifiP2pDevice) -> Unit,
    dataStatus: String,
    messages: List<ChatMessage>,
    onSend: (String) -> Boolean
) {
    var channel by remember { mutableStateOf("CHANNEL 01") }
    var isPtt by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Navy, surface = Panel)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Navy) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = Green, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("DirectRadio Messenger", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Surface(color = Color(0xFF174B38), shape = RoundedCornerShape(20.dp)) {
                        Text("OFFLINE", color = Color(0xFF8EF0BE), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 10.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("СЕТЬ • $channel", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(if (state.connected) "P2P-соединение установлено" else "${state.devices.size} устройств обнаружено", color = Color(0xFFB8C7D1), fontSize = 12.sp)
                        Text("Канал данных: $dataStatus", color = Green, fontSize = 11.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onDiscover, modifier = Modifier.weight(1f)) { Text("Найти") }
                            OutlinedButton(onClick = { channel = if (channel == "CHANNEL 01") "CHANNEL 02" else "CHANNEL 01" }) { Text("Канал") }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("УЧАСТНИКИ ПОБЛИЗОСТИ", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 112.dp)) {
                    items(state.devices, key = { it.deviceAddress }) { device ->
                        DeviceRow(device = device, onConnect = { onConnect(device) })
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ПЕРЕПИСКА", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${messages.size} сообщений", color = Green, fontSize = 10.sp)
                }
                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Panel)
                ) {
                    if (messages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Сообщений пока нет. Подключите участника.", color = Color.LightGray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                            reverseLayout = true,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages.asReversed(), key = { it.id }) { message ->
                                MessageBubble(message)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 4000) draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Сообщение…") },
                        enabled = state.connected,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val text = draft.trim()
                            if (text.isNotEmpty()) {
                                if (onSend(text)) draft = ""
                            }
                        },
                        enabled = state.connected && draft.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Отправить", tint = Green)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { isPtt = !isPtt },
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = if (isPtt) Color(0xFF9A5A22) else Color(0xFF176B4A),
                        border = BorderStroke(2.dp, Green)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Mic, contentDescription = "PTT", tint = Color.White)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(if (isPtt) "PTT: демонстрационный режим" else "Голосовая связь пока не реализована", color = Color.Gray, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.incoming) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            color = if (message.incoming) Color(0xFF263B49) else Color(0xFF145B42),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(message.text, color = Color.White, fontSize = 14.sp)
                Text(message.time, color = Color(0xFFB8C7D1), fontSize = 9.sp, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

@Composable
private fun DeviceRow(device: WifiP2pDevice, onConnect: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(device.deviceName.ifBlank { "Android-устройство" }, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(device.statusLabel(), color = Green, fontSize = 10.sp)
            }
            Button(onClick = onConnect, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("Связать") }
        }
    }
}

private fun WifiP2pDevice.statusLabel(): String = when (status) {
    WifiP2pDevice.AVAILABLE -> "Доступно"
    WifiP2pDevice.INVITED -> "Приглашение отправлено"
    WifiP2pDevice.CONNECTED -> "Подключено"
    WifiP2pDevice.FAILED -> "Ошибка"
    WifiP2pDevice.UNAVAILABLE -> "Недоступно"
    else -> "Статус неизвестен"
}
