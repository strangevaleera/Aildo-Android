package com.xinqi.ui.bluetooth

import kotlinx.coroutines.flow.StateFlow

/**
 * 蓝牙通信管理器接口
 * 定义与蓝牙设备通信的基本功能
 */
interface BluetoothCommunicationManager {
    
    /**
     * 蓝牙连接状态
     */
    val connectionState: StateFlow<BluetoothConnectionState>
    
    /**
     * 当前连接的设备信息
     */
    val connectedDevice: StateFlow<BluetoothDeviceInfo?>

    suspend fun connectToDevice(deviceAddress: String): Result<Unit>

    suspend fun disconnect(): Result<Unit>
    
    /**
     * 发送指令到蓝牙设备
     */
    suspend fun sendCommand(command: BluetoothCommand): Result<Unit>
    
    /**
     * 发送自定义数据包
     */
    suspend fun sendCustomPacket(packet: ByteArray): Result<Unit>
    
    /**
     * 扫描可用的蓝牙设备
     */
    suspend fun scanForDevices(): Result<List<BluetoothDeviceInfo>>

    fun stopScan()
}

/**
 * 蓝牙连接状态
 */
enum class BluetoothConnectionState {
    DISCONNECTED,    // 未连接
    CONNECTING,      // 连接中
    CONNECTED,       // 已连接
    CONNECTION_FAILED, // 连接失败
    DISCONNECTING    // 断开连接中
}

/**
 * 蓝牙设备信息
 */
data class BluetoothDeviceInfo(
    val address: String,
    val name: String?,
    val rssi: Int,
    val isConnected: Boolean = false
)

sealed class BluetoothCommand {
    data class BodyPartTouch(
        val part: String,
        val x: Float,
        val y: Float
    ) : BluetoothCommand()
    
    data class AnimationControl(
        val animationType: String,
        val character: String
    ) : BluetoothCommand()
    
    data class CustomCommand(
        val command: String,
        val parameters: Map<String, Any> = emptyMap()
    ) : BluetoothCommand()
    
    data class VibrationControl(
        val intensity: Int, // 0-100
        val duration: Long  // 毫秒
    ) : BluetoothCommand()
    
    data class HeatingControl(
        val temperature: Float, // 摄氏度
        val duration: Long      // 毫秒
    ) : BluetoothCommand()
}

/**
 * 蓝牙通信结果
 */
sealed class BluetoothResult<out T> {
    data class Success<T>(val data: T) : BluetoothResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : BluetoothResult<Nothing>()
}

object BluetoothCommandBuilder {

    fun buildBodyPartTouchCommand(part: String, x: Float, y: Float): BluetoothCommand {
        return BluetoothCommand.BodyPartTouch(part, x, y)
    }

    fun buildAnimationControlCommand(animationType: String, character: String): BluetoothCommand {
        return BluetoothCommand.AnimationControl(animationType, character)
    }

    fun buildVibrationCommand(intensity: Int, duration: Long): BluetoothCommand {
        return BluetoothCommand.VibrationControl(intensity, duration)
    }

    fun buildHeatingCommand(temperature: Float, duration: Long): BluetoothCommand {
        return BluetoothCommand.HeatingControl(temperature, duration)
    }
    
    /**
     * 构建自定义指令
     */
    fun buildCustomCommand(command: String, parameters: Map<String, Any> = emptyMap()): BluetoothCommand {
        return BluetoothCommand.CustomCommand(command, parameters)
    }
}

