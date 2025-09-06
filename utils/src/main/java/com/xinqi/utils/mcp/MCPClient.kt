package com.xinqi.utils.mcp

import android.content.Context
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
 * MCP客户端
 * 用于发送MCP请求和处理响应
 */
class MCPClient private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "MCPClient"
        private const val DEFAULT_TIMEOUT = 10000L // 默认超时时间10秒
        
        @Volatile
        private var INSTANCE: MCPClient? = null
        
        fun getInstance(context: Context): MCPClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MCPClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mcpServer = MCPServer.getInstance(context)
    
    // 请求管理
    private val pendingRequests = ConcurrentHashMap<String, PendingRequest>()
    private val requestCounter = AtomicInteger(0)
    
    // 事件监听器
    private val eventListeners = mutableListOf<MCPClientEventListener>()
    
    /**
     * 待处理请求
     */
    private data class PendingRequest(
        val request: MCPRequest,
        val callback: (MCPResponse) -> Unit,
        val timeout: Long,
        val startTime: Long = System.currentTimeMillis()
    )
    
    /**
     * MCP客户端事件监听器
     */
    interface MCPClientEventListener {
        fun onRequestSent(request: MCPRequest)
        fun onResponseReceived(response: MCPResponse)
        fun onRequestTimeout(requestId: String)
        fun onError(error: String, requestId: String? = null)
    }
    
    fun addEventListener(listener: MCPClientEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }
    
    fun removeEventListener(listener: MCPClientEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * 发送MCP请求
     */
    fun sendRequest(
        method: String,
        params: Map<String, Any> = emptyMap(),
        context: MCPContext? = null,
        timeout: Long = DEFAULT_TIMEOUT,
        callback: (MCPResponse) -> Unit
    ): String {
        val requestId = generateRequestId()
        val request = MCPRequest(
            id = requestId,
            type = "request",
            method = method,
            params = params,
            context = context
        )
        
        val pendingRequest = PendingRequest(request, callback, timeout)
        pendingRequests[requestId] = pendingRequest
        
        // 发送请求
        scope.launch {
            try {
                notifyRequestSent(request)
                val response = mcpServer.handleRequest(request)
                handleResponse(response)
            } catch (e: Exception) {
                logE("发送请求失败: ${e.message}")
                handleError("发送请求失败: ${e.message}", requestId)
            }
        }
        
        // 设置超时
        scope.launch {
            delay(timeout)
            if (pendingRequests.containsKey(requestId)) {
                handleTimeout(requestId)
            }
        }
        
        return requestId
    }
    
    /**
     * 处理响应
     */
    private fun handleResponse(response: MCPResponse) {
        val pendingRequest = pendingRequests.remove(response.id)
        if (pendingRequest != null) {
            notifyResponseReceived(response)
            pendingRequest.callback(response)
        } else {
            logE("收到未知请求的响应: ${response.id}")
        }
    }
    
    /**
     * 处理超时
     */
    private fun handleTimeout(requestId: String) {
        val pendingRequest = pendingRequests.remove(requestId)
        if (pendingRequest != null) {
            logE("请求超时: $requestId")
            notifyRequestTimeout(requestId)
            
            val timeoutResponse = MCPResponse(
                id = requestId,
                success = false,
                error = MCPError(MCPErrorCodes.TIMEOUT, "请求超时")
            )
            pendingRequest.callback(timeoutResponse)
        }
    }
    
    /**
     * 处理错误
     */
    private fun handleError(error: String, requestId: String? = null) {
        notifyError(error, requestId)
        
        if (requestId != null) {
            val pendingRequest = pendingRequests.remove(requestId)
            if (pendingRequest != null) {
                val errorResponse = MCPResponse(
                    id = requestId,
                    success = false,
                    error = MCPError(MCPErrorCodes.INTERNAL_ERROR, error)
                )
                pendingRequest.callback(errorResponse)
            }
        }
    }
    
    /**
     * 生成请求ID
     */
    private fun generateRequestId(): String {
        return "req_${requestCounter.incrementAndGet()}_${System.currentTimeMillis()}"
    }
    
    // 便捷方法
    
    /**
     * 扫描蓝牙设备
     */
    fun scanDevices(callback: (MCPResponse) -> Unit): String {
        return sendRequest("bluetooth.scan", callback = callback)
    }
    
    /**
     * 连接设备
     */
    fun connectDevice(deviceId: String, callback: (MCPResponse) -> Unit): String {
        return sendRequest(
            method = "bluetooth.connect",
            params = mapOf("device_id" to deviceId),
            callback = callback
        )
    }
    
    /**
     * 断开设备连接
     */
    fun disconnectDevice(callback: (MCPResponse) -> Unit): String {
        return sendRequest("bluetooth.disconnect", callback = callback)
    }
    
    /**
     * 控制震动
     */
    fun controlVibration(
        motorId: Int = 0,
        mode: Int = 1,
        intensity: Int = 50,
        duration: Long = 0L,
        callback: (MCPResponse) -> Unit
    ): String {
        return sendRequest(
            method = "bluetooth.control_vibration",
            params = mapOf(
                "motor_id" to motorId,
                "mode" to mode,
                "intensity" to intensity,
                "duration" to duration
            ),
            callback = callback
        )
    }
    
    /**
     * 控制加热
     */
    fun controlHeating(
        status: Int = 1,
        temperature: Int = 37,
        heaterId: Int = 0,
        duration: Long = 0L,
        callback: (MCPResponse) -> Unit
    ): String {
        return sendRequest(
            method = "bluetooth.control_heating",
            params = mapOf(
                "status" to status,
                "temperature" to temperature,
                "heater_id" to heaterId,
                "duration" to duration
            ),
            callback = callback
        )
    }
    
    /**
     * 组合控制
     */
    fun combinedControl(
        commands: List<Map<String, Any>>,
        callback: (MCPResponse) -> Unit
    ): String {
        return sendRequest(
            method = "bluetooth.combined_control",
            params = mapOf("commands" to commands),
            callback = callback
        )
    }
    
    /**
     * 获取设备状态
     */
    fun getDeviceStatus(deviceId: String? = null, callback: (MCPResponse) -> Unit): String {
        val params = if (deviceId != null) {
            mapOf("device_id" to deviceId)
        } else {
            emptyMap<String, Any>()
        }
        
        return sendRequest(
            method = "bluetooth.get_status",
            params = params,
            callback = callback
        )
    }
    
    /**
     * 查询设备信息
     */
    fun queryDeviceInfo(callback: (MCPResponse) -> Unit): String {
        return sendRequest("bluetooth.query_info", callback = callback)
    }
    
    /**
     * 查询电量状态
     */
    fun queryBatteryStatus(callback: (MCPResponse) -> Unit): String {
        return sendRequest("bluetooth.query_battery", callback = callback)
    }
    
    /**
     * Ping服务器
     */
    fun ping(callback: (MCPResponse) -> Unit): String {
        return sendRequest("system.ping", callback = callback)
    }
    
    /**
     * 获取服务器能力
     */
    fun getCapabilities(callback: (MCPResponse) -> Unit): String {
        return sendRequest("system.get_capabilities", callback = callback)
    }
    
    /**
     * 取消请求
     */
    fun cancelRequest(requestId: String): Boolean {
        val pendingRequest = pendingRequests.remove(requestId)
        return pendingRequest != null
    }
    
    /**
     * 获取待处理请求数量
     */
    fun getPendingRequestCount(): Int {
        return pendingRequests.size
    }
    
    /**
     * 清理所有待处理请求
     */
    fun clearPendingRequests() {
        pendingRequests.clear()
    }
    
    // 通知方法
    private fun notifyRequestSent(request: MCPRequest) {
        eventListeners.forEach { it.onRequestSent(request) }
    }
    
    private fun notifyResponseReceived(response: MCPResponse) {
        eventListeners.forEach { it.onResponseReceived(response) }
    }
    
    private fun notifyRequestTimeout(requestId: String) {
        eventListeners.forEach { it.onRequestTimeout(requestId) }
    }
    
    private fun notifyError(error: String, requestId: String? = null) {
        eventListeners.forEach { it.onError(error, requestId) }
    }
}

