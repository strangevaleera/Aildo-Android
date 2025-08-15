package com.xinqi.utils.bt

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import com.xinqi.utils.bt.spec.odm.BluetoothProtocol
import com.xinqi.utils.bt.spec.odm.BroadcastData
import com.xinqi.utils.bt.spec.odm.SvakomPacket
import com.xinqi.utils.common.ioScope
import com.xinqi.utils.log.logI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 蓝牙ViewModel
 * 管理蓝牙状态和操作
 */
class BluetoothViewModel : ViewModel(), AildoBluetoothManager.BluetoothEventListener {
    
    private var bluetoothManager: AildoBluetoothManager? = null
    
    // UI状态数据类
    data class UiState(
        val isBluetoothAvailable: Boolean = false,
        val isScanning: Boolean = false,
        val connectionState: Int = BluetoothProtocol.ConnectionState.STATE_DISCONNECTED,
        val discoveredDevices: List<BluetoothDeviceInfo> = emptyList(),
        val currentDevice: BluetoothDeviceInfo? = null,
        val receivedData: List<String> = emptyList(),
        val sentData: List<String> = emptyList(),
        val errorMessage: String? = null
    )
    
    // 状态流
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // 设备信息缓存
    private val deviceInfoCache = mutableMapOf<String, BluetoothDeviceInfo>()
    
    /**
     * 初始化蓝牙管理器
     */
    fun initializeBluetoothManager(context: android.content.Context) {
        if (bluetoothManager == null) {
            bluetoothManager = AildoBluetoothManager.getInstance(context)
            bluetoothManager?.addEventListener(this)
            updateBluetoothAvailability()
        }
    }
    
    /**
     * 更新蓝牙可用性
     */
    private fun updateBluetoothAvailability() {
        bluetoothManager?.let { manager ->
            _uiState.value = _uiState.value.copy(
                isBluetoothAvailable = manager.isBluetoothAvailable()
            )
        }
    }
    
    /**
     * 开始扫描
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        //todo: viewmodel scope error
        ioScope.launch {
            bluetoothManager?.let { manager ->
                if (manager.startScan()) {
                    logI("扫描已启动")
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "启动扫描失败"
                    )
                }
            }
        }
    }
    
    /**
     * 停止扫描
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        bluetoothManager?.stopScan()
    }
    
    /**
     * 连接设备
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun connectDevice(deviceInfo: BluetoothDeviceInfo) {
        ioScope.launch {
            bluetoothManager?.let  { manager ->
                if (manager.connectDevice(deviceInfo.device)) {
                    _uiState.value = _uiState.value.copy(
                        currentDevice = deviceInfo
                    )
                    logI("开始连接设备: ${deviceInfo.deviceName}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "连接设备失败"
                    )
                }
            }
        }
    }
    
    /**
     * 断开设备连接
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnectDevice() {
        bluetoothManager?.disconnectDevice()
        _uiState.value = _uiState.value.copy(
            currentDevice = null
        )
    }
    
    /**
     * 查询设备信息
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun queryDeviceInfo() {
        bluetoothManager?.queryDeviceInfo()
    }
    
    /**
     * 查询电量状态
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun queryBatteryStatus() {
        bluetoothManager?.queryBatteryStatus()
    }
    
    /**
     * 控制震动
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun controlVibration(motorId: Int, mode: Int, intensity: Int) {
        bluetoothManager?.controlVibration(motorId, mode, intensity)
    }
    
    /**
     * 控制加热
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun controlHeating(status: Int, temperature: Int, heaterId: Int = 0) {
        bluetoothManager?.controlHeating(status, temperature, heaterId)
    }
    
    /**
     * 发送自定义数据包
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun sendCustomPacket(packet: SvakomPacket) {
        bluetoothManager?.sendData(packet)
    }
    
    /**
     * 清除错误消息
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }
    
    /**
     * 清除数据历史
     */
    fun clearDataHistory() {
        _uiState.value = _uiState.value.copy(
            receivedData = emptyList(),
            sentData = emptyList()
        )
    }
    
    // BluetoothEventListener 实现
    
    override fun onDeviceDiscovered(device: BluetoothDevice, broadcastData: BroadcastData?) {
        val deviceInfo = BluetoothDeviceInfo(device, broadcastData)
        deviceInfoCache[device.address] = deviceInfo
        
        val currentList = _uiState.value.discoveredDevices.toMutableList()
        if (!currentList.contains(deviceInfo)) {
            currentList.add(deviceInfo)
            _uiState.value = _uiState.value.copy(
                discoveredDevices = currentList
            )
        }
    }
    
    override fun onScanStarted() {
        _uiState.value = _uiState.value.copy(
            isScanning = true,
            discoveredDevices = emptyList()
        )
        deviceInfoCache.clear()
    }
    
    override fun onScanStopped() {
        _uiState.value = _uiState.value.copy(
            isScanning = false
        )
    }
    
    override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
        _uiState.value = _uiState.value.copy(
            connectionState = state
        )
        
        when (state) {
            BluetoothProtocol.ConnectionState.STATE_CONNECTED -> {
                logI("设备已连接: ${device.address}")
            }
            BluetoothProtocol.ConnectionState.STATE_DISCONNECTED -> {
                logI("设备已断开: ${device.address}")
                _uiState.value = _uiState.value.copy(
                    currentDevice = null
                )
            }
            BluetoothProtocol.ConnectionState.STATE_CONNECTING -> {
                logI("正在连接设备: ${device.address}")
            }
            BluetoothProtocol.ConnectionState.STATE_DISCONNECTING -> {
                logI("正在断开设备: ${device.address}")
            }
        }
    }
    
    override fun onDataReceived(device: BluetoothDevice, data: ByteArray) {
        val dataString = "收到: ${data.joinToString(", ") { "0x${it.toString(16).uppercase()}" }}"
        val currentList = _uiState.value.receivedData.toMutableList()
        currentList.add(dataString)
        if (currentList.size > 100) { // 限制历史记录数量
            currentList.removeAt(0)
        }
        _uiState.value = _uiState.value.copy(
            receivedData = currentList
        )
    }
    
    override fun onDataSent(device: BluetoothDevice, data: ByteArray, success: Boolean) {
        val status = if (success) "成功" else "失败"
        val dataString = "发送($status): ${data.joinToString(", ") { "0x${it.toString(16).uppercase()}" }}"
        val currentList = _uiState.value.sentData.toMutableList()
        currentList.add(dataString)
        if (currentList.size > 100) { // 限制历史记录数量
            currentList.removeAt(0)
        }
        _uiState.value = _uiState.value.copy(
            sentData = currentList
        )
    }
    
    override fun onError(error: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = error
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        bluetoothManager?.removeEventListener(this)
        bluetoothManager = null
    }
}
