package com.xinqi.utils.mcp

import android.content.Context
import com.xinqi.utils.log.logI
import com.xinqi.utils.log.showResult

/**
 * MCP协议使用示例
 * 展示如何使用MCP协议让LLM回复控制蓝牙设备
 */
object MCPExample {
    
    /**
     * 示例1: 基本MCP使用流程
     */
    fun basicMCPUsage(context: Context) {
        logI("开始基本MCP使用示例")
        
        val integrationManager = MCPIntegrationManager.getInstance(context)
        
        // 1. 开始MCP会话
        val session = integrationManager.startSession(
            userId = "user123",
            deviceId = "device456"
        )
        
        logI("MCP会话已开始: ${session.sessionId}")
        
        // 2. 模拟LLM回复
        val llmResponse = "请开启震动功能，强度设置为70%，持续10秒"
        
        // 3. 处理LLM回复并执行蓝牙控制
        integrationManager.processLLMResponse(llmResponse, session.sessionId) { commands, results ->
            logI("执行了${commands.size}个命令，成功${results.count { it }}个")
            
            // 显示结果
            val resultText = buildString {
                appendLine("MCP执行结果:")
                appendLine("原始文本: $llmResponse")
                appendLine("解析到命令数: ${commands.size}")
                commands.forEachIndexed { index, command ->
                    appendLine("命令${index + 1}: ${command.action.value}")
                    appendLine("  参数: ${command.parameters}")
                    appendLine("  结果: ${if (results[index]) "成功" else "失败"}")
                }
            }
            
            showResult(context, resultText)
        }
        
        // 4. 结束会话
        integrationManager.endSession(session.sessionId)
    }
    
    /**
     * 示例2: 直接发送MCP请求
     */
    fun directMCPRequest(context: Context) {
        logI("开始直接MCP请求示例")
        
        val integrationManager = MCPIntegrationManager.getInstance(context)
        val session = integrationManager.startSession()
        
        // 扫描设备
        integrationManager.sendMCPRequest("bluetooth.scan", sessionId = session.sessionId) { response ->
            logI("扫描设备结果: ${response.success}")
            if (response.success) {
                showResult(context, "设备扫描成功")
            } else {
                showResult(context, "设备扫描失败: ${response.error?.message}")
            }
        }
        
        // 控制震动
        integrationManager.sendMCPRequest(
            method = "bluetooth.control_vibration",
            params = mapOf(
                "motor_id" to 0,
                "mode" to 1,
                "intensity" to 80,
                "duration" to 5000L
            ),
            sessionId = session.sessionId
        ) { response ->
            logI("震动控制结果: ${response.success}")
            if (response.success) {
                showResult(context, "震动控制成功")
            } else {
                showResult(context, "震动控制失败: ${response.error?.message}")
            }
        }
        
        // 控制加热
        integrationManager.sendMCPRequest(
            method = "bluetooth.control_heating",
            params = mapOf(
                "status" to 1,
                "temperature" to 40,
                "heater_id" to 0,
                "duration" to 10000L
            ),
            sessionId = session.sessionId
        ) { response ->
            logI("加热控制结果: ${response.success}")
            if (response.success) {
                showResult(context, "加热控制成功")
            } else {
                showResult(context, "加热控制失败: ${response.error?.message}")
            }
        }
        
        integrationManager.endSession(session.sessionId)
    }
    
    /**
     * 示例3: 组合控制
     */
    fun combinedControlExample(context: Context) {
        logI("开始组合控制示例")
        
        val integrationManager = MCPIntegrationManager.getInstance(context)
        val session = integrationManager.startSession()
        
        // 同时控制震动和加热
        val commands = listOf(
            mapOf(
                "action" to "vibration",
                "motor_id" to 0,
                "mode" to 2, // 脉冲模式
                "intensity" to 60
            ),
            mapOf(
                "action" to "heating",
                "status" to 1,
                "temperature" to 38,
                "heater_id" to 0
            )
        )
        
        integrationManager.sendMCPRequest(
            method = "bluetooth.combined_control",
            params = mapOf("commands" to commands),
            sessionId = session.sessionId
        ) { response ->
            logI("组合控制结果: ${response.success}")
            if (response.success) {
                showResult(context, "组合控制成功")
            } else {
                showResult(context, "组合控制失败: ${response.error?.message}")
            }
        }
        
        integrationManager.endSession(session.sessionId)
    }
    
