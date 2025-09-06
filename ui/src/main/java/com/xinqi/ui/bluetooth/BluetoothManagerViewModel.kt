package com.xinqi.ui.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xinqi.utils.bt.AildoBluetoothManager
import com.xinqi.utils.bt.spec.odm.BroadcastData
import com.xinqi.utils.log.logI
import com.xinqi.utils.log.logE
import com.xinqi.utils.mcp.MCPIntegrationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class BluetoothManagerViewModel(private val context: Context) : ViewModel() {
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val aildoBluetoothManager = AildoBluetoothManager.getInstance(context)
    private val mcpManager = MCPIntegrationManager.getInstance(context)
    
    // 蓝牙状态
    private val _bluetoothState = MutableStateFlow(BluetoothState.DISABLED)
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()
    
    // 扫描状态
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    // 设备列表
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()
    
    // 连接状态
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    init {
        updateBluetoothState()
        observeBluetoothConnection()
    }
    
    private fun updateBluetoothState() {
        when {
            bluetoothAdapter == null -> {
                _bluetoothState.value = BluetoothState.NOT_SUPPORTED
            }
            !hasBluetoothPermissions() -> {
                _bluetoothState.value = BluetoothState.NO_PERMISSION
            }
            !bluetoothAdapter.isEnabled -> {
                _bluetoothState.value = BluetoothState.DISABLED
            }
            else -> {
                _bluetoothState.value = BluetoothState.ENABLED
            }
        }
    }
    
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    private fun observeBluetoothConnection() {
        viewModelScope.launch {
            // 监听AildoBluetoothManager的连接状态
            while (true) {
                try {
                    val isConnected = aildoBluetoothManager.isConnected()
                    val connectedDevice = aildoBluetoothManager.getCurrentDevice()
                    
                    _connectionState.value = if (isConnected && connectedDevice != null) {
                        ConnectionState.Connected(connectedDevice.name ?: "未知设备")
                    } else {
                        ConnectionState.Disconnected
                    }
                } catch (e: Exception) {
                    logE("观察蓝牙连接状态时出错: ${e.message}")
                }
                
                delay(1000) // 每秒检查一次
            }
        }
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        if (_bluetoothState.value != BluetoothState.ENABLED) {
            logE("蓝牙未启用，无法扫描")
            return
        }
        
        viewModelScope.launch {
            try {
                _isScanning.value = true
                _devices.value = emptyList()
                
                logI("开始扫描蓝牙设备")
                
                // 添加事件监听器来接收扫描结果
                val scanListener = object : AildoBluetoothManager.BluetoothEventListener {
                    override fun onDeviceDiscovered(
                        device: android.bluetooth.BluetoothDevice,
                        broadcastData: BroadcastData?
                    ) {
                        logI("发现设备: ${device.name} - ${device.address}")

                        // 更新设备列表
                        val currentDevices = _devices.value.toMutableList()
                        val existingIndex = currentDevices.indexOfFirst { it.address == device.address }

                        val bluetoothDevice = BluetoothDevice(
                            address = device.address,
                            name = device.name,
                            isConnected = false
                        )

                        if (existingIndex >= 0) {
                            currentDevices[existingIndex] = bluetoothDevice
                        } else {
                            currentDevices.add(bluetoothDevice)
                        }

                        _devices.value = currentDevices
                    }
                    
                    override fun onScanStarted() {
                        logI("扫描已开始")
                    }
                    
                    override fun onScanStopped() {
                        logI("扫描已停止")
                        _isScanning.value = false
                    }
                    
                    override fun onConnectionStateChanged(device: android.bluetooth.BluetoothDevice, state: Int) {
                        // 更新连接状态
                        val currentDevices = _devices.value.toMutableList()
                        val index = currentDevices.indexOfFirst { it.address == device.address }
                        if (index >= 0) {
                            currentDevices[index] = currentDevices[index].copy(isConnected = state == BluetoothProfile.STATE_CONNECTED)
                            _devices.value = currentDevices
                        }
                    }
                    
                    override fun onDataReceived(device: android.bluetooth.BluetoothDevice, data: ByteArray) {
                        // 处理接收到的数据
                    }
                    
                    override fun onDataSent(device: android.bluetooth.BluetoothDevice, data: ByteArray, success: Boolean) {
                        // 处理数据发送结果
                    }
                    
                    override fun onError(error: String) {
                        logE("蓝牙错误: $error")
                        _isScanning.value = false
                    }
                }
                
                aildoBluetoothManager.addEventListener(scanListener)
                
                // 使用AildoBluetoothManager进行扫描
                val scanStarted = aildoBluetoothManager.startScan()
                if (!scanStarted) {
                    logE("启动扫描失败")
                    _isScanning.value = false
                    return@launch
                }
                
                // 扫描30秒后自动停止
                delay(30000)
                stopScan()
                
                // 移除事件监听器
                aildoBluetoothManager.removeEventListener(scanListener)
                
            } catch (e: Exception) {
                logE("扫描设备时出错: ${e.message}")
                _isScanning.value = false
            }
        }
    }
    
    fun stopScan() {
        viewModelScope.launch {
            try {
                aildoBluetoothManager.stopScan()
                _isScanning.value = false
                logI("停止扫描蓝牙设备")
            } catch (e: Exception) {
                logE("停止扫描时出错: ${e.message}")
            }
        }
    }
    
    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.Connecting
                logI("尝试连接到设备: $deviceAddress")
                
                // 从已发现的设备中找到目标设备
                val discoveredDevices = aildoBluetoothManager.getDiscoveredDevices()
                val targetDevice = discoveredDevices.find { it.address == deviceAddress }
                
                if (targetDevice == null) {
                    _connectionState.value = ConnectionState.Error("设备未找到，请先扫描")
                    logE("设备未找到: $deviceAddress")
                    return@launch
                }
                
                val result = aildoBluetoothManager.connectDevice(targetDevice)
                
                if (result) {
                    logI("开始连接设备: $deviceAddress")
                    // 连接状态会通过observeBluetoothConnection更新
                } else {
                    _connectionState.value = ConnectionState.Error("连接失败")
                    logE("连接设备失败: $deviceAddress")
                }
                
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error(e.message ?: "连接异常")
                logE("连接设备时出错: ${e.message}")
            }
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            try {
                aildoBluetoothManager.disconnectDevice()
                _connectionState.value = ConnectionState.Disconnected
                logI("断开蓝牙连接")
            } catch (e: Exception) {
                logE("断开连接时出错: ${e.message}")
            }
        }
    }
    
    fun testVibration() {
        viewModelScope.launch {
            try {
                logI("测试震动功能")
                
                // 使用AildoBluetoothManager直接发送震动命令
                val result = aildoBluetoothManager.controlVibration(
                    motorId = 0,
                    mode = 1,
                    intensity = 80
                )
                
                if (result) {
                    logI("震动测试命令发送成功")
                } else {
                    logE("震动测试命令发送失败")
                }
                
            } catch (e: Exception) {
                logE("震动测试时出错: ${e.message}")
            }
        }
    }
    
    fun testHeating() {
        viewModelScope.launch {
            try {
                logI("测试加热功能")
                
                // 使用AildoBluetoothManager直接发送加热命令
                val result = aildoBluetoothManager.controlHeating(
                    status = 1, // 开启加热
                    temperature = 40, // 40度
                    heaterId = 0
                )
                
                if (result) {
                    logI("加热测试命令发送成功")
                } else {
                    logE("加热测试命令发送失败")
                }
                
            } catch (e: Exception) {
                logE("加热测试时出错: ${e.message}")
            }
        }
    }
    
    fun refreshBluetoothState() {
        updateBluetoothState()
    }
    
    // 数据类定义
    data class BluetoothDevice(
        val address: String,
        val name: String?,
        val isConnected: Boolean = false
    )
    
    enum class BluetoothState {
        ENABLED,
        DISABLED,
        NO_PERMISSION,
        NOT_SUPPORTED
    }
    
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val deviceName: String) : ConnectionState()
        data class Error(val error: String) : ConnectionState()
    }
}
