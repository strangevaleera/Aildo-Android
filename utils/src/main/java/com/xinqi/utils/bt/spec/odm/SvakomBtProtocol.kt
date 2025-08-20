package com.xinqi.utils.bt.spec.odm

import com.xinqi.utils.log.logE

/**
 * 蓝牙协议 基于SVAKOM蓝牙协议V2的数据结构和常量:
 * @see <a href="https://pcni5ldjg9t4.feishu.cn/wiki/QGNjwmy9Ui8r18k6o4IcPbzVn8f?fromScene=spaceOverview#share-NK14dWk1ToYfNCxct7fcw7usnff"> svakom蓝牙协议
 * @see <a href="https://pcni5ldjg9t4.feishu.cn/wiki/ZyT1wRiCXiGkiCkCD9HcJjUnnQg"> xinqi固件要求
 */
object BluetoothProtocol {
    
    const val PROTOCOL_VERSION = 2
    const val PROTOCOL_HEADER = 0x5A
    const val PACKET_SIZE = 7 //限制统一7bytes数据包
    const val BROADCAST_IDENTIFIER = 0x58515431 //限制广播数据开头“XQT1”
    
    object UUID {
        const val SERVICE_UUID = "0000FFF0-0000-1000-8000-00805F9B34FB"
        const val WRITE_CHARACTERISTIC_UUID = "0000FFF1-0000-1000-8000-00805F9B34FB"  //无响应写
        const val READ_NOTIFY_CHARACTERISTIC_UUID = "0000FFF2-0000-1000-8000-00805F9B34FB"  //读/通知
    }
    
    //公司ID定义
    //妙乐(0x26)，奥古（0x27）,巨鑫（0x28），巾斗云（0x29），杰士邦（0x30），嘉通盛达（0x31），时运来（0x32）, 杰弘(0x33)，建贸电子（0x34)，捷昌（0X35），新创美（0x36），尊旭（0x37），润色（0x38），中策（0x39），煜琅（0x40），爱哒（0x41），锂余（0x42）,荣盛全（0x43）,彼爱丝 （0x44）
    object CompanyId {
        /*const val JIE_HONG = 0x33
        const val JIAN_MAO = 0x34
        const val JIE_CHANG = 0x35
        const val XIN_CHUANG_MEI = 0x36*/
        const val XIN_QI = 0x45
    }
    
    //核心功能指令集
    object Commands {
        const val CMD_QUERY_DEVICE_INFO = 0x00        // 查询设备信息
        const val CMD_QUERY_BATTERY_STATUS = 0x02     // 查询电量及状态
        const val CMD_VIBRATION_CONTROL = 0x03        // 控制震动
        const val CMD_REALTIME_INTENSITY = 0x04       // 实时强度控制
        const val CMD_HEATING_CONTROL = 0x05          // 加热控制
        const val CMD_SPECIAL_MODE_CONTROL = 0x12     // 特殊模式控制
        const val CMD_LED_COLOR_CONTROL = 0xA1        // LED颜色控制
        const val CMD_FIND_DEVICE_SOUND = 0xA2        // 寻物声响控制
        const val CMD_OTA_UPGRADE = 0x5B              // OTA升级
        const val CMD_PRESSURE_CTX = 0xF0 // 压力传感器上报
        const val CMD_DEVICE_CTX = 0xF1 // 设备状态同步
        const val CMD_PHYSICAL_BUTTON_FEEDBACK = 0xFE // 物理按钮反馈
        const val CMD_ERROR_FEEDBACK = 0xFF           // 通用错误反馈
        
        //OTA指令
        const val OTA_START = 0x01
        const val OTA_DATA_TRANSFER = 0x02
    }
    
    //马达控制
    object Motor {
        const val MOTOR_ALL = 0x00
        const val MOTOR_1 = 0x01
        const val MOTOR_2 = 0x02
        const val MOTOR_3 = 0x03
    }
    
    //震动模式
    object VibrationMode {
        const val MODE_STOP = 0x00
        const val MODE_CONSTANT = 0x01
        const val MODE_PULSE = 0x02
        const val MODE_ESCALATING = 0x03
        const val MODE_WAVE = 0x04
    }
    
    //特殊模式编号
    object SpecialMode {
        const val MODE_SOOTHING = 0x00     // 舒缓
        const val MODE_INTELLIGENT = 0x01  // 智能
        const val MODE_FLAPPING = 0x02   //拍打模式
        const val MODE_VIBRATION = 0x03   //震动模式
        const val MODE_SILENT = 0xFF       // 静默模式
    }
    
    //加热控制
    object Heating {
        const val HEATING_OFF = 0x00
        const val HEATING_ON = 0x01
        const val TEMP_MIN = 38
        const val TEMP_MAX = 55
    }
    
    //设备状态
    object DeviceStatus {
        const val STATUS_OFF = 0x00
        const val STATUS_ON = 0x01
    }
    
