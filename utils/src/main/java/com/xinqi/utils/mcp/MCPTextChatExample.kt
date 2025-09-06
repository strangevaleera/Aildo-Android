package com.xinqi.utils.mcp

import android.content.Context
import com.xinqi.utils.llm.LLMManager
import com.xinqi.utils.llm.model.LLMModel
import com.xinqi.utils.llm.model.PromptTemplate
import com.xinqi.utils.log.logI
import com.xinqi.utils.log.showResult

/**
 * MCP集成到textChat方法的使用示例
 */
object MCPTextChatExample {
    
    /**
     * 示例1: 基本textChat使用（自动处理蓝牙控制）
     */
    fun basicTextChatExample(context: Context) {
        logI("开始基本textChat示例")
        
        val llmManager = LLMManager.getInstance(context)
        
        // 添加事件监听器
        llmManager.addEventListener(object : LLMManager.LLMEventListener {
            override fun onModelChanged(model: LLMModel) {
                logI("模型已切换: ${model.displayName}")
            }
            
            override fun onConfigUpdated(config: com.xinqi.utils.llm.LLMConfig) {
                logI("配置已更新")
            }
            
            override fun onError(error: String) {
                logI("发生错误: $error")
            }
            
            override fun onBluetoothCommandsExecuted(commands: List<BluetoothControlCommand>, results: List<Boolean>) {
                logI("蓝牙命令执行完成: ${commands.size}个命令")
                commands.forEachIndexed { index, command ->
                    logI("命令${index + 1}: ${command.action.value} - ${if (results[index]) "成功" else "失败"}")
                }
            }
        })
        
        // 使用textChat方法，自动处理蓝牙控制
        llmManager.textChat("请开启震动功能，强度设置为70%") { response ->
            logI("LLM回复: $response")
            showResult(context, "LLM回复: $response")
        }
    }
    
    /**
     * 示例2: 使用自定义Prompt模板
     */
    fun customPromptExample(context: Context) {
        logI("开始自定义Prompt示例")
        
        val llmManager = LLMManager.getInstance(context)
        
        val customPrompt = PromptTemplate(
            name = "蓝牙控制助手",
            description = "专门用于蓝牙设备控制的助手",
            template = "你是一个智能助手，可以理解用户的指令并控制蓝牙设备。当用户提到震动、加热、设备控制等相关指令时，请直接回复相应的控制指令。\n\n用户问题：{{question}}",
            variables = listOf("question")
        )
        
        llmManager.textChat("加热到40度，持续3分钟", customPrompt) { response ->
            logI("自定义Prompt回复: $response")
            showResult(context, "自定义Prompt回复: $response")
        }
    }
    
    /**
     * 示例3: 流式对话示例
     */
    fun streamChatExample(context: Context) {
        logI("开始流式对话示例")
        
        val llmManager = LLMManager.getInstance(context)
        
        llmManager.textChatStream("同时开启震动和加热功能") { chunk, isComplete ->
            if (isComplete) {
                logI("流式对话完成")
                showResult(context, "流式对话完成")
            } else {
                logI("收到片段: $chunk")
            }
        }
    }
    
    /**
     * 示例4: 直接发送MCP请求
     */
    fun directMCPRequestExample(context: Context) {
        logI("开始直接MCP请求示例")
        
        val llmManager = LLMManager.getInstance(context)
        
        // 扫描设备
        llmManager.sendMCPRequest("bluetooth.scan") { response ->
            logI("扫描设备结果: ${response.success}")
            if (response.success) {
                showResult(context, "设备扫描成功")
            } else {
                showResult(context, "设备扫描失败: ${response.error?.message}")
            }
        }
        
        // 控制震动
        llmManager.sendMCPRequest(
            method = "bluetooth.control_vibration",
            params = mapOf(
                "motor_id" to 0,
                "mode" to 1,
                "intensity" to 80,
                "duration" to 5000L
            )
        ) { response ->
            logI("震动控制结果: ${response.success}")
            if (response.success) {
                showResult(context, "震动控制成功")
            } else {
                showResult(context, "震动控制失败: ${response.error?.message}")
            }
        }
    }
    
    /**
     * 示例5: 会话管理示例
     */
    fun sessionManagementExample(context: Context) {
        logI("开始会话管理示例")
        
        val llmManager = LLMManager.getInstance(context)
        
        // 开始MCP会话
        val session = llmManager.startMCPSession("test_user", "test_device")
        logI("MCP会话已开始: ${session.sessionId}")
        
        // 使用textChat
        llmManager.textChat("请开启震动功能") { response ->
            logI("会话中LLM回复: $response")
        }
        
        // 获取当前会话
        val currentSession = llmManager.getCurrentMCPSession()
        logI("当前会话: ${currentSession?.sessionId}")
        
        // 结束会话
        llmManager.endMCPSession()
        logI("MCP会话已结束")
    }
    
    /**
     * 示例6: 多种控制指令测试
     */
    fun multipleCommandsExample(context: Context) {
        logI("开始多种控制指令测试")
        
        val llmManager = LLMManager.getInstance(context)
        
        val testCommands = listOf(
            "请开启震动功能，强度设置为50%",
            "加热到38度，持续2分钟",
            "同时开启震动和加热功能",
            "停止所有设备功能",
            "查询设备电量和状态",
            "扫描附近的蓝牙设备"
        )
        
        testCommands.forEachIndexed { index, command ->
            logI("测试命令${index + 1}: $command")
            
            llmManager.textChat(command) { response ->
                logI("命令${index + 1}回复: $response")
                
                if (index == testCommands.size - 1) {
                    showResult(context, "所有测试命令完成")
                }
            }
        }
    }
    
    /**
     * 示例7: 错误处理示例
     */
    fun errorHandlingExample(context: Context) {
        logI("开始错误处理示例")
        
        val llmManager = LLMManager.getInstance(context)
        
        // 添加错误监听器
        llmManager.addEventListener(object : LLMManager.LLMEventListener {
            override fun onModelChanged(model: LLMModel) {}
            override fun onConfigUpdated(config: com.xinqi.utils.llm.LLMConfig) {}
            override fun onError(error: String) {
                logI("捕获到错误: $error")
                showResult(context, "错误: $error")
            }
            override fun onBluetoothCommandsExecuted(commands: List<BluetoothControlCommand>, results: List<Boolean>) {
                val failedCommands = commands.filterIndexed { index, _ -> !results[index] }
                if (failedCommands.isNotEmpty()) {
                    logI("有${failedCommands.size}个命令执行失败")
                    showResult(context, "有${failedCommands.size}个命令执行失败")
                }
            }
        })
        
        // 测试无效命令
        llmManager.textChat("执行一个无效的蓝牙控制命令") { response ->
            logI("无效命令回复: $response")
        }
    }
    
    /**
     * 运行所有示例
     */
    fun runAllExamples(context: Context) {
        logI("开始运行所有MCP textChat示例")
        
        // 依次运行各个示例
        basicTextChatExample(context)
        
        // 延迟运行其他示例
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            customPromptExample(context)
        }, 2000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            streamChatExample(context)
        }, 4000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            directMCPRequestExample(context)
        }, 6000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            sessionManagementExample(context)
        }, 8000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            multipleCommandsExample(context)
        }, 10000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            errorHandlingExample(context)
        }, 12000)
    }
}

