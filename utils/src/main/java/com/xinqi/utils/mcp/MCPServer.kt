package com.xinqi.utils.mcp

import android.content.Context
import com.xinqi.utils.bt.AildoBluetoothManager
import com.xinqi.utils.bt.spec.odm.SvakomPacket
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * MCP服务器
 * 处理来自LLM的蓝牙控制请求
 */
class MCPServer private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "MCPServer"
        
        @Volatile
        private var INSTANCE: MCPServer? = null
        
        fun getInstance(context: Context): MCPServer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MCPServer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val bluetoothManager = AildoBluetoothManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // 请求处理
    private val pendingRequests = ConcurrentHashMap<String, MCPRequest>()
    private val requestCounter = AtomicInteger(0)
    
    // 事件监听器
    private val eventListeners = mutableListOf<MCPServerEventListener>()
    
    // 设备状态缓存
    private val deviceStatusCache = ConcurrentHashMap<String, DeviceStatus>()
    
    // 命令队列
    private val commandQueue = mutableListOf<BluetoothControlCommand>()
    private var isProcessingQueue = false
    
    /**
     * MCP服务器事件监听器
     */
    interface MCPServerEventListener {
        fun onRequestReceived(request: MCPRequest)
        fun onResponseSent(response: MCPResponse)
        fun onCommandExecuted(command: BluetoothControlCommand, success: Boolean)
        fun onDeviceStatusChanged(deviceId: String, status: DeviceStatus)
        fun onError(error: String, requestId: String? = null)
    }
    
    fun addEventListener(listener: MCPServerEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }
    
    fun removeEventListener(listener: MCPServerEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * 处理MCP请求
     */
    fun handleRequest(request: MCPRequest): MCPResponse {
        logI("处理MCP请求: ${request.method}")
        notifyRequestReceived(request)
        
        return try {
            when (request.method) {
                "bluetooth.scan" -> handleScanDevices(request)
                "bluetooth.connect" -> handleConnectDevice(request)
                "bluetooth.disconnect" -> handleDisconnectDevice(request)
                "bluetooth.control_vibration" -> handleControlVibration(request)
                "bluetooth.control_heating" -> handleControlHeating(request)
                "bluetooth.combined_control" -> handleCombinedControl(request)
                "bluetooth.get_status" -> handleGetDeviceStatus(request)
                "bluetooth.query_info" -> handleQueryDeviceInfo(request)
                "bluetooth.query_battery" -> handleQueryBatteryStatus(request)
                "system.ping" -> handlePing(request)
                "system.get_capabilities" -> handleGetCapabilities(request)
                else -> createErrorResponse(request.id, MCPErrorCodes.METHOD_NOT_FOUND, "未知方法: ${request.method}")
            }
        } catch (e: Exception) {
            logE("处理请求异常: ${e.message}")
            createErrorResponse(request.id, MCPErrorCodes.INTERNAL_ERROR, "内部错误: ${e.message}")
        }
    }
    
    /**
     * 处理扫描设备请求
     */
    private fun handleScanDevices(request: MCPRequest): MCPResponse {
        if (!bluetoothManager.isBluetoothAvailable()) {
            return createErrorResponse(request.id, MCPErrorCodes.BLUETOOTH_NOT_AVAILABLE, "蓝牙不可用")
        }
        
        val success = bluetoothManager.startScan()
        if (success) {
            // 设置扫描超时
            scope.launch {
                delay(10000) // 10秒后停止扫描
                bluetoothManager.stopScan()
            }
            
            return MCPResponse(
                id = request.id,
                success = true,
                result = mapOf(
                    "message" to "开始扫描设备",
                    "timeout" to 10000L
                )
            )
        } else {
            return createErrorResponse(request.id, MCPErrorCodes.CONNECTION_FAILED, "启动扫描失败")
        }
    }
    
    /**
     * 处理连接设备请求
     */
    private fun handleConnectDevice(request: MCPRequest): MCPResponse {
        val deviceId = request.params["device_id"] as? String
        if (deviceId == null) {
            return createErrorResponse(request.id, MCPErrorCodes.INVALID_PARAMS, "缺少设备ID参数")
        }
        
        val device = bluetoothManager.getDiscoveredDevices().find { it.address == deviceId }
        if (device == null) {
            return createErrorResponse(request.id, MCPErrorCodes.DEVICE_NOT_FOUND, "设备未找到")
        }
        
        val success = bluetoothManager.connectDevice(device)
        if (success) {
            return MCPResponse(
                id = request.id,
                success = true,
                result = mapOf(
                    "message" to "开始连接设备",
                    "device_id" to deviceId
                )
            )
        } else {
            return createErrorResponse(request.id, MCPErrorCodes.CONNECTION_FAILED, "连接设备失败")
        }
    }
    
    /**
     * 处理断开设备请求
     */
    private fun handleDisconnectDevice(request: MCPRequest): MCPResponse {
        bluetoothManager.disconnectDevice()
        
        return MCPResponse(
            id = request.id,
            success = true,
            result = mapOf("message" to "设备已断开")
        )
    }
    
    /**
     * 处理震动控制请求
     */
    private fun handleControlVibration(request: MCPRequest): MCPResponse {
        if (!bluetoothManager.isConnected()) {
            return createErrorResponse(request.id, MCPErrorCodes.DEVICE_NOT_CONNECTED, "设备未连接")
        }
        
        val motorId = (request.params["motor_id"] as? Number)?.toInt() ?: 0
        val mode = (request.params["mode"] as? Number)?.toInt() ?: 1
        val intensity = (request.params["intensity"] as? Number)?.toInt() ?: 50
        val duration = (request.params["duration"] as? Number)?.toLong() ?: 0L
        
        val success = bluetoothManager.controlVibration(motorId, mode, intensity)
        
        if (success) {
            // 如果有持续时间，设置定时停止
            if (duration > 0) {
                scope.launch {
                    delay(duration)
                    bluetoothManager.controlVibration(motorId, 0, 0) // 停止震动
                }
            }
            
            return MCPResponse(
                id = request.id,
                success = true,
                result = mapOf(
                    "message" to "震动控制成功",
                    "motor_id" to motorId,
                    "mode" to mode,
                    "intensity" to intensity,
                    "duration" to duration
                )
            )
        } else {
            return createErrorResponse(request.id, MCPErrorCodes.COMMAND_EXECUTION_FAILED, "震动控制失败")
        }
    }
    
    /**
     * 处理加热控制请求
     */
    private fun handleControlHeating(request: MCPRequest): MCPResponse {
        if (!bluetoothManager.isConnected()) {
            return createErrorResponse(request.id, MCPErrorCodes.DEVICE_NOT_CONNECTED, "设备未连接")
        }
        
        val status = (request.params["status"] as? Number)?.toInt() ?: 1
        val temperature = (request.params["temperature"] as? Number)?.toInt() ?: 37
        val heaterId = (request.params["heater_id"] as? Number)?.toInt() ?: 0
        val duration = (request.params["duration"] as? Number)?.toLong() ?: 0L
        
        val success = bluetoothManager.controlHeating(status, temperature, heaterId)
        
        if (success) {
            // 如果有持续时间，设置定时停止
            if (duration > 0) {
                scope.launch {
                    delay(duration)
                    bluetoothManager.controlHeating(0, 0, heaterId) // 停止加热
                }
            }
            
            return MCPResponse(
                id = request.id,
                success = true,
                result = mapOf(
                    "message" to "加热控制成功",
                    "status" to status,
                    "temperature" to temperature,
                    "heater_id" to heaterId,
                    "duration" to duration
                )
            )
        } else {
            return createErrorResponse(request.id, MCPErrorCodes.COMMAND_EXECUTION_FAILED, "加热控制失败")
        }
    }
    
    /**
     * 处理组合控制请求
     */
    private fun handleCombinedControl(request: MCPRequest): MCPResponse {
        if (!bluetoothManager.isConnected()) {
            return createErrorResponse(request.id, MCPErrorCodes.DEVICE_NOT_CONNECTED, "设备未连接")
        }
        
        val commands = request.params["commands"] as? List<Map<String, Any>>
        if (commands == null || commands.isEmpty()) {
            return createErrorResponse(request.id, MCPErrorCodes.INVALID_PARAMS, "缺少控制命令")
        }
        
        val results = mutableListOf<Map<String, Any>>()
        var allSuccess = true
        
        for (command in commands) {
            val action = command["action"] as? String
            when (action) {
                "vibration" -> {
                    val motorId = (command["motor_id"] as? Number)?.toInt() ?: 0
                    val mode = (command["mode"] as? Number)?.toInt() ?: 1
                    val intensity = (command["intensity"] as? Number)?.toInt() ?: 50
                    
                    val success = bluetoothManager.controlVibration(motorId, mode, intensity)
                    results.add(mapOf(
                        "action" to "vibration",
                        "success" to success,
                        "motor_id" to motorId,
                        "intensity" to intensity
                    ))
                    if (!success) allSuccess = false
                }
                "heating" -> {
                    val status = (command["status"] as? Number)?.toInt() ?: 1
                    val temperature = (command["temperature"] as? Number)?.toInt() ?: 37
                    val heaterId = (command["heater_id"] as? Number)?.toInt() ?: 0
                    
                    val success = bluetoothManager.controlHeating(status, temperature, heaterId)
                    results.add(mapOf(
                        "action" to "heating",
                        "success" to success,
                        "temperature" to temperature,
                        "heater_id" to heaterId
                    ))
                    if (!success) allSuccess = false
                }
            }
        }
        
        return MCPResponse(
            id = request.id,
            success = allSuccess,
            result = mapOf(
                "message" to if (allSuccess) "组合控制成功" else "部分控制失败",
                "results" to results
            )
        )
    }
    
    /**
     * 处理获取设备状态请求
     */
    private fun handleGetDeviceStatus(request: MCPRequest): MCPResponse {
        val deviceId = request.params["device_id"] as? String
        val status = if (deviceId != null) {
            deviceStatusCache[deviceId] ?: DeviceStatus(
                deviceId = deviceId,
                isConnected = bluetoothManager.isConnected()
            )
        } else {
            // 返回所有设备状态
            deviceStatusCache.values.toList()
        }
        
        return MCPResponse(
            id = request.id,
            success = true,
            result = status
        )
    }
    
    /**
     * 处理查询设备信息请求
     */
    private fun handleQueryDeviceInfo(request: MCPRequest): MCPResponse {
        if (!bluetoothManager.isConnected()) {
            return createErrorResponse(request.id, MCPErrorCodes.DEVICE_NOT_CONNECTED, "设备未连接")
        }
        
        val success = bluetoothManager.queryDeviceInfo()
        return MCPResponse(
            id = request.id,
            success = success,
            result = mapOf("message" to if (success) "查询设备信息成功" else "查询设备信息失败")
        )
    }
    
    /**
     * 处理查询电量状态请求
     */
    private fun handleQueryBatteryStatus(request: MCPRequest): MCPResponse {
        if (!bluetoothManager.isConnected()) {
            return createErrorResponse(request.id, MCPErrorCodes.DEVICE_NOT_CONNECTED, "设备未连接")
        }
        
        val success = bluetoothManager.queryBatteryStatus()
        return MCPResponse(
            id = request.id,
            success = success,
            result = mapOf("message" to if (success) "查询电量状态成功" else "查询电量状态失败")
        )
    }
    
    /**
     * 处理Ping请求
     */
    private fun handlePing(request: MCPRequest): MCPResponse {
        return MCPResponse(
            id = request.id,
            success = true,
            result = mapOf(
                "message" to "pong",
                "timestamp" to System.currentTimeMillis(),
                "version" to MCPVersion.VERSION
            )
        )
    }
    
    /**
     * 处理获取能力请求
     */
    private fun handleGetCapabilities(request: MCPRequest): MCPResponse {
        val capabilities = mapOf(
            "version" to MCPVersion.VERSION,
            "protocol" to MCPVersion.PROTOCOL_NAME,
            "bluetooth" to mapOf(
                "available" to bluetoothManager.isBluetoothAvailable(),
                "connected" to bluetoothManager.isConnected(),
                "scanning" to bluetoothManager.isScanning(),
                "supported_actions" to BluetoothAction.values().map { it.value }
            ),
            "features" to listOf(
                "device_scan",
                "device_connect",
                "vibration_control",
                "heating_control",
                "combined_control",
                "status_query"
            )
        )
        
        return MCPResponse(
            id = request.id,
            success = true,
            result = capabilities
        )
    }
    
    /**
     * 创建错误响应
     */
    private fun createErrorResponse(requestId: String, code: Int, message: String): MCPResponse {
        return MCPResponse(
            id = requestId,
            success = false,
            error = MCPError(code, message)
        )
    }
    
    /**
     * 更新设备状态
     */
    fun updateDeviceStatus(deviceId: String, status: DeviceStatus) {
        deviceStatusCache[deviceId] = status
        notifyDeviceStatusChanged(deviceId, status)
    }
    
    /**
     * 添加命令到队列
     */
    fun addCommandToQueue(command: BluetoothControlCommand) {
        commandQueue.add(command)
        if (!isProcessingQueue) {
            processCommandQueue()
        }
    }
    
    /**
     * 处理命令队列
     */
    private fun processCommandQueue() {
        if (isProcessingQueue || commandQueue.isEmpty()) return
        
        isProcessingQueue = true
        scope.launch {
            while (commandQueue.isNotEmpty()) {
                val command = commandQueue.removeAt(0)
                val success = executeBluetoothCommand(command)
                notifyCommandExecuted(command, success)
                
                // 命令间延迟
                delay(100)
            }
            isProcessingQueue = false
        }
    }
    
    /**
     * 执行蓝牙命令
     */
    private suspend fun executeBluetoothCommand(command: BluetoothControlCommand): Boolean {
        return try {
            when (command.action) {
                BluetoothAction.CONTROL_VIBRATION -> {
                    val motorId = (command.parameters["motor_id"] as? Number)?.toInt() ?: 0
                    val mode = (command.parameters["mode"] as? Number)?.toInt() ?: 1
                    val intensity = (command.parameters["intensity"] as? Number)?.toInt() ?: 50
                    bluetoothManager.controlVibration(motorId, mode, intensity)
                }
                BluetoothAction.CONTROL_HEATING -> {
                    val status = (command.parameters["status"] as? Number)?.toInt() ?: 1
                    val temperature = (command.parameters["temperature"] as? Number)?.toInt() ?: 37
                    val heaterId = (command.parameters["heater_id"] as? Number)?.toInt() ?: 0
                    bluetoothManager.controlHeating(status, temperature, heaterId)
                }
                BluetoothAction.QUERY_DEVICE_INFO -> {
                    bluetoothManager.queryDeviceInfo()
                }
                BluetoothAction.QUERY_BATTERY_STATUS -> {
                    bluetoothManager.queryBatteryStatus()
                }
                else -> false
            }
        } catch (e: Exception) {
            logE("执行蓝牙命令失败: ${e.message}")
            false
        }
    }
    
    // 通知方法
    private fun notifyRequestReceived(request: MCPRequest) {
        eventListeners.forEach { it.onRequestReceived(request) }
    }
    
    private fun notifyResponseSent(response: MCPResponse) {
        eventListeners.forEach { it.onResponseSent(response) }
    }
    
    private fun notifyCommandExecuted(command: BluetoothControlCommand, success: Boolean) {
        eventListeners.forEach { it.onCommandExecuted(command, success) }
    }
    
    private fun notifyDeviceStatusChanged(deviceId: String, status: DeviceStatus) {
        eventListeners.forEach { it.onDeviceStatusChanged(deviceId, status) }
    }
    
    private fun notifyError(error: String, requestId: String? = null) {
        eventListeners.forEach { it.onError(error, requestId) }
    }
}