    /**
     * 示例4: LLM回复解析测试
     */
    fun llmResponseParsingExample(context: Context) {
        logI("开始LLM回复解析示例")
        
        val integrationManager = MCPIntegrationManager.getInstance(context)
        val session = integrationManager.startSession()
        
        // 测试不同的LLM回复
        val testResponses = listOf(
            "请开启震动功能，强度设置为50%",
            "加热到40度，持续5分钟",
            "同时开启震动和加热功能",
            "停止所有设备功能",
            "查询设备电量和状态",
            "扫描附近的蓝牙设备"
        )
        
        testResponses.forEachIndexed { index, response ->
            logI("测试回复${index + 1}: $response")
            
            integrationManager.processLLMResponse(response, session.sessionId) { commands, results ->
                val resultText = buildString {
                    appendLine("测试${index + 1}: $response")
                    appendLine("解析到命令: ${commands.size}个")
                    commands.forEach { command ->
                        appendLine("  - ${command.action.value}: ${command.parameters}")
                    }
                    appendLine("执行结果: ${results.count { it }}/${results.size}成功")
                    appendLine("---")
                }
                
                showResult(context, resultText)
            }
        }
        
        integrationManager.endSession(session.sessionId)
    }
    
    /**
     * 示例5: 事件监听器使用
     */
    fun eventListenerExample(context: Context) {
        logI("开始事件监听器示例")
        
        val integrationManager = MCPIntegrationManager.getInstance(context)
        
        // 添加事件监听器
        integrationManager.addEventListener(object : MCPIntegrationManager.MCPIntegrationEventListener {
            override fun onSessionStarted(session: MCPIntegrationManager.MCPSession) {
                logI("会话开始: ${session.sessionId}")
            }
            
            override fun onSessionEnded(sessionId: String) {
                logI("会话结束: $sessionId")
            }
            
            override fun onLLMResponseReceived(text: String, sessionId: String) {
                logI("收到LLM回复: ${text.take(50)}...")
            }
            
            override fun onCommandParsed(result: LLMResponseParseResult, sessionId: String) {
                logI("命令解析完成: ${result.commands.size}个命令，置信度: ${result.confidence}")
            }
            
            override fun onCommandExecuted(command: BluetoothControlCommand, success: Boolean, sessionId: String) {
                logI("命令执行完成: ${command.action.value} - ${if (success) "成功" else "失败"}")
            }
            
            override fun onError(error: String, sessionId: String?) {
                logI("发生错误: $error")
            }
        })
        
        // 开始会话并处理LLM回复
        val session = integrationManager.startSession()
        val llmResponse = "请开启震动功能，强度80%，持续3秒"
        
        integrationManager.processLLMResponse(llmResponse, session.sessionId) { commands, results ->
            logI("事件监听器示例完成")
            showResult(context, "事件监听器示例完成，请查看日志输出")
        }
        
        integrationManager.endSession(session.sessionId)
    }
    
    /**
     * 示例6: 获取状态信息
     */
    fun statusInfoExample(context: Context) {
        logI("开始状态信息示例")
        
        val integrationManager = MCPIntegrationManager.getInstance(context)
        val session = integrationManager.startSession()
        
        // 获取蓝牙状态
        val bluetoothStatus = integrationManager.getBluetoothStatus()
        logI("蓝牙状态: $bluetoothStatus")
        
        // 获取活跃会话
        val activeSessions = integrationManager.getActiveSessions()
        logI("活跃会话数: ${activeSessions.size}")
        
        // 获取命令历史
        val commandHistory = integrationManager.getCommandHistory(session.sessionId)
        logI("命令历史数: ${commandHistory.size}")
        
        val statusText = buildString {
            appendLine("系统状态信息:")
            appendLine("蓝牙状态:")
            bluetoothStatus.forEach { (key, value) ->
                appendLine("  $key: $value")
            }
            appendLine("活跃会话: ${activeSessions.size}")
            appendLine("命令历史: ${commandHistory.size}")
        }
        
        showResult(context, statusText)
        integrationManager.endSession(session.sessionId)
    }
    
    /**
     * 运行所有示例
     */
    fun runAllExamples(context: Context) {
        logI("开始运行所有MCP示例")
        
        // 依次运行各个示例
        basicMCPUsage(context)
        
        // 延迟运行其他示例
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            directMCPRequest(context)
        }, 2000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            combinedControlExample(context)
        }, 4000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            llmResponseParsingExample(context)
        }, 6000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            eventListenerExample(context)
        }, 8000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            statusInfoExample(context)
        }, 10000)
    }
}

