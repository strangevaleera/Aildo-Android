package com.xinqi.utils.mcp

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * MCP (Model Context Protocol) 协议定义
 * 用于LLM与蓝牙设备控制之间的通信协议
 */

/**
 * MCP消息基类
 */
sealed class MCPMessage {
    abstract val id: String
    abstract val type: String
    abstract val timestamp: Long
}

/**
 * MCP请求消息
 */
data class MCPRequest(
    override val id: String = UUID.randomUUID().toString(),
    override val type: String,
    override val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val params: Map<String, Any> = emptyMap(),
    val context: MCPContext? = null
) : MCPMessage()

/**
 * MCP响应消息
 */
data class MCPResponse(
    override val id: String,
    override val type: String = "response",
    override val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean,
    val result: Any? = null,
    val error: MCPError? = null
) : MCPMessage()

/**
 * MCP错误信息
 */
data class MCPError(
    val code: Int,
    val message: String,
    val details: Map<String, Any> = emptyMap()
)

/**
 * MCP上下文信息
 */
data class MCPContext(
    val sessionId: String,
    val userId: String? = null,
    val deviceId: String? = null,
    val conversationId: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * MCP通知消息
 */
data class MCPNotification(
    override val id: String = UUID.randomUUID().toString(),
    override val type: String = "notification",
    override val timestamp: Long = System.currentTimeMillis(),
    val event: String,
    val data: Any? = null
) : MCPMessage()

/**
 * 蓝牙控制命令
 */
data class BluetoothControlCommand(
    val action: BluetoothAction,
    val deviceId: String,
    val parameters: Map<String, Any> = emptyMap(),
    val priority: Int = 0, // 优先级，0-10，10最高
    val timeout: Long = 5000L // 超时时间（毫秒）
)

/**
 * 蓝牙操作类型
 */
enum class BluetoothAction(val value: String) {
    // 设备管理
    SCAN_DEVICES("scan_devices"),
    CONNECT_DEVICE("connect_device"),
    DISCONNECT_DEVICE("disconnect_device"),
    QUERY_DEVICE_INFO("query_device_info"),
    QUERY_BATTERY_STATUS("query_battery_status"),
    
    // 震动控制
    CONTROL_VIBRATION("control_vibration"),
    START_VIBRATION("start_vibration"),
    STOP_VIBRATION("stop_vibration"),
    SET_VIBRATION_INTENSITY("set_vibration_intensity"),
    
    // 加热控制
    CONTROL_HEATING("control_heating"),
    START_HEATING("start_heating"),
    STOP_HEATING("stop_heating"),
    SET_TEMPERATURE("set_temperature"),
    
    // 组合控制
    COMBINED_CONTROL("combined_control"),
    
    // 状态查询
    GET_DEVICE_STATUS("get_device_status"),
    GET_CONNECTION_STATUS("get_connection_status")
}

/**
 * 震动控制参数
 */
data class VibrationParams(
    val motorId: Int = 0,
    val mode: Int = 1, // 1-连续震动，2-脉冲震动，3-波浪震动
    val intensity: Int = 50, // 0-100
    val duration: Long = 0L, // 持续时间（毫秒），0表示持续
    val pattern: List<Int> = emptyList() // 自定义震动模式
)

/**
 * 加热控制参数
 */
data class HeatingParams(
    val heaterId: Int = 0,
    val temperature: Int = 37, // 目标温度（摄氏度）
    val duration: Long = 0L, // 持续时间（毫秒），0表示持续
    val autoStop: Boolean = true // 是否自动停止
)

/**
 * 设备状态信息
 */
data class DeviceStatus(
    val deviceId: String,
    val isConnected: Boolean,
    val batteryLevel: Int? = null,
    val temperature: Int? = null,
    val vibrationIntensity: Int? = null,
    val heatingStatus: Boolean? = null,
    val lastUpdateTime: Long = System.currentTimeMillis()
)

/**
 * LLM回复解析结果
 */
data class LLMResponseParseResult(
    val hasBluetoothCommand: Boolean,
    val commands: List<BluetoothControlCommand> = emptyList(),
    val confidence: Float = 0.0f,
    val originalText: String,
    val parsedIntent: String? = null,
    val extractedParams: Map<String, Any> = emptyMap()
)

/**
 * MCP协议版本
 */
object MCPVersion {
    const val VERSION = "1.0.0"
    const val PROTOCOL_NAME = "MCP-Bluetooth-Control"
}

/**
 * MCP消息序列化工具
 */
object MCPMessageSerializer {
    private val gson = Gson()
    
    fun serialize(message: MCPMessage): String {
        return gson.toJson(message)
    }
    
    fun deserializeRequest(json: String): MCPRequest? {
        return try {
            gson.fromJson(json, MCPRequest::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun deserializeResponse(json: String): MCPResponse? {
        return try {
            gson.fromJson(json, MCPResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun deserializeNotification(json: String): MCPNotification? {
        return try {
            gson.fromJson(json, MCPNotification::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * MCP错误代码定义
 */
object MCPErrorCodes {
    const val SUCCESS = 0
    const val INVALID_REQUEST = 1001
    const val METHOD_NOT_FOUND = 1002
    const val INVALID_PARAMS = 1003
    const val INTERNAL_ERROR = 1004
    const val BLUETOOTH_NOT_AVAILABLE = 2001
    const val DEVICE_NOT_FOUND = 2002
    const val DEVICE_NOT_CONNECTED = 2003
    const val CONNECTION_FAILED = 2004
    const val COMMAND_EXECUTION_FAILED = 2005
    const val PERMISSION_DENIED = 2006
    const val TIMEOUT = 2007
    const val INVALID_COMMAND = 2008
    const val DEVICE_BUSY = 2009
    const val BATTERY_LOW = 2010
}

