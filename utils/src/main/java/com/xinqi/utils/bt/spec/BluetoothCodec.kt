package com.xinqi.utils.bt.spec

import com.xinqi.utils.log.logE
import java.nio.ByteBuffer
import java.nio.ByteOrder


object BluetoothCodec {

    /**
     * 查询设备信息
     */
    fun createQueryDeviceInfoPacket(): SvakomPacket {
        return SvakomPacket(command = BluetoothProtocol.Commands.CMD_QUERY_DEVICE_INFO)
    }

    /**
     * 查询设备电量
     */
    fun createQueryBatteryStatusPacket(): SvakomPacket {
        return SvakomPacket(command = BluetoothProtocol.Commands.CMD_QUERY_BATTERY_STATUS)
    }

    /**
     * 震动控制
     */
    fun createVibrationControlPacket(control: VibrationControl): SvakomPacket {
        return SvakomPacket(
            command = BluetoothProtocol.Commands.CMD_VIBRATION_CONTROL,
            param1 = control.motorId,
            param2 = control.mode,
            param3 = control.intensity
        )
    }

    /**
     * 实时强度控制
     */
    fun createRealtimeIntensityPacket(control: RealtimeIntensityControl): SvakomPacket {
        return SvakomPacket(
            command = BluetoothProtocol.Commands.CMD_REALTIME_INTENSITY,
            param1 = control.motorMask,
            param2 = control.intensity
        )
    }

    /**
     * 加热控制
     */
    fun createHeatingControlPacket(control: HeatingControl): SvakomPacket {
        return SvakomPacket(
            command = BluetoothProtocol.Commands.CMD_HEATING_CONTROL,
            param1 = control.status,
            param2 = control.temperature,
            param3 = control.heaterId
        )
    }

    /**
     * 特殊模式控制
     */
    fun createSpecialModeControlPacket(control: SpecialModeControl): SvakomPacket {
        return SvakomPacket(
            command = BluetoothProtocol.Commands.CMD_SPECIAL_MODE_CONTROL,
            param1 = control.status,
            param2 = control.modeId
        )
    }

    /**
     * LED颜色控制
     */
    fun createLedColorControlPacket(control: LedColorControl): SvakomPacket {
        return SvakomPacket(
            command = BluetoothProtocol.Commands.CMD_LED_COLOR_CONTROL,
            param1 = control.ledId,
            param2 = control.red,
            param3 = control.green,
            param4 = control.blue
        )
    }

    /**
     * 寻物
     */
    fun createFindDeviceSoundPacket(enable: Boolean): SvakomPacket {
        return SvakomPacket(
            command = BluetoothProtocol.Commands.CMD_FIND_DEVICE_SOUND,
            param1 = if (enable) 1 else 0
        )
    }

    /**
     * OTA升级开始
     */
    fun createOtaStartPacket(): SvakomPacket {
        return SvakomPacket(
            command = BluetoothProtocol.Commands.CMD_OTA_UPGRADE,
            param1 = BluetoothProtocol.Commands.OTA_START
        )
    }

    /**
     * OTA数据传输
     */
    fun createOtaDataPacket(frameIndex: Int, data: ByteArray): SvakomPacket {
        return SvakomPacket(
            command = BluetoothProtocol.Commands.CMD_OTA_UPGRADE,
            param1 = BluetoothProtocol.Commands.OTA_DATA_TRANSFER,
            param2 = frameIndex and 0xFF,
            param3 = if (data.isNotEmpty()) data[0].toInt() and 0xFF else 0,
            param4 = if (data.size > 1) data[1].toInt() and 0xFF else 0,
            param5 = if (data.size > 2) data[2].toInt() and 0xFF else 0
        )
    }

    /**
     * 解析设备信息响应
     */
    fun parseDeviceInfoResponse(packet: SvakomPacket): DeviceInfoResponse? {
        if (packet.command != BluetoothProtocol.Commands.CMD_QUERY_DEVICE_INFO) return null

        //这里需要根据实际协议定义解析参数
        val version = "${packet.param1}.${packet.param2}.${packet.param3}"
        val productCode = packet.param4
        val uid = byteArrayOf(packet.param5.toByte()) //简化处理

        return DeviceInfoResponse(version, productCode, uid)
    }

    /**
     * 解析电量状态响应
     */
    fun parseBatteryStatusResponse(packet: SvakomPacket): BatteryStatusResponse? {
        if (packet.command != BluetoothProtocol.Commands.CMD_QUERY_BATTERY_STATUS) return null

        return BatteryStatusResponse(
            device1Battery = packet.param1,
            device1Status = packet.param2,
            device2Battery = packet.param3,
            device2Status = packet.param4
        )
    }

    /**
     * 解析广播数据
     */
    fun parseBroadcastData(manufacturerData: ByteArray): BroadcastData? {
        if (manufacturerData.size < 16) {
            logE("manufacturerData小于16位")
            return null
        }

        val buffer = ByteBuffer.wrap(manufacturerData)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val identifier = buffer.int
        if (identifier != BluetoothProtocol.BROADCAST_IDENTIFIER) {
            logE("身份辨识不是SVA2")
            return null
        }

        val protocolVersion = buffer.get().toInt() and 0xFF
        val productCode = buffer.get().toInt() and 0xFF

        val uid = ByteArray(6)
        buffer.get(uid)

        val companyId = buffer.get().toInt() and 0xFF
        val productionBatch = buffer.get().toInt() and 0xFF
        val internalVersion = buffer.get().toInt() and 0xFF

        return BroadcastData(
            identifier = identifier,
            protocolVersion = protocolVersion,
            productCode = productCode,
            uid = uid,
            companyId = companyId,
            productionBatch = productionBatch,
            internalVersion = internalVersion
        )
    }
}