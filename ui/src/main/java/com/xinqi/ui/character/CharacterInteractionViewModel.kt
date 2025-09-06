package com.xinqi.ui.character

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xinqi.utils.bt.AildoBluetoothManager
import com.xinqi.utils.log.logI
import com.xinqi.utils.log.showResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    private val _showCharacterSelector = MutableStateFlow(false)
    val showCharacterSelector: StateFlow<Boolean> = _showCharacterSelector.asStateFlow()

    // 蓝牙连接状态
    private val _bluetoothConnected = MutableStateFlow(false)
    val bluetoothConnected: StateFlow<Boolean> = _bluetoothConnected.asStateFlow()
    
    // 蓝牙管理器
    private var bluetoothManager: AildoBluetoothManager? = null
    
    init {
        // 启动蓝牙状态监听
        startBluetoothStatusMonitoring()
    }
    
    private fun startBluetoothStatusMonitoring() {
        viewModelScope.launch {
            while (true) {
                try {
                    bluetoothManager?.let { manager ->
                        val isConnected = manager.isConnected()
                        _bluetoothConnected.value = isConnected
                        logI("蓝牙连接状态更新: $isConnected")
                    }
                } catch (e: Exception) {
                    logI("检查蓝牙连接状态时出错: ${e.message}")
                }
                delay(2000) // 每2秒检查一次
            }
        }
    }
    
    fun initializeBluetoothManager(context: Context) {
        bluetoothManager = AildoBluetoothManager.getInstance(context)
        logI("蓝牙管理器已初始化")
    }

    /**
     * 选择角色
     */
    fun selectCharacter(character: String) {
        _currentCharacter.value = character
        // 这里可以添加角色切换的逻辑
    }

    fun toggleCharacterSelector() {
        _showCharacterSelector.value = !_showCharacterSelector.value
    }

    fun hideCharacterSelector() {
        _showCharacterSelector.value = false
    }

    /**
     * 播放指定的动画效果
     */
    fun playAnimation(animationType: String) {
        logI("play animation: $animationType")
        _currentAnimation.value = animationType
    }

    /**
     * 处理身体部位点击事件
     */
    fun onBodyPartClick(context: Context,
                        character: String,
                        bodyPart: String, x: Float, y: Float) {
        viewModelScope.launch {
            val animationTrigger = CharacterModel.getAnimationTrigger(character, bodyPart, CharacterModel.ClickType.SINGLE_CLICK)
            
            if (animationTrigger != null) {
                // 播放对应动画
                playAnimation(animationTrigger)
                // 获取点击动作配置
                val clickAction = CharacterModel.getClickAction(character, bodyPart, CharacterModel.ClickType.SINGLE_CLICK)
                // 显示角色回复，目前为toast
                clickAction?.response?.let { response ->
                    showResult(context, response)
                }
                // 发送蓝牙指令
                clickAction?.bluetoothCommand?.let { command ->
                    sendBluetoothCommand(context, command)
                }
            } else {
                showResult(context, "别碰了")
            }
        }
    }

    fun onBodyPartLongPress(context: Context,
                            character: String,
                            bodyPart: String, x: Float, y: Float) {
        viewModelScope.launch {
            val animationTrigger = CharacterModel.getAnimationTrigger(character, bodyPart, CharacterModel.ClickType.LONG_PRESS)
            
            if (animationTrigger != null) {
                playAnimation(animationTrigger)
                // 获取点击动作配置
                val clickAction = CharacterModel.getClickAction(character, bodyPart, CharacterModel.ClickType.LONG_PRESS)
                // 显示角色回复
                clickAction?.response?.let { response ->
                    showResult(context, response)
                }
            }
        }
    }

    fun onContinuousClick(context: Context,
                          character: String,
                          count: Int, bodyPart: String, x: Float, y: Float) {
        viewModelScope.launch {
            if (count == Constants.RAPID_CLICK_THRESHOLD) {
                val animationTrigger = CharacterModel.getAnimationTrigger(character, bodyPart, CharacterModel.ClickType.RAPID_CLICK)
                
                if (animationTrigger != null) {
                    playAnimation(animationTrigger)
                    val clickAction = CharacterModel.getClickAction(character, bodyPart, CharacterModel.ClickType.RAPID_CLICK)
                    clickAction?.response?.let { response ->
                        showResult(context, response)
                    }
                }
            }
        }
    }

    /**
     * 发送蓝牙指令
     */
    private fun sendBluetoothCommand(context: Context, command: String?) {
        command?.let { cmd ->
            // 调用蓝牙管理器发送指令
            // BluetoothManager.sendCommand(cmd)
            logI( "发送蓝牙指令: $cmd")
        }
    }


    fun setBluetoothConnected(connected: Boolean) {
        _bluetoothConnected.value = connected
    }

    fun sendCustomCommand(command: String) {
        viewModelScope.launch {
            // 这里调用蓝牙管理器发送自定义指令
            // BluetoothManager.sendCommand(command)
            logI("发送自定义指令: $command")
        }
    }
}