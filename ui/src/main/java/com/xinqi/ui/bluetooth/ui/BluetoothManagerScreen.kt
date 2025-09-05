package com.xinqi.ui.bluetooth.ui

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
import com.xinqi.ui.bluetooth.BluetoothManagerViewModel
import com.xinqi.utils.bt.AildoBluetoothManager
import com.xinqi.utils.log.logI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothManagerScreen(
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { BluetoothManagerViewModel(context) }
    
    val bluetoothState by viewModel.bluetoothState.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部标题栏
        TopAppBar(
            title = { Text("蓝牙管理") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 蓝牙状态卡片
        BluetoothStatusCard(
            bluetoothState = bluetoothState,
            onRequestPermissions = onRequestPermissions,
            onEnableBluetooth = onEnableBluetooth
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 连接状态卡片
        ConnectionStatusCard(
            connectionState = connectionState,
            onDisconnect = { viewModel.disconnect() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 设备扫描控制
        ScanControlCard(
            isScanning = isScanning,
            onStartScan = { viewModel.startScan() },
            onStopScan = { viewModel.stopScan() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 设备列表
        DeviceListCard(
            devices = devices,
            onDeviceClick = { device ->
                viewModel.connectToDevice(device.address)
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 测试控制
        TestControlCard(
            onVibrationTest = { viewModel.testVibration() },
            onHeatingTest = { viewModel.testHeating() }
        )
    }
}

@Composable
private fun BluetoothStatusCard(
    bluetoothState: BluetoothManagerViewModel.BluetoothState,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "蓝牙状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (bluetoothState) {
                        BluetoothManagerViewModel.BluetoothState.ENABLED -> Icons.Default.Bluetooth
                        BluetoothManagerViewModel.BluetoothState.DISABLED -> Icons.Default.BluetoothDisabled
                        BluetoothManagerViewModel.BluetoothState.NO_PERMISSION -> Icons.Default.Security
                        BluetoothManagerViewModel.BluetoothState.NOT_SUPPORTED -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when (bluetoothState) {
                        BluetoothManagerViewModel.BluetoothState.ENABLED -> MaterialTheme.colorScheme.primary
                        BluetoothManagerViewModel.BluetoothState.DISABLED -> MaterialTheme.colorScheme.error
                        BluetoothManagerViewModel.BluetoothState.NO_PERMISSION -> MaterialTheme.colorScheme.error
                        BluetoothManagerViewModel.BluetoothState.NOT_SUPPORTED -> MaterialTheme.colorScheme.error
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = when (bluetoothState) {
                        BluetoothManagerViewModel.BluetoothState.ENABLED -> "蓝牙已启用"
                        BluetoothManagerViewModel.BluetoothState.DISABLED -> "蓝牙未启用"
                        BluetoothManagerViewModel.BluetoothState.NO_PERMISSION -> "缺少权限"
                        BluetoothManagerViewModel.BluetoothState.NOT_SUPPORTED -> "不支持蓝牙"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when (bluetoothState) {
                BluetoothManagerViewModel.BluetoothState.DISABLED -> {
                    Button(
                        onClick = onEnableBluetooth,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("启用蓝牙")
                    }
                }
                BluetoothManagerViewModel.BluetoothState.NO_PERMISSION -> {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("请求权限")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: BluetoothManagerViewModel.ConnectionState,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "连接状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (connectionState) {
                        is BluetoothManagerViewModel.ConnectionState.Connected -> Icons.Default.CheckCircle
                        is BluetoothManagerViewModel.ConnectionState.Connecting -> Icons.Default.Sync
                        is BluetoothManagerViewModel.ConnectionState.Disconnected -> Icons.Default.Cancel
                        is BluetoothManagerViewModel.ConnectionState.Error -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when (connectionState) {
                        is BluetoothManagerViewModel.ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                        is BluetoothManagerViewModel.ConnectionState.Connecting -> MaterialTheme.colorScheme.secondary
                        is BluetoothManagerViewModel.ConnectionState.Disconnected -> MaterialTheme.colorScheme.outline
                        is BluetoothManagerViewModel.ConnectionState.Error -> MaterialTheme.colorScheme.error
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = when (connectionState) {
                        is BluetoothManagerViewModel.ConnectionState.Connected -> "已连接到 ${connectionState.deviceName}"
                        is BluetoothManagerViewModel.ConnectionState.Connecting -> "连接中..."
                        is BluetoothManagerViewModel.ConnectionState.Disconnected -> "未连接"
                        is BluetoothManagerViewModel.ConnectionState.Error -> "连接错误: ${connectionState.error}"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (connectionState is BluetoothManagerViewModel.ConnectionState.Connected) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("断开连接")
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
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "设备扫描",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isScanning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正在扫描设备...")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onStopScan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("停止扫描")
                }
            } else {
                Button(
                    onClick = onStartScan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("开始扫描")
                }
            }
        }
    }
}

@Composable
private fun DeviceListCard(
    devices: List<BluetoothManagerViewModel.BluetoothDevice>,
    onDeviceClick: (BluetoothManagerViewModel.BluetoothDevice) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "可用设备 (${devices.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (devices.isEmpty()) {
                Text(
                    text = "未发现设备",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices) { device ->
                        DeviceItem(
                            device = device,
                            onClick = { onDeviceClick(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: BluetoothManagerViewModel.BluetoothDevice,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name ?: "未知设备",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (device.isConnected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已连接",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TestControlCard(
    onVibrationTest: () -> Unit,
    onHeatingTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "功能测试",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onVibrationTest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("震动测试")
                }
                
                Button(
                    onClick = onHeatingTest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("加热测试")
                }
            }
        }
    }
}

