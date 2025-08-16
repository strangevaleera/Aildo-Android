package com.xinqi.ui.character

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xinqi.utils.log.logI
import com.xinqi.utils.log.showResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 人物交互界面的ViewModel
 * 管理界面状态和蓝牙通信
 */
class CharacterInteractionViewModel : ViewModel() {
    
    // 当前选中的角色
    private val _currentCharacter = MutableStateFlow("fig1")
    val currentCharacter: StateFlow<String> = _currentCharacter.asStateFlow()
    
    // 当前播放的动画类型
    private val _currentAnimation = MutableStateFlow("chat")
    val currentAnimation: StateFlow<String> = _currentAnimation.asStateFlow()
    
    // 是否显示人物选择器
    private val _showCharacterSelector = MutableStateFlow(false)
    val showCharacterSelector: StateFlow<Boolean> = _showCharacterSelector.asStateFlow()
    
    // 蓝牙连接状态
    private val _bluetoothConnected = MutableStateFlow(false)
    val bluetoothConnected: StateFlow<Boolean> = _bluetoothConnected.asStateFlow()
    
    /**
     * 选择角色
     */
    fun selectCharacter(character: String) {
        _currentCharacter.value = character
        // 这里可以添加角色切换的逻辑
    }
    
    /**
     * 切换人物选择器显示状态
     */
    fun toggleCharacterSelector() {
        _showCharacterSelector.value = !_showCharacterSelector.value
    }
    
    /**
     * 隐藏人物选择器
     */
    fun hideCharacterSelector() {
        _showCharacterSelector.value = false
    }
    
    /**
     * 播放指定动画
     */
    fun playAnimation(animationType: String) {
        _currentAnimation.value = animationType
        // 这里可以添加动画播放的逻辑
    }
    
    /**
     * 处理身体部位点击事件
     */
    fun onBodyPartClick(context: Context, bodyPart: String, x: Float, y: Float) {
        viewModelScope.launch {
            // 发送蓝牙指令
            sendBluetoothCommand(context, bodyPart, x, y)
        }
    }
    
    /**
     * 发送蓝牙指令
     */
    private fun sendBluetoothCommand(context: Context, bodyPart: String, x: Float, y: Float) {
        //根据点击的身体部位和位置生成蓝牙指令
        val command = when (bodyPart) {
            "head" -> generateHeadCommand(x, y)
            "body" -> generateBodyCommand(x, y)
            "legs" -> generateLegsCommand(x, y)
            else -> null
        }
        
        command?.let { cmd ->
            // 调用蓝牙管理器发送指令
            // BluetoothManager.sendCommand(cmd)
            showResult( context,"发送蓝牙指令: $cmd")
        }
    }
    
    /**
     * 生成头部指令
     */
    private fun generateHeadCommand(x: Float, y: Float): String? {
        return when {
            x < 0.3f -> "HEAD_LEFT"
            x > 0.7f -> "HEAD_RIGHT"
            else -> "HEAD_CENTER"
        }
    }
    
    /**
     * 生成身体指令
     */
    private fun generateBodyCommand(x: Float, y: Float): String? {
        return when {
            x < 0.3f -> "BODY_LEFT"
            x > 0.7f -> "BODY_RIGHT"
            else -> "BODY_CENTER"
        }
    }
    
    /**
     * 生成腿部指令
     */
    private fun generateLegsCommand(x: Float, y: Float): String? {
        return when {
            x < 0.3f -> "LEGS_LEFT"
            x > 0.7f -> "LEGS_RIGHT"
            else -> "LEGS_CENTER"
        }
    }
    
    /**
     * 设置蓝牙连接状态
     */
    fun setBluetoothConnected(connected: Boolean) {
        _bluetoothConnected.value = connected
    }
    
    /**
     * 发送自定义控制指令
     */
    fun sendCustomCommand(command: String) {
        viewModelScope.launch {
            // 这里调用蓝牙管理器发送自定义指令
            // BluetoothManager.sendCommand(command)
            logI("发送自定义指令: $command")
        }
    }
}