    //连接
    object ConnectionState {
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        const val STATE_DISCONNECTING = 3
    }

    object ErrorCodes {
        const val ERROR_NONE = 0x00
        const val ERROR_UNSUPPORTED_COMMAND = 0x01    // 指令不支持
        const val ERROR_DEVICE1_OFF = 0x02            // 大头关机
        const val ERROR_DEVICE2_OFF = 0x03            // 小头关机
        const val ERROR_INVALID_PARAM = 0x04          // 参数错误
        const val ERROR_TIMEOUT = 0x05
        const val ERROR_DISCONNECTED = 0x06
        const val ERROR_BUSY = 0x07
        const val ERROR_BIND_FAILED = 0x08
        const val ERROR_CONNECT_FAILED = 0x09
        const val ERROR_OTA_FAILED = 0x0A
    }
    
    //通知编码（物理按钮反馈）
    object NotificationCodes {
        const val NOTIFICATION_VIBRATION_CHANGED = 0x01
        const val NOTIFICATION_HEATING_CHANGED = 0x02
        const val NOTIFICATION_MODE_CHANGED = 0x03
        const val NOTIFICATION_INTENSITY_CHANGED = 0x04
        const val NOTIFICATION_POWER_CHANGED = 0x05
    }
    
    object DeviceType {
        const val TYPE_UNKNOWN = 0
        const val TYPE_SVAKOM = 1
    }
    
    object BindStatus {
        const val STATUS_UNBOUND = 0
        const val STATUS_BOUND = 1
        const val STATUS_BINDING = 2
    }
}

/**
 * 蓝牙广播数据包
 *
 * { data1，data2，data3, 协议版本，产品编码，
 * uid1，uid2，uid3，uid4，uid5，uid6，功能字节, 公司ID
 * 产品唯一编码1，产品唯一编码2，产品唯一编码3，参见登记表。例如：0x000001，此处填充0x00, 0x00, 0x01
 * year，month，day，
 * 研发内部版本低，研发内部版本高 :由固件开发者按需自由填写
 *
 * 注意：8和9，这5个字节，供应商自由支配，预留的初衷是方便盘点库存
 * }
 *
 * @param identifier 1.	data1，data2, data3 : 0x58, 0x51, 0x54, 0x31 ->'X' 'Q' 'T' '1'
 * @param protocolVersion 2.	协议版本，本文档版本传`2`   用以后续协议不得不分化时候的判断标识
 * @param productCode    3.   产品编码：此字节作废，填充FF
 * @param uid  4.   全局唯一ID，6个字节，第1个字节为供应商公司ID，后5个字节为流水号（或芯片序列号，能保证唯一即可），需保证出厂后，每个单独玩具的UID都互不相同（不同供应商之间也不能重复），且固定的（OTA升级后也需保持不变）
 * @param companyId 5. 公司标识 1个字节 申请0x45
 * @param productionBatch 6. 年月日每个占一个字节（年需取后两位数，即2024为24）
 */
data class BroadcastData(
    val identifier: Int = BluetoothProtocol.BROADCAST_IDENTIFIER,     // 识别标识 "SVA2"
    val protocolVersion: Int = BluetoothProtocol.PROTOCOL_VERSION,    // 协议版本
    val productCode: Int = 0xFF,                    // 产品编码（写死0xFF）
    val uid: ByteArray,                             // 6字节UID（唯一标识）
    val companyId: Int = BluetoothProtocol.CompanyId.XIN_QI, // 公司ID
    val productionBatch: Int = 0,                       // optional 生产批次（年月日）
    val internalVersion: Int = 0                        // optional 研发内部版本号
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as BroadcastData
        return identifier == other.identifier &&
                protocolVersion == other.protocolVersion &&
                productCode == other.productCode &&
                uid.contentEquals(other.uid) &&
                companyId == other.companyId &&
                productionBatch == other.productionBatch &&
                internalVersion == other.internalVersion
    }
    
    override fun hashCode(): Int {
        var result = identifier
        result = 31 * result + protocolVersion
        result = 31 * result + productCode
        result = 31 * result + uid.contentHashCode()
        result = 31 * result + companyId
        result = 31 * result + productionBatch
        result = 31 * result + internalVersion
        return result
    }
}

/**
 * 设备信息响应
 */
data class DeviceInfoResponse(
    val firmwareVersion: String,
    val productCode: Int,
    val uid: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as DeviceInfoResponse
        return firmwareVersion == other.firmwareVersion &&
                productCode == other.productCode &&
                uid.contentEquals(other.uid)
    }
    
    override fun hashCode(): Int {
        var result = firmwareVersion.hashCode()
        result = 31 * result + productCode
        result = 31 * result + uid.contentHashCode()
        return result
    }
}

/**
 * 电量状态响应
 */
