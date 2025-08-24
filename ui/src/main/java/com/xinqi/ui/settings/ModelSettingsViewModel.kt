package com.xinqi.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xinqi.utils.llm.LLMManager
import com.xinqi.utils.llm.modal.LLMModel
import com.xinqi.utils.llm.tts.modal.TTSConfig
import com.xinqi.utils.llm.tts.VoiceInfo
import com.xinqi.utils.llm.tts.modal.TTSFactory
import com.xinqi.utils.llm.tts.modal.TTSModel
import com.xinqi.utils.llm.tts.modal.TTSProviderType
import com.xinqi.utils.log.logI
import com.xinqi.utils.log.showResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 模型设置页面的ViewModel
 * 管理LLM模型、TTS模型和音色的选择状态
 */
class ModelSettingsViewModel : ViewModel() {

    // 当前选择的LLM模型
    private val _currentLLMModel = MutableStateFlow(LLMModel.DOUBAO)
    val currentLLMModel: StateFlow<LLMModel> = _currentLLMModel.asStateFlow()

    // 当前选择的TTS模型
    private val _currentTTSModel = MutableStateFlow(
        TTSFactory.getDefaultTTSModel()
    )
    val currentTTSModel: StateFlow<TTSModel> = _currentTTSModel.asStateFlow()

    // 当前选择的TTS音色
    private val _currentVoice = MutableStateFlow("cheerfulvoice-general-male-cn")
    val currentVoice: StateFlow<String> = _currentVoice.asStateFlow()

    // 可用的音色列表
    private val _availableVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<VoiceInfo>> = _availableVoices.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 临时存储的设置，用于应用时统一更新
    private var pendingLLMModel: LLMModel? = null
    private var pendingTTSModel: TTSModel? = null
    private var pendingVoice: String? = null

    /**
     * 加载当前设置
     */
    fun loadCurrentSettings(llmManager: LLMManager) {
        viewModelScope.launch {
            try {
                // 获取当前LLM模型
                val currentModel = llmManager.getCurrentModel()
                _currentLLMModel.value = currentModel

                // 获取当前TTS配置（如果有的话）
                // 注意：LLMManager可能没有直接提供TTS配置的getter方法
                // 这里我们使用默认值，实际应用中可能需要从其他地方获取
                logI("加载当前设置完成，LLM模型: ${currentModel.displayName}")
            } catch (e: Exception) {
                logI("加载当前设置失败: ${e.message}")
            }
        }
    }

    /**
     * 加载可用的音色列表
     */
    fun loadAvailableVoices(llmManager: LLMManager) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val voices = llmManager.getTTSManager().getAvailableVoices()
                _availableVoices.value = voices
                logI("加载音色列表完成，共${voices.size}个音色")
            } catch (e: Exception) {
                logI("加载音色列表失败: ${e.message}")
                // 如果加载失败，提供一些默认音色
                _availableVoices.value = getDefaultVoices()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 更新LLM模型选择
     */
    fun updateLLMModel(llmManager: LLMManager, model: LLMModel) {
        pendingLLMModel = model
        _currentLLMModel.value = model
        logI("选择LLM模型: ${model.displayName}")
    }

    /**
     * 更新TTS模型选择
     */
    fun updateTTSModel(llmManager: LLMManager, model: TTSModel) {
        pendingTTSModel = model
        _currentTTSModel.value = model
        logI("选择TTS模型: ${model.name}")
    }

    /**
     * 更新TTS音色选择
     */
    fun updateTTSVoice(llmManager: LLMManager, voice: String) {
        pendingVoice = voice
        _currentVoice.value = voice
        logI("选择TTS音色: $voice")
    }

    /**
     * 应用所有设置
     */
    fun applySettings(context: Context, llmManager: LLMManager) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // 应用LLM模型设置
                pendingLLMModel?.let { model ->
                    if (model != llmManager.getCurrentModel()) {
                        llmManager.switchModel(model)
                        logI("已切换到LLM模型: ${model.displayName}")
                    }
                }

                // 应用TTS设置
                pendingTTSModel?.let { ttsModel ->
                    pendingVoice?.let { voice ->
                        // 创建新的TTS配置
                        val newTTSConfig = TTSConfig(
                            provider = TTSProviderType.RIRIXIN, // 默认使用日日新
                            baseUrl = "https://api.sensenova.cn/v1/audio/speech",
                            apiKey = "", // 这里需要从配置中获取
                            ak = "", // 这里需要从配置中获取
                            sk = "", // 这里需要从配置中获取
                            model = ttsModel,
                            voice = voice
                        )

                        // 重新初始化TTS提供商
                        llmManager.getTTSManager().initializeProvider(newTTSConfig)
                        logI("已更新TTS配置: ${ttsModel.name}, 音色: $voice")
                    }
                }

                // 清空待处理的设置
                pendingLLMModel = null
                pendingTTSModel = null
                pendingVoice = null

                showResult( context,"所有设置已应用完成")
            } catch (e: Exception) {
                logI("应用设置失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 获取默认音色列表（当API调用失败时的备用方案）
     */
    private fun getDefaultVoices(): List<VoiceInfo> {
        return listOf(
            VoiceInfo(
                voiceId = "cheerfulvoice-general-male-cn",
                name = "欢快男声-中文",
                language = "zh-CN",
                gender = "male",
                description = "适合日常对话的欢快男声"
            ),
            VoiceInfo(
                voiceId = "cheerfulvoice-general-female-cn",
                name = "欢快女声-中文",
                language = "zh-CN",
                gender = "female",
                description = "适合日常对话的欢快女声"
            ),
            VoiceInfo(
                voiceId = "gentlevoice-general-male-cn",
                name = "温柔男声-中文",
                language = "zh-CN",
                gender = "male",
                description = "适合温柔对话的男声"
            ),
            VoiceInfo(
                voiceId = "gentlevoice-general-female-cn",
                name = "温柔女声-中文",
                language = "zh-CN",
                gender = "female",
                description = "适合温柔对话的女声"
            )
        )
    }

    /**
     * 检查是否有待应用的设置
     */
    fun hasPendingChanges(): Boolean {
        return pendingLLMModel != null || pendingTTSModel != null || pendingVoice != null
    }

    /**
     * 重置所有设置到默认值
     */
    fun resetToDefaults() {
        pendingLLMModel = LLMModel.DOUBAO
        pendingTTSModel = TTSModel(
            modelId = "nova-tts-1",
            name = "日日新TTS模型",
            description = "日日新提供的文本转语音服务",
            maxTextLength = 2000,
            supportedLanguages = listOf("zh-CN", "en-US", "ja-JP")
        )
        pendingVoice = "cheerfulvoice-general-male-cn"

        _currentLLMModel.value = pendingLLMModel!!
        _currentTTSModel.value = pendingTTSModel!!
        _currentVoice.value = pendingVoice!!

        logI("已重置设置到默认值")
    }
}
