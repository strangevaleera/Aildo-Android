package com.xinqi.utils.mcp

import android.content.Context
import com.xinqi.utils.bt.AildoBluetoothManager
import com.xinqi.utils.llm.LLMManager
import com.xinqi.utils.llm.model.LLMModel
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * MCP集成管理器
 * 整合LLM、MCP协议和蓝牙控制功能
 */
class MCPIntegrationManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "MCPIntegrationManager"
        
        @Volatile
        private var INSTANCE: MCPIntegrationManager? = null
        
        fun getInstance(context: Context): MCPIntegrationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MCPIntegrationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // 核心组件
    private val mcpClient = MCPClient.getInstance(context)
    private val mcpServer = MCPServer.getInstance(context)
    private val responseParser = LLMResponseParser.getInstance()
    private val bluetoothManager = AildoBluetoothManager.getInstance(context)
    
    // 延迟初始化LLMManager，避免循环依赖
    private val llmManager: LLMManager by lazy { LLMManager.getInstance(context) }
    
    // 状态管理
    private val activeSessions = ConcurrentHashMap<String, MCPSession>()
    private val commandHistory = mutableListOf<MCPCommandHistory>()
    
    // 事件监听器
    private val eventListeners = mutableListOf<MCPIntegrationEventListener>()
    
    /**
     * MCP会话
     */
    data class MCPSession(
        val sessionId: String,
        val userId: String? = null,
        val deviceId: String? = null,
        val startTime: Long = System.currentTimeMillis(),
        val lastActivity: Long = System.currentTimeMillis(),
        val context: MCPContext
    )
    
    /**
     * MCP命令历史
     */
    data class MCPCommandHistory(
        val id: String,
        val sessionId: String,
        val originalText: String,
        val parsedResult: LLMResponseParseResult,
        val executedCommands: List<BluetoothControlCommand>,
        val executionResults: List<Boolean>,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * MCP集成事件监听器
     */
    interface MCPIntegrationEventListener {
        fun onSessionStarted(session: MCPSession)
        fun onSessionEnded(sessionId: String)
        fun onLLMResponseReceived(text: String, sessionId: String)
        fun onCommandParsed(result: LLMResponseParseResult, sessionId: String)
        fun onCommandExecuted(command: BluetoothControlCommand, success: Boolean, sessionId: String)
        fun onError(error: String, sessionId: String? = null)
    }
    
    fun addEventListener(listener: MCPIntegrationEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }
    
    fun removeEventListener(listener: MCPIntegrationEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * 开始MCP会话
     */
    fun startSession(
        userId: String? = null,
        deviceId: String? = null,
        conversationId: String? = null
    ): MCPSession {
        val sessionId = generateSessionId()
        val context = MCPContext(
            sessionId = sessionId,
            userId = userId,
            deviceId = deviceId,
            conversationId = conversationId
        )
        
        val session = MCPSession(
            sessionId = sessionId,
            userId = userId,
            deviceId = deviceId,
            context = context
        )
        
        activeSessions[sessionId] = session
        notifySessionStarted(session)
        
        logI("MCP会话已开始: $sessionId")
        return session
    }
    
    /**
     * 结束MCP会话
     */
    fun endSession(sessionId: String) {
        val session = activeSessions.remove(sessionId)
        if (session != null) {
            notifySessionEnded(sessionId)
            logI("MCP会话已结束: $sessionId")
        }
    }
    
    /**
     * 处理LLM回复并执行蓝牙控制
     */
    fun processLLMResponse(
        llmResponse: String,
        sessionId: String,
        callback: (List<BluetoothControlCommand>, List<Boolean>) -> Unit
    ) {
        val session = activeSessions[sessionId]
        if (session == null) {
            logE("会话不存在: $sessionId")
            notifyError("会话不存在: $sessionId", sessionId)
            return
        }
        
        logI("处理LLM回复: ${llmResponse.take(100)}...")
        notifyLLMResponseReceived(llmResponse, sessionId)
        
        // 解析LLM回复
        responseParser.parseLLMResponse(llmResponse, session.context) { parseResult ->
            notifyCommandParsed(parseResult, sessionId)
            
            if (!parseResult.hasBluetoothCommand) {
                logI("LLM回复中未包含蓝牙控制命令")
                callback(emptyList(), emptyList())
                return@parseLLMResponse
            }
            
            logI("解析到${parseResult.commands.size}个蓝牙控制命令，置信度: ${parseResult.confidence}")
            
            // 执行蓝牙控制命令
            executeBluetoothCommands(parseResult.commands, sessionId) { executedCommands, results ->
                // 记录命令历史
                val history = MCPCommandHistory(
                    id = generateCommandId(),
                    sessionId = sessionId,
                    originalText = llmResponse,
                    parsedResult = parseResult,
                    executedCommands = executedCommands,
                    executionResults = results
                )
                commandHistory.add(history)
                
                callback(executedCommands, results)
            }
        }
    }
    
    /**
     * 执行蓝牙控制命令
     */
    private fun executeBluetoothCommands(
        commands: List<BluetoothControlCommand>,
        sessionId: String,
        callback: (List<BluetoothControlCommand>, List<Boolean>) -> Unit
    ) {
        val executedCommands = mutableListOf<BluetoothControlCommand>()
        val results = mutableListOf<Boolean>()
        var completedCount = 0
        
        if (commands.isEmpty()) {
            callback(emptyList(), emptyList())
            return
        }
        
        // 按优先级排序命令
        val sortedCommands = commands.sortedByDescending { it.priority }
        
        sortedCommands.forEach { command ->
            executeSingleCommand(command, sessionId) { success ->
                executedCommands.add(command)
                results.add(success)
                completedCount++
                
                notifyCommandExecuted(command, success, sessionId)
                
                // 所有命令执行完成
                if (completedCount == sortedCommands.size) {
                    callback(executedCommands, results)
                }
            }
        }
    }
    
    /**
     * 执行单个蓝牙控制命令
     */
    private fun executeSingleCommand(
        command: BluetoothControlCommand,
        sessionId: String,
        callback: (Boolean) -> Unit
    ) {
        logI("执行蓝牙命令: ${command.action.value}")
        
        when (command.action) {
            BluetoothAction.SCAN_DEVICES -> {
                mcpClient.scanDevices { response ->
                    callback(response.success)
                }
            }
            
            BluetoothAction.CONNECT_DEVICE -> {
                val deviceId = command.parameters["device_id"] as? String ?: "default"
                mcpClient.connectDevice(deviceId) { response ->
                    callback(response.success)
                }
            }
            
            BluetoothAction.DISCONNECT_DEVICE -> {
                mcpClient.disconnectDevice { response ->
                    callback(response.success)
                }
            }
            
            BluetoothAction.CONTROL_VIBRATION -> {
                val motorId = (command.parameters["motor_id"] as? Number)?.toInt() ?: 0
                val mode = (command.parameters["mode"] as? Number)?.toInt() ?: 1
                val intensity = (command.parameters["intensity"] as? Number)?.toInt() ?: 50
                val duration = (command.parameters["duration"] as? Number)?.toLong() ?: 0L
                
                mcpClient.controlVibration(motorId, mode, intensity, duration) { response ->
                    callback(response.success)
                }
            }
            
            BluetoothAction.CONTROL_HEATING -> {
                val status = (command.parameters["status"] as? Number)?.toInt() ?: 1
                val temperature = (command.parameters["temperature"] as? Number)?.toInt() ?: 37
                val heaterId = (command.parameters["heater_id"] as? Number)?.toInt() ?: 0
                val duration = (command.parameters["duration"] as? Number)?.toLong() ?: 0L
                
                mcpClient.controlHeating(status, temperature, heaterId, duration) { response ->
                    callback(response.success)
                }
            }
            
            BluetoothAction.COMBINED_CONTROL -> {
                val commands = command.parameters["commands"] as? List<Map<String, Any>> ?: emptyList()
                mcpClient.combinedControl(commands) { response ->
                    callback(response.success)
                }
            }
            
            BluetoothAction.QUERY_DEVICE_INFO -> {
                mcpClient.queryDeviceInfo { response ->
                    callback(response.success)
                }
            }
            
            BluetoothAction.QUERY_BATTERY_STATUS -> {
                mcpClient.queryBatteryStatus { response ->
                    callback(response.success)
                }
            }
            
            else -> {
                logE("未知的蓝牙控制命令: ${command.action}")
                callback(false)
            }
        }
    }
    
    /**
     * 直接发送MCP请求
     */
    fun sendMCPRequest(
        method: String,
        params: Map<String, Any> = emptyMap(),
        sessionId: String? = null,
        callback: (MCPResponse) -> Unit
    ): String {
        val context = sessionId?.let { activeSessions[it]?.context }
        return mcpClient.sendRequest(method, params, context, callback = callback)
    }
    
    /**
     * 获取会话信息
     */
    fun getSession(sessionId: String): MCPSession? {
        return activeSessions[sessionId]
    }
    
    /**
     * 获取所有活跃会话
     */
    fun getActiveSessions(): List<MCPSession> {
        return activeSessions.values.toList()
    }
    
    /**
     * 获取命令历史
     */
    fun getCommandHistory(sessionId: String? = null): List<MCPCommandHistory> {
        return if (sessionId != null) {
            commandHistory.filter { it.sessionId == sessionId }
        } else {
            commandHistory.toList()
        }
    }
    
    /**
     * 清理命令历史
     */
    fun clearCommandHistory(sessionId: String? = null) {
        if (sessionId != null) {
            commandHistory.removeAll { it.sessionId == sessionId }
        } else {
            commandHistory.clear()
        }
    }
    
    /**
     * 获取蓝牙设备状态
     */
    fun getBluetoothStatus(): Map<String, Any> {
        return mapOf(
            "available" to bluetoothManager.isBluetoothAvailable(),
            "connected" to bluetoothManager.isConnected(),
            "scanning" to bluetoothManager.isScanning(),
            "discovered_devices" to bluetoothManager.getDiscoveredDevices().size,
            "current_device" to bluetoothManager.getCurrentDevice()?.address
        ) as Map<String, Any>
    }
    
    /**
     * 生成会话ID
     */
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * 生成命令ID
     */
    private fun generateCommandId(): String {
        return "cmd_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    // 通知方法
    private fun notifySessionStarted(session: MCPSession) {
        eventListeners.forEach { it.onSessionStarted(session) }
    }
    
    private fun notifySessionEnded(sessionId: String) {
        eventListeners.forEach { it.onSessionEnded(sessionId) }
    }
    
    private fun notifyLLMResponseReceived(text: String, sessionId: String) {
        eventListeners.forEach { it.onLLMResponseReceived(text, sessionId) }
    }
    
    private fun notifyCommandParsed(result: LLMResponseParseResult, sessionId: String) {
        eventListeners.forEach { it.onCommandParsed(result, sessionId) }
    }
    
    private fun notifyCommandExecuted(command: BluetoothControlCommand, success: Boolean, sessionId: String) {
        eventListeners.forEach { it.onCommandExecuted(command, success, sessionId) }
    }
    
    private fun notifyError(error: String, sessionId: String? = null) {
        eventListeners.forEach { it.onError(error, sessionId) }
    }
}
