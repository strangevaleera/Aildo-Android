package com.xinqi.utils.bt

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.xinqi.utils.bt.spec.odm.BluetoothProtocol
import com.xinqi.utils.bt.spec.odm.BroadcastData
import com.xinqi.utils.bt.spec.odm.HeatingControl
import com.xinqi.utils.bt.spec.odm.SvakomBtCodec
import com.xinqi.utils.bt.spec.odm.SvakomPacket
import com.xinqi.utils.bt.spec.odm.VibrationControl
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 蓝牙管理器
 * 实现基于BluetoothProtocol协议的蓝牙扫描、绑定、连接和通信功能
 */
class AildoBluetoothManager private constructor(private val context: Context) {
    
    companion object Companion {
        private const val TAG = "BluetoothManager"
        private const val SCAN_TIMEOUT = 10000L // 扫描超时时间 10秒
        private const val CONNECTION_TIMEOUT = 15000L // 连接超时时间 15秒
        private const val WRITE_TIMEOUT = 5000L // 写入超时时间 5秒
        
        @Volatile
        private var INSTANCE: AildoBluetoothManager? = null
        
        fun getInstance(context: Context): AildoBluetoothManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AildoBluetoothManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    
    //扫描
    private var isScanning = false
    private var scanJob: Job? = null
    private val discoveredDevices = ConcurrentHashMap<String, BluetoothDevice>()
    
    //连接
    private var currentGatt: BluetoothGatt? = null
    private var currentDevice: BluetoothDevice? = null
    private var connectionState = BluetoothProtocol.ConnectionState.STATE_DISCONNECTED
    
    //特征值
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    
    //回调
    private var scanCallback: ScanCallback? = null
    private var gattCallback: BluetoothGattCallback? = null
    
    //事件监听器
    private val eventListeners = mutableListOf<BluetoothEventListener>()
    
    private val scope = CoroutineScope(Dispatchers.Main)
    
    /**
     * 蓝牙事件监听器接口
     */
    interface BluetoothEventListener {
        fun onDeviceDiscovered(device: BluetoothDevice, broadcastData: BroadcastData?)
        fun onScanStarted()
        fun onScanStopped()
        fun onConnectionStateChanged(device: BluetoothDevice, state: Int)
        fun onDataReceived(device: BluetoothDevice, data: ByteArray)
        fun onDataSent(device: BluetoothDevice, data: ByteArray, success: Boolean)
        fun onError(error: String)
    }

    fun addEventListener(listener: BluetoothEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }

    fun removeEventListener(listener: BluetoothEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * 检查蓝牙是否可用
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * 开始扫描
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan(): Boolean {
        if (!isBluetoothAvailable()) {
            notifyError("蓝牙未启用")
            return false
        }
        
        if (isScanning) {
            logI("扫描已在进行中")
            return false
        }
        
        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                notifyError("缺少蓝牙扫描权限")
                return false
            }
        }
        
        try {
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
            if (bluetoothLeScanner == null) {
                notifyError("不支持BLE扫描")
                return false
            }
            
            //创建扫描过滤器，只扫描XQT1设备
            val scanFilter = ScanFilter.Builder()
                .setManufacturerData(
                    BluetoothProtocol.CompanyId.XIN_QI,
                    byteArrayOf(0x58, 0x51, 0x54, 0x31), // "XQT1"
                    (0..3).map { it.toByte() }.toByteArray()
                )
                .build()
            
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()
            
            scanCallback = object : ScanCallback() {
                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    super.onScanResult(callbackType, result)
                    handleScanResult(result)
                }
                
                @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
                override fun onScanFailed(errorCode: Int) {
                    super.onScanFailed(errorCode)
                    logE("扫描失败: $errorCode")
                    stopScan()
                    notifyError("扫描失败: $errorCode")
                }
            }
            
            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            isScanning = true
            discoveredDevices.clear()
            
            notifyScanStarted()
            
            //设置扫描超时
            scanJob = scope.launch {
                delay(SCAN_TIMEOUT)
                stopScan()
            }
            
            logI("开始扫描")
            return true
            
        } catch (e: Exception) {
            logE("启动扫描失败: ${e.message}")
            notifyError("启动扫描失败: ${e.message}")
            return false
        }
    }
    
    /**
     * 停止扫描
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        if (!isScanning) return
        
        try {
            scanJob?.cancel()
            scanJob = null

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                logE("doesnt have BLUETOOTH_SCAN permission")
                return
            } else {
                bluetoothLeScanner?.stopScan(scanCallback)
                scanCallback = null

                isScanning = false
                notifyScanStopped()

                logI("停止扫描")
            }
        } catch (e: Exception) {
            logE("停止扫描失败: ${e.message}")
        }
    }
    
    /**
     * 处理扫描结果
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val deviceAddress = device.address
        
        if (discoveredDevices.containsKey(deviceAddress)) {
            return // 设备已发现
        }
        
        discoveredDevices[deviceAddress] = device
        
        // 解析广播数据
        val broadcastData = parseManufacturerData(result.scanRecord?.manufacturerSpecificData as Map<Int, ByteArray>?)
        
        logI("发现设备: ${device.name ?: "未知"} (${deviceAddress})")
        notifyDeviceDiscovered(device, broadcastData)
    }
    
    /**
     * 解析厂商数据
     */
    private fun parseManufacturerData(manufacturerData: Map<Int, ByteArray>?): BroadcastData? {
        if (manufacturerData == null) return null
        
        val data = manufacturerData[BluetoothProtocol.CompanyId.XIN_QI]
        return if (data != null) {
            SvakomBtCodec.parseBroadcastData(data)
        } else null
    }
    
    /**
     * 连接设备
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectDevice(device: BluetoothDevice): Boolean {
        if (connectionState != BluetoothProtocol.ConnectionState.STATE_DISCONNECTED) {
            logI("已有连接，请先断开")
            return false
        }
        
        try {
            currentDevice = device
            connectionState = BluetoothProtocol.ConnectionState.STATE_CONNECTING
            
            gattCallback = object : BluetoothGattCallback() {
                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    super.onConnectionStateChange(gatt, status, newState)
                    handleConnectionStateChange(gatt, status, newState)
                }
                
                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    super.onServicesDiscovered(gatt, status)
                    handleServicesDiscovered(gatt, status)
                }
                
                override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                    super.onCharacteristicRead(gatt, characteristic, status)
                    handleCharacteristicRead(gatt, characteristic, status)
                }
                
                override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                    super.onCharacteristicWrite(gatt, characteristic, status)
                    handleCharacteristicWrite(gatt, characteristic, status)
                }
                
                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                    super.onCharacteristicChanged(gatt, characteristic, value)
                    handleCharacteristicChanged(gatt, characteristic, value)
                }
            }
            
            currentGatt = device.connectGatt(context, false, gattCallback)
            
            // 设置连接超时
            scope.launch {
                delay(CONNECTION_TIMEOUT)
                if (connectionState == BluetoothProtocol.ConnectionState.STATE_CONNECTING) {
                    logE("连接超时")
                    disconnectDevice()
                    notifyError("连接超时")
                }
            }
            
            logI("开始连接设备: ${device.address}")
            return true
            
        } catch (e: Exception) {
            logE("连接设备失败: ${e.message}")
            connectionState = BluetoothProtocol.ConnectionState.STATE_DISCONNECTED
            currentDevice = null
            notifyError("连接设备失败: ${e.message}")
            return false
        }
    }
    
    /**
     * 断开设备连接
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnectDevice() {
        try {
            currentGatt?.disconnect()
            currentGatt?.close()
            currentGatt = null
            
            currentDevice = null
            connectionState = BluetoothProtocol.ConnectionState.STATE_DISCONNECTED
            
            writeCharacteristic = null
            readCharacteristic = null
            
            logI("设备已断开")
            
        } catch (e: Exception) {
            logE("断开设备失败: ${e.message}")
        }
    }
    
    /**
     * 处理连接状态变化
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        val device = gatt.device
        
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
                logI("设备已连接: ${device.address}")
                connectionState = BluetoothProtocol.ConnectionState.STATE_CONNECTED
                notifyConnectionStateChanged(device, newState)
                
                // 发现服务
                gatt.discoverServices()
            }
            
            BluetoothProfile.STATE_DISCONNECTED -> {
                logI("设备已断开: ${device.address}")
                connectionState = BluetoothProtocol.ConnectionState.STATE_DISCONNECTED
                notifyConnectionStateChanged(device, newState)
                
                currentGatt = null
                currentDevice = null
                writeCharacteristic = null
                readCharacteristic = null
            }
        }
    }
    
    /**
     * 处理服务发现
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            logE("服务发现失败: $status")
            return
        }
        
        val service = gatt.getService(UUID.fromString(BluetoothProtocol.UUID.SERVICE_UUID))
        if (service == null) {
            logE("未找到目标服务")
            return
        }
        
        writeCharacteristic = service.getCharacteristic(UUID.fromString(BluetoothProtocol.UUID.WRITE_CHARACTERISTIC_UUID))
        readCharacteristic = service.getCharacteristic(UUID.fromString(BluetoothProtocol.UUID.READ_NOTIFY_CHARACTERISTIC_UUID))
        
        if (writeCharacteristic == null || readCharacteristic == null) {
            logE("未找到目标特征值")
            return
        }
        
        // 启用通知
        gatt.setCharacteristicNotification(readCharacteristic, true)
        
        logI("服务发现完成，特征值已获取")
    }
    
    /**
     * 处理特征值读取
     */
    private fun handleCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            logE("特征值读取失败: $status")
            return
        }
        
        val data = characteristic.value
        logI("读取数据: ${data.joinToString(", ") { "0x${it.toString(16).uppercase()}" }}")
    }
    
    /**
     * 处理特征值写入
     */
    private fun handleCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            logE("特征值写入失败: $status")
            notifyDataSent(gatt.device, characteristic.value, false)
            return
        }
        
        logI("数据写入成功")
        notifyDataSent(gatt.device, characteristic.value, true)
    }
    
    /**
     * 处理特征值变化
     */
    private fun handleCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        logI("收到数据: ${value.joinToString(", ") { "0x${it.toString(16).uppercase()}" }}")
        notifyDataReceived(gatt.device, value)
    }
    
    /**
     * 发送数据
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendData(packet: SvakomPacket): Boolean {
        if (connectionState != BluetoothProtocol.ConnectionState.STATE_CONNECTED) {
            logE("设备未连接")
            return false
        }
        
        if (writeCharacteristic == null) {
            logE("写入特征值未初始化")
            return false
        }
        
        try {
            val data = packet.toByteArray()
            writeCharacteristic?.value = data
            val success = currentGatt?.writeCharacteristic(writeCharacteristic) == true
            if (success) {
                logI("发送数据: ${data.joinToString(", ") { "0x${it.toString(16).uppercase()}" }}")
                // 设置写入超时
                scope.launch {
                    delay(WRITE_TIMEOUT)
                    // 如果超时，通知发送失败
                    notifyDataSent(currentDevice!!, data, false)
                }
            } else {
                logE("发送数据失败")
                notifyDataSent(currentDevice!!, data, false)
            }
            return success
        } catch (e: Exception) {
            logE("发送数据异常: ${e.message}")
            return false
        }
    }
    
    /**
     * 查询设备信息
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun queryDeviceInfo(): Boolean {
        val packet = SvakomBtCodec.createQueryDeviceInfoPacket()
        return sendData(packet)
    }
    
    /**
     * 查询电量状态
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun queryBatteryStatus(): Boolean {
        val packet = SvakomBtCodec.createQueryBatteryStatusPacket()
        return sendData(packet)
    }
    
    /**
     * 控制震动
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun controlVibration(motorId: Int, mode: Int, intensity: Int): Boolean {
        val control = VibrationControl(motorId, mode, intensity)
        val packet = SvakomBtCodec.createVibrationControlPacket(control)
        return sendData(packet)
    }
    
    /**
     * 控制加热
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun controlHeating(status: Int, temperature: Int, heaterId: Int = 0): Boolean {
        val control = HeatingControl(status, temperature, heaterId)
        val packet = SvakomBtCodec.createHeatingControlPacket(control)
        return sendData(packet)
    }
    
    /**
     * 获取当前连接状态
     */
    fun getConnectionState(): Int = connectionState
    
    /**
     * 获取当前连接的设备
     */
    fun getCurrentDevice(): BluetoothDevice? = currentDevice
    
    /**
     * 获取已发现的设备列表
     */
    fun getDiscoveredDevices(): List<BluetoothDevice> = discoveredDevices.values.toList()
    
    /**
     * 是否正在扫描
     */
    fun isScanning(): Boolean = isScanning
    
    /**
     * 是否已连接
     */
    fun isConnected(): Boolean = connectionState == BluetoothProtocol.ConnectionState.STATE_CONNECTED
    
    // 通知方法
    private fun notifyDeviceDiscovered(device: BluetoothDevice, broadcastData: BroadcastData?) {
        eventListeners.forEach { it.onDeviceDiscovered(device, broadcastData) }
    }
    
    private fun notifyScanStarted() {
        eventListeners.forEach { it.onScanStarted() }
    }
    
    private fun notifyScanStopped() {
        eventListeners.forEach { it.onScanStopped() }
    }
    
    private fun notifyConnectionStateChanged(device: BluetoothDevice, state: Int) {
        eventListeners.forEach { it.onConnectionStateChanged(device, state) }
    }
    
    private fun notifyDataReceived(device: BluetoothDevice, data: ByteArray) {
        eventListeners.forEach { it.onDataReceived(device, data) }
    }
    
    private fun notifyDataSent(device: BluetoothDevice, data: ByteArray, success: Boolean) {
        eventListeners.forEach { it.onDataSent(device, data, success) }
    }
    
    private fun notifyError(error: String) {
        eventListeners.forEach { it.onError(error) }
    }
}
