package com.xinqi.utils.bt

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import com.xinqi.utils.bt.spec.odm.BroadcastData

/**
 * 蓝牙设备信息
 * 包含设备基本信息和广播数据
 */

data class BluetoothDeviceInfo(
    val device: BluetoothDevice,
    val broadcastData: BroadcastData? = null,
    val rssi: Int = 0,
    val scanTime: Long = System.currentTimeMillis()
) {
    val deviceName: String
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        get() = device.name ?: "未知设备"
    
    val deviceAddress: String
        get() = device.address
    
    val deviceType: String
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        get() = when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "经典蓝牙"
            BluetoothDevice.DEVICE_TYPE_LE -> "低功耗蓝牙"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "双模蓝牙"
            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "未知类型"
            else -> "未知类型"
        }
    
    val isSVAKOMDevice: Boolean
        get() = broadcastData?.companyId == com.xinqi.utils.bt.spec.odm.BluetoothProtocol.CompanyId.XIN_QI
    
    val companyName: String
        get() = when (broadcastData?.companyId) {
            com.xinqi.utils.bt.spec.odm.BluetoothProtocol.CompanyId.XIN_QI -> "新琪"
            else -> "未知厂商"
        }
    
    val protocolVersion: String
        get() = "V${broadcastData?.protocolVersion ?: "?"}"
    
    val uid: String
        get() = broadcastData?.uid?.joinToString(":") { "0x${it.toString(16).uppercase()}" } ?: "未知"
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as BluetoothDeviceInfo
        return device.address == other.device.address
    }
    
    override fun hashCode(): Int {
        return device.address.hashCode()
    }
}
