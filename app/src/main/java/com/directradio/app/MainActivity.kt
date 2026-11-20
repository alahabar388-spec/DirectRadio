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

private val Navy = Color(0xFF101820)
private val Panel = Color(0xFF1B2B36)
private val Green = Color(0xFF25B879)

class MainActivity : ComponentActivity() {
    private var p2pState by mutableStateOf(P2pUiState())
    private var transportStatus by mutableStateOf("Канал данных ещё не запущен")
    private lateinit var p2p: P2pController
    private lateinit var transport: TcpMessageTransport

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) p2p.discover()
        else p2pState = p2pState.copy(status = "Разрешение не предоставлено. Поиск недоступен")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transport = TcpMessageTransport { message ->
            runOnUiThread { transportStatus = message }
        }
        p2p = P2pController(this) { newState ->
            p2pState = newState
            if (newState.connected) {
                transport.start(newState.groupOwner, newState.groupOwnerAddress)
            } else {
                transport.stop()
            }
        }
        p2p.start()
        setContent {
            DirectRadioApp(
                state = p2pState,
                onDiscover = { discoverWithPermission() },
                onConnect = { p2p.connect(it) },
                dataStatus = transportStatus,
                onTestData = { transport.sendTestMessage() }
            )
        }
    }

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
    onTestData: () -> Unit
) {
    var channel by remember { mutableStateOf("CHANNEL 01") }
    var isPtt by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Navy, surface = Panel)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Navy) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = Green, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("DirectRadio", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Surface(color = Color(0xFF174B38), shape = RoundedCornerShape(20.dp)) {
                        Text("OFFLINE", color = Color(0xFF8EF0BE), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ТЕКУЩИЙ КАНАЛ", color = Color.LightGray, fontSize = 11.sp)
                        Text(channel, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(if (state.connected) "Прямое соединение активно" else "${state.devices.size} устройств обнаружено", color = Color(0xFFB8C7D1), fontSize = 13.sp)
                        if (state.connected) Text(if (state.groupOwner) "Роль: владелец группы" else "Адрес узла: ${state.groupOwnerAddress}", color = Green, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Surface(
                    onClick = { isPtt = !isPtt }, modifier = Modifier.size(148.dp), shape = CircleShape,
                    color = if (isPtt) Color(0xFF9A5A22) else Color(0xFF176B4A), border = BorderStroke(4.dp, Green)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Mic, contentDescription = "PTT", tint = Color.White, modifier = Modifier.size(38.dp))
                        Text("PTT", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text(if (isPtt) "ТЕСТ" else "НАЖМИ И ГОВОРИ", color = Color.White, fontSize = 9.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(state.status, color = Color(0xFFB8C7D1), fontSize = 12.sp)
                Text("КАНАЛ ДАННЫХ: $dataStatus", color = Green, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onTestData,
                    enabled = state.connected,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ОТПРАВИТЬ ТЕСТОВОЕ СООБЩЕНИЕ") }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onDiscover, modifier = Modifier.fillMaxWidth()) { Text("НАЙТИ УСТРОЙСТВА") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { channel = if (channel == "CHANNEL 01") "CHANNEL 02" else "CHANNEL 01" }, modifier = Modifier.fillMaxWidth()) { Text("СМЕНИТЬ КАНАЛ") }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("НАЙДЕННЫЕ УСТРОЙСТВА", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${state.devices.size}", color = Green, fontSize = 12.sp)
                }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(state.devices, key = { it.deviceAddress }) { device ->
                        DeviceRow(device = device, onConnect = { onConnect(device) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("DirectRadio 0.2.0 • Wi-Fi Direct discovery", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: WifiP2pDevice, onConnect: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(device.deviceName.ifBlank { "Android-устройство" }, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(device.deviceAddress, color = Color.LightGray, fontSize = 10.sp)
                Text(device.statusLabel(), color = Green, fontSize = 10.sp)
            }
            Button(onClick = onConnect, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("Связать") }
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