data class BatteryStatusResponse(
    val device1Battery: Int,    // 设备1电量 (1-100)
    val device1Status: Int,     // 设备1状态 (0关机/1开机)
    val device2Battery: Int,    // 设备2电量 (1-100)
    val device2Status: Int      // 设备2状态 (0关机/1开机)
)

/**
 * 震动控制指令
 */
data class VibrationControl(
    val motorId: Int,       // 马达编号 (0全动/1-3单独控制)
    val mode: Int,          // 模式
    val intensity: Int      // 强度
)

/**
 * 实时强度控制指令
 */
data class RealtimeIntensityControl(
    val motorMask: Int,     // 马达掩码
    val intensity: Int      // 强度 (0-255)
)

/**
 * 加热控制指令
 */
data class HeatingControl(
    val status: Int,        // 开关 (0关/1开)
    val temperature: Int,   // 温度 (38-55度)
    val heaterId: Int = 0   // 加热器编号
)

/**
 * 特殊模式控制指令
 */
data class SpecialModeControl(
    val status: Int,        // 状态 (0关/1开)
    val modeId: Int         // 模式编号
)

/**
 * LED颜色控制指令
 */
data class LedColorControl(
    val ledId: Int,         // 灯编号
    val red: Int,           // R通道值 (0-255)
    val green: Int,         // G通道值 (0-255)
    val blue: Int           // B通道值 (0-255)
)

/**
 * BLE SVAKOM协议数据包结构（7字节）
 */
data class SvakomPacket(
    val header: Int = BluetoothProtocol.PROTOCOL_HEADER,  // 协议头 0x5A
    val command: Int,                   // 指令编码
    val param1: Int = 0,                // 参数1
    val param2: Int = 0,                // 参数2
    val param3: Int = 0,                // 参数3
    val param4: Int = 0,                // 参数4
    val param5: Int = 0                 // 参数5
) {
    fun toByteArray(): ByteArray {
        return byteArrayOf(
            header.toByte(),
            command.toByte(),
            param1.toByte(),
            param2.toByte(),
            param3.toByte(),
            param4.toByte(),
            param5.toByte()
        )
    }
    
    companion object {
        fun fromByteArray(data: ByteArray): SvakomPacket? {
            if (data.size != BluetoothProtocol.PACKET_SIZE) {
                logE("size error, not equals to :${BluetoothProtocol.PACKET_SIZE}")
                return null
            }
            if (data[0].toInt() and 0xFF != BluetoothProtocol.PROTOCOL_HEADER) {
                logE("header error, not equals to :${BluetoothProtocol.PROTOCOL_HEADER}")
                return null
            }
            
            return SvakomPacket(
                header = data[0].toInt() and 0xFF,
                command = data[1].toInt() and 0xFF,
                param1 = data[2].toInt() and 0xFF,
                param2 = data[3].toInt() and 0xFF,
                param3 = data[4].toInt() and 0xFF,
                param4 = data[5].toInt() and 0xFF,
                param5 = data[6].toInt() and 0xFF
            )
        }
    }
}

////////////////
/**
 * 绑定数据
 */
data class BindData(
    val deviceId: String,
    val deviceName: String,
    val deviceType: Int,
    val bindTime: Long,
    val status: Int,
    val reserved: ByteArray = ByteArray(8)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BindData
        return deviceId == other.deviceId &&
                deviceName == other.deviceName &&
                deviceType == other.deviceType &&
                bindTime == other.bindTime &&
                status == other.status &&
                reserved.contentEquals(other.reserved)
    }

    override fun hashCode(): Int {
        var result = deviceId.hashCode()
        result = 31 * result + deviceName.hashCode()
        result = 31 * result + deviceType
        result = 31 * result + bindTime.hashCode()
        result = 31 * result + status
        result = 31 * result + reserved.contentHashCode()
        return result
    }
}

/**
 * 事件数据
 */
data class EventData(
    val eventId: Int,
    val timestamp: Long,
    val dataLen: Int,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EventData
        return eventId == other.eventId &&
                timestamp == other.timestamp &&
                dataLen == other.dataLen &&
                data.contentEquals(other.data)
    }
    
    override fun hashCode(): Int {
        var result = eventId
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + dataLen
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * 流数据结构
 */
data class StreamData(
    val streamId: Int,
    val timestamp: Long,
    val dataLen: Int,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as StreamData
        return streamId == other.streamId &&
                timestamp == other.timestamp &&
                dataLen == other.dataLen &&
                data.contentEquals(other.data)
    }
    
    override fun hashCode(): Int {
        var result = streamId
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + dataLen
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * 连接参数
 */
data class ConnectionParams(
    val interval: Int = 20,      // 连接间隔 (ms)
    val latency: Int = 0,        // 延迟
    val timeout: Int = 500,      // 超时 (ms)
    val mtu: Int = 23,           // MTU大小
    val phy: Int = 1             // PHY类型
)
