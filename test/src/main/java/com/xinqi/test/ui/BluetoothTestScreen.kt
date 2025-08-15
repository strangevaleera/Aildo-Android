package com.xinqi.test.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xinqi.test.ui.components.*
import com.xinqi.utils.bt.BluetoothDeviceInfo
import com.xinqi.utils.bt.BluetoothViewModel
import com.xinqi.utils.bt.spec.odm.BluetoothProtocol
import com.xinqi.utils.bt.spec.odm.SvakomPacket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothTestScreen(
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    viewModel: BluetoothViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // 初始化蓝牙管理器
    LaunchedEffect(Unit) {
        viewModel.initializeBluetoothManager(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("蓝牙协议测试") },
                actions = {
                    IconButton(onClick = { viewModel.clearDataHistory() }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除历史")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 状态信息
            item {
                StatusCard(uiState, onRequestPermissions, onEnableBluetooth)
            }
            
            // 扫描控制
            item {
                ScanControlCard(
                    isScanning = uiState.isScanning,
                    onStartScan = { viewModel.startScan() },
                    onStopScan = { viewModel.stopScan() }
                )
            }
            
            // 设备列表
            if (uiState.discoveredDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "发现的设备 (${uiState.discoveredDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(uiState.discoveredDevices) { deviceInfo ->
                    DeviceCard(
                        deviceInfo = deviceInfo,
                        isConnected = uiState.currentDevice?.deviceAddress == deviceInfo.deviceAddress,
                        onConnect = { viewModel.connectDevice(deviceInfo) },
                        onDisconnect = { viewModel.disconnectDevice() }
                    )
                }
            }
            
            // 连接状态
            if (uiState.currentDevice != null) {
                item {
                    ConnectionStatusCard(
                        deviceInfo = uiState.currentDevice!!,
                        connectionState = uiState.connectionState
                    )
                }
                
                // 控制面板
                item {
                    ControlPanelCard(
                        onQueryDeviceInfo = { viewModel.queryDeviceInfo() },
                        onQueryBatteryStatus = { viewModel.queryBatteryStatus() },
                        onControlVibration = { motorId, mode, intensity ->
                            viewModel.controlVibration(motorId, mode, intensity)
                        },
                        onControlHeating = { status, temperature, heaterId ->
                            viewModel.controlHeating(status, temperature, heaterId)
                        },
                        onSendCustomPacket = { packet ->
                            viewModel.sendCustomPacket(packet)
                        }
                    )
                }
            }
            
            // 数据通信
            if (uiState.currentDevice != null) {
                item {
                    DataCommunicationCard(
                        receivedData = uiState.receivedData,
                        sentData = uiState.sentData
                    )
                }
            }
            
            // 错误消息
            uiState.errorMessage?.let { error ->
                item {
                    ErrorCard(
                        error = error,
                        onDismiss = { viewModel.clearErrorMessage() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    uiState: BluetoothViewModel.UiState,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "蓝牙状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("蓝牙可用:")
                Text(
                    text = if (uiState.isBluetoothAvailable) "是" else "否",
                    color = if (uiState.isBluetoothAvailable) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("连接状态:")
                Text(
                    text = when (uiState.connectionState) {
                        BluetoothProtocol.ConnectionState.STATE_DISCONNECTED -> "未连接"
                        BluetoothProtocol.ConnectionState.STATE_CONNECTING -> "连接中"
                        BluetoothProtocol.ConnectionState.STATE_CONNECTED -> "已连接"
                        BluetoothProtocol.ConnectionState.STATE_DISCONNECTING -> "断开中"
                        else -> "未知"
                    },
                    color = when (uiState.connectionState) {
                        BluetoothProtocol.ConnectionState.STATE_CONNECTED -> MaterialTheme.colorScheme.primary
                        BluetoothProtocol.ConnectionState.STATE_CONNECTING -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            
            if (!uiState.isBluetoothAvailable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onEnableBluetooth,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("启用蓝牙")
                    }
                    
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("请求权限")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanControlCard(
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "设备扫描",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartScan,
                    enabled = !isScanning,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (isScanning) "扫描中..." else "开始扫描")
                }
                
                Button(
                    onClick = onStopScan,
                    enabled = isScanning,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("停止扫描")
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    deviceInfo: BluetoothDeviceInfo,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deviceInfo.deviceName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = deviceInfo.deviceAddress,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (deviceInfo.isSVAKOMDevice) {
                        Text(
                            text = "${deviceInfo.companyName} ${deviceInfo.protocolVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "UID: ${deviceInfo.uid}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Button(
                    onClick = if (isConnected) onDisconnect else onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isConnected) "断开" else "连接")
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    deviceInfo: BluetoothDeviceInfo,
    connectionState: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "连接状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text("设备: ${deviceInfo.deviceName}")
            Text("地址: ${deviceInfo.deviceAddress}")
            Text(
                text = "状态: ${
                    when (connectionState) {
                        BluetoothProtocol.ConnectionState.STATE_DISCONNECTED -> "未连接"
                        BluetoothProtocol.ConnectionState.STATE_CONNECTING -> "连接中"
                        BluetoothProtocol.ConnectionState.STATE_CONNECTED -> "已连接"
                        BluetoothProtocol.ConnectionState.STATE_DISCONNECTING -> "断开中"
                        else -> "未知"
                    }
                }",
                color = when (connectionState) {
                    BluetoothProtocol.ConnectionState.STATE_CONNECTED -> MaterialTheme.colorScheme.primary
                    BluetoothProtocol.ConnectionState.STATE_CONNECTING -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun ControlPanelCard(
    onQueryDeviceInfo: () -> Unit,
    onQueryBatteryStatus: () -> Unit,
    onControlVibration: (Int, Int, Int) -> Unit,
    onControlHeating: (Int, Int, Int) -> Unit,
    onSendCustomPacket: (SvakomPacket) -> Unit
) {
    var showVibrationDialog by remember { mutableStateOf(false) }
    var showHeatingDialog by remember { mutableStateOf(false) }
    var showCustomPacketDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "控制面板",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // 查询按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQueryDeviceInfo,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("设备信息")
                }
                
                Button(
                    onClick = onQueryBatteryStatus,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("电量状态")
                }
            }
            
            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showVibrationDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Face, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("震动控制")
                }
                
                Button(
                    onClick = { showHeatingDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("加热控制")
                }
            }
            
            // 自定义数据包
            Button(
                onClick = { showCustomPacketDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("发送自定义数据包")
            }
        }
    }
    
    // 震动控制对话框
    if (showVibrationDialog) {
        VibrationControlDialog(
            onDismiss = { showVibrationDialog = false },
            onConfirm = { motorId, mode, intensity ->
                onControlVibration(motorId, mode, intensity)
                showVibrationDialog = false
            }
        )
    }
    
    // 加热控制对话框
    if (showHeatingDialog) {
        HeatingControlDialog(
            onDismiss = { showHeatingDialog = false },
            onConfirm = { status, temperature, heaterId ->
                onControlHeating(status, temperature, heaterId)
                showHeatingDialog = false
            }
        )
    }
    
    // 自定义数据包对话框
    if (showCustomPacketDialog) {
        CustomPacketDialog(
            onDismiss = { showCustomPacketDialog = false },
            onConfirm = { packet ->
                onSendCustomPacket(packet)
                showCustomPacketDialog = false
            }
        )
    }
}

@Composable
private fun DataCommunicationCard(
    receivedData: List<String>,
    sentData: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "数据通信",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // 发送的数据
            Text(
                text = "发送的数据 (${sentData.size}):",
                style = MaterialTheme.typography.titleSmall
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 150.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sentData.reversed()) { data ->
                    Text(
                        text = data,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            Divider()
            
            // 接收的数据
            Text(
                text = "接收的数据 (${receivedData.size}):",
                style = MaterialTheme.typography.titleSmall
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 150.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(receivedData.reversed()) { data ->
                    Text(
                        text = data,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
