package com.xinqi.utils.mcp

import android.os.Build
import androidx.annotation.RequiresApi
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * LLM回复解析器
 * 解析LLM的回复文本，提取蓝牙控制命令
 */
class LLMResponseParser private constructor() {
    
    companion object {
        private const val TAG = "LLMResponseParser"
        
        @Volatile
        private var INSTANCE: LLMResponseParser? = null
        
        fun getInstance(): LLMResponseParser {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LLMResponseParser().also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // 事件监听器
    private val eventListeners = mutableListOf<LLMResponseParserEventListener>()
    
    /**
     * LLM回复解析器事件监听器
     */
    interface LLMResponseParserEventListener {
        fun onCommandParsed(result: LLMResponseParseResult)
        fun onParseError(error: String, originalText: String)
    }
    
    fun addEventListener(listener: LLMResponseParserEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }
    
    fun removeEventListener(listener: LLMResponseParserEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * 解析LLM回复文本
     */
    fun parseLLMResponse(
        text: String,
        context: MCPContext? = null,
        callback: (LLMResponseParseResult) -> Unit
    ) {
        scope.launch {
            try {
                val result = parseText(text, context)
                callback(result)
                notifyCommandParsed(result)
            } catch (e: Exception) {
                logE("解析LLM回复失败: ${e.message}")
                val errorResult = LLMResponseParseResult(
                    hasBluetoothCommand = false,
                    originalText = text,
                    confidence = 0.0f
                )
                callback(errorResult)
                notifyParseError("解析失败: ${e.message}", text)
            }
        }
    }
    
    /**
     * 解析文本内容
     */
    private fun parseText(text: String, context: MCPContext?): LLMResponseParseResult {
        val normalizedText = text.lowercase().trim()
        
        // 检查是否包含蓝牙控制相关的关键词
        val hasBluetoothKeywords = containsBluetoothKeywords(normalizedText)
        if (!hasBluetoothKeywords) {
            return LLMResponseParseResult(
                hasBluetoothCommand = false,
                originalText = text,
                confidence = 0.0f
            )
        }
        
        // 解析具体的控制命令
        val commands = mutableListOf<BluetoothControlCommand>()
        var confidence = 0.0f
        var intent: String? = null
        
        // 解析震动控制
        val vibrationCommands = parseVibrationCommands(normalizedText, text)
        commands.addAll(vibrationCommands)
        
        // 解析加热控制
        val heatingCommands = parseHeatingCommands(normalizedText, text)
        commands.addAll(heatingCommands)
        
        // 解析组合控制
        val combinedCommands = parseCombinedCommands(normalizedText, text)
        commands.addAll(combinedCommands)
        
        // 解析设备管理命令
        val deviceCommands = parseDeviceCommands(normalizedText, text)
        commands.addAll(deviceCommands)
        
        // 计算置信度

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            confidence = calculateConfidence(normalizedText, commands)
        }

        // 识别意图
        intent = identifyIntent(normalizedText)
        
        // 提取参数
        val extractedParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            extractParameters(normalizedText)
        } else {
            emptyMap()
        }

        return LLMResponseParseResult(
            hasBluetoothCommand = commands.isNotEmpty(),
            commands = commands,
            confidence = confidence,
            originalText = text,
            parsedIntent = intent,
            extractedParams = extractedParams
        )
    }
    
    /**
     * 检查是否包含蓝牙控制关键词
     */
    private fun containsBluetoothKeywords(text: String): Boolean {
        val keywords = listOf(
            "震动", "振动", "vibration", "vibrate",
            "加热", "温度", "heating", "temperature", "heat",
            "蓝牙", "bluetooth", "设备", "device",
            "连接", "connect", "断开", "disconnect",
            "扫描", "scan", "电量", "battery",
            "控制", "control", "开启", "start", "停止", "stop"
        )
        
        return keywords.any { text.contains(it) }
    }
    
    /**
     * 解析震动控制命令
     */
    private fun parseVibrationCommands(normalizedText: String, originalText: String): List<BluetoothControlCommand> {
        val commands = mutableListOf<BluetoothControlCommand>()
        
        // 震动强度模式
        val intensityPatterns = listOf(
            Pattern.compile("震动强度(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("振动强度(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("震动(\\d+)%", Pattern.CASE_INSENSITIVE),
            Pattern.compile("振动(\\d+)%", Pattern.CASE_INSENSITIVE),
            Pattern.compile("intensity\\s*(\\d+)", Pattern.CASE_INSENSITIVE)
        )
        
        // 震动模式
        val modePatterns = listOf(
            Pattern.compile("连续震动", Pattern.CASE_INSENSITIVE),
            Pattern.compile("脉冲震动", Pattern.CASE_INSENSITIVE),
            Pattern.compile("波浪震动", Pattern.CASE_INSENSITIVE),
            Pattern.compile("continuous", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pulse", Pattern.CASE_INSENSITIVE),
            Pattern.compile("wave", Pattern.CASE_INSENSITIVE)
        )
        
        // 持续时间
        val durationPatterns = listOf(
            Pattern.compile("持续(\\d+)秒", Pattern.CASE_INSENSITIVE),
            Pattern.compile("持续(\\d+)分钟", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)秒", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)分钟", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s*seconds?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s*minutes?", Pattern.CASE_INSENSITIVE)
        )
        
        // 检查是否包含震动相关词汇
        val vibrationKeywords = listOf("震动", "振动", "vibration", "vibrate")
        if (!vibrationKeywords.any { normalizedText.contains(it) }) {
            return commands
        }
        
        // 提取强度
        var intensity = 6 // 默认强度
        for (pattern in intensityPatterns) {
            val matcher = pattern.matcher(originalText)
            if (matcher.find()) {
                intensity = matcher.group(1).toIntOrNull() ?: 50
                break
            }
        }
        
        // 提取模式
        var mode = 1 // 默认连续模式
        when {
            normalizedText.contains("脉冲") || normalizedText.contains("pulse") -> mode = 2
            normalizedText.contains("波浪") || normalizedText.contains("wave") -> mode = 3
        }
        
        // 提取持续时间
        var duration = 0L
        for (pattern in durationPatterns) {
            val matcher = pattern.matcher(originalText)
            if (matcher.find()) {
                val value = matcher.group(1).toIntOrNull() ?: 0
                duration = if (pattern.pattern().contains("分钟") || pattern.pattern().contains("minutes?")) {
                    value * 60 * 1000L
                } else {
                    value * 1000L
                }
                break
            }
        }
        
        // 检查停止命令
        if (normalizedText.contains("停止震动") || normalizedText.contains("停止振动") || 
            normalizedText.contains("stop vibration")) {
            intensity = 0
            mode = 0
        }
        
        val command = BluetoothControlCommand(
            action = BluetoothAction.CONTROL_VIBRATION,
            deviceId = "default", // 默认设备ID
            parameters = mapOf(
                "motor_id" to 0,
                "mode" to mode,
                "intensity" to intensity,
                "duration" to duration
            ),
            priority = 5
        )
        
        commands.add(command)
        return commands
    }
    
    /**
     * 解析加热控制命令
     */
    private fun parseHeatingCommands(normalizedText: String, originalText: String): List<BluetoothControlCommand> {
        val commands = mutableListOf<BluetoothControlCommand>()
        
        // 温度模式
        val temperaturePatterns = listOf(
            Pattern.compile("温度(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("加热到(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)度", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s*celsius?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("temperature\\s*(\\d+)", Pattern.CASE_INSENSITIVE)
        )
        
        // 持续时间
        val durationPatterns = listOf(
            Pattern.compile("持续(\\d+)秒", Pattern.CASE_INSENSITIVE),
            Pattern.compile("持续(\\d+)分钟", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)秒", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)分钟", Pattern.CASE_INSENSITIVE)
        )
        
        // 检查是否包含加热相关词汇
        val heatingKeywords = listOf("加热", "温度", "heating", "temperature", "heat")
        if (!heatingKeywords.any { normalizedText.contains(it) }) {
            return commands
        }
        
        // 提取温度
        var temperature = 37 // 默认温度
        for (pattern in temperaturePatterns) {
            val matcher = pattern.matcher(originalText)
            if (matcher.find()) {
                temperature = matcher.group(1).toIntOrNull() ?: 37
                break
            }
        }
        
        // 提取持续时间
        var duration = 0L
        for (pattern in durationPatterns) {
            val matcher = pattern.matcher(originalText)
            if (matcher.find()) {
                val value = matcher.group(1).toIntOrNull() ?: 0
                duration = if (pattern.pattern().contains("分钟")) {
                    value * 60 * 1000L
                } else {
                    value * 1000L
                }
                break
            }
        }
        
        // 检查停止命令
        val status = if (normalizedText.contains("停止加热") || normalizedText.contains("stop heating")) {
            0
        } else {
            1
        }
        
        val command = BluetoothControlCommand(
            action = BluetoothAction.CONTROL_HEATING,
            deviceId = "default",
            parameters = mapOf(
                "status" to status,
                "temperature" to temperature,
                "heater_id" to 0,
                "duration" to duration
            ),
            priority = 5
        )
        
        commands.add(command)
        return commands
    }
    
    /**
     * 解析组合控制命令
     */
    private fun parseCombinedCommands(normalizedText: String, originalText: String): List<BluetoothControlCommand> {
        val commands = mutableListOf<BluetoothControlCommand>()
        
        // 检查是否包含组合控制关键词
        val combinedKeywords = listOf("同时", "一起", "both", "together", "同时进行")
        if (!combinedKeywords.any { normalizedText.contains(it) }) {
            return commands
        }
        
        // 检查是否同时包含震动和加热
        val hasVibration = listOf("震动", "振动", "vibration").any { normalizedText.contains(it) }
        val hasHeating = listOf("加热", "温度", "heating").any { normalizedText.contains(it) }
        
        if (hasVibration && hasHeating) {
            val combinedCommands = mutableListOf<Map<String, Any>>()
            
            // 添加震动命令
            combinedCommands.add(mapOf(
                "action" to "vibration",
                "motor_id" to 0,
                "mode" to 1,
                "intensity" to 50
            ))
            
            // 添加加热命令
            combinedCommands.add(mapOf(
                "action" to "heating",
                "status" to 1,
                "temperature" to 37,
                "heater_id" to 0
            ))
            
            val command = BluetoothControlCommand(
                action = BluetoothAction.COMBINED_CONTROL,
                deviceId = "default",
                parameters = mapOf("commands" to combinedCommands),
                priority = 7
            )
            
            commands.add(command)
        }
        
        return commands
    }
    
    /**
     * 解析设备管理命令
     */
    private fun parseDeviceCommands(normalizedText: String, originalText: String): List<BluetoothControlCommand> {
        val commands = mutableListOf<BluetoothControlCommand>()
        
        when {
            normalizedText.contains("扫描") || normalizedText.contains("scan") -> {
                commands.add(BluetoothControlCommand(
                    action = BluetoothAction.SCAN_DEVICES,
                    deviceId = "default",
                    priority = 8
                ))
            }
            normalizedText.contains("连接") || normalizedText.contains("connect") -> {
                commands.add(BluetoothControlCommand(
                    action = BluetoothAction.CONNECT_DEVICE,
                    deviceId = "default",
                    priority = 8
                ))
            }
            normalizedText.contains("断开") || normalizedText.contains("disconnect") -> {
                commands.add(BluetoothControlCommand(
                    action = BluetoothAction.DISCONNECT_DEVICE,
                    deviceId = "default",
                    priority = 8
                ))
            }
            normalizedText.contains("电量") || normalizedText.contains("battery") -> {
                commands.add(BluetoothControlCommand(
                    action = BluetoothAction.QUERY_BATTERY_STATUS,
                    deviceId = "default",
                    priority = 6
                ))
            }
            normalizedText.contains("设备信息") || normalizedText.contains("device info") -> {
                commands.add(BluetoothControlCommand(
                    action = BluetoothAction.QUERY_DEVICE_INFO,
                    deviceId = "default",
                    priority = 6
                ))
            }
        }
        
        return commands
    }
    
    /**
     * 计算置信度
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun calculateConfidence(text: String, commands: List<BluetoothControlCommand>): Float {
        if (commands.isEmpty()) return 0.0f
        
        var confidence = 0.5f // 基础置信度
        
        // 根据关键词数量增加置信度
        val keywordCount = listOf("震动", "振动", "加热", "温度", "控制", "开启", "停止").count { text.contains(it) }
        confidence += keywordCount * 0.1f
        
        // 根据数字参数增加置信度
        val numberPattern = Pattern.compile("\\d+")
        val numberCount = numberPattern.matcher(text).results().count()
        confidence += numberCount * 0.05f
        
        // 根据命令数量调整置信度
        if (commands.size > 1) {
            confidence += 0.1f
        }
        
        return confidence.coerceAtMost(1.0f)
    }
    
    /**
     * 识别意图
     */
    private fun identifyIntent(text: String): String {
        return when {
            text.contains("震动") || text.contains("振动") -> "vibration_control"
            text.contains("加热") || text.contains("温度") -> "heating_control"
            text.contains("扫描") || text.contains("连接") -> "device_management"
            text.contains("同时") || text.contains("一起") -> "combined_control"
            text.contains("停止") || text.contains("关闭") -> "stop_control"
            else -> "general_control"
        }
    }
    
    /**
     * 提取参数
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun extractParameters(text: String): Map<String, Any> {
        val params = mutableMapOf<String, Any>()
        
        // 提取数字参数
        val numberPattern = Pattern.compile("\\d+")
        val numbers = numberPattern.matcher(text).results().map { it.group().toInt() }.toList()
        if (numbers.isNotEmpty()) {
            params["numbers"] = numbers
        }
        
        // 提取时间相关参数
        val timePattern = Pattern.compile("(\\d+)(秒|分钟|秒|minute|second)")
        val timeMatches = timePattern.matcher(text).results().toList()
        if (timeMatches.isNotEmpty()) {
            params["time_values"] = timeMatches.map { it.group() }
        }
        
        return params
    }
    
    // 通知方法
    private fun notifyCommandParsed(result: LLMResponseParseResult) {
        eventListeners.forEach { it.onCommandParsed(result) }
    }
    
    private fun notifyParseError(error: String, originalText: String) {
        eventListeners.forEach { it.onParseError(error, originalText) }
    }
}

