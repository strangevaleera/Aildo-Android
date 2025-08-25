package com.xinqi.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xinqi.utils.llm.LLMManager
import com.xinqi.utils.llm.model.LLMModel
import com.xinqi.utils.llm.tts.model.VoiceInfo
import com.xinqi.utils.llm.tts.config.ModelConfig
import com.xinqi.utils.llm.tts.config.VoiceConfig
import com.xinqi.utils.llm.tts.model.TTSFactory
import com.xinqi.utils.llm.tts.model.TTSModel
import com.xinqi.utils.llm.tts.model.TTSProviderType
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
                val currentModel = llmManager.getCurrentModel()
                _currentLLMModel.value = currentModel
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
     * 选择模型后自动更新对应的音色列表
     */
    fun updateTTSModel(llmManager: LLMManager, model: TTSModel) {
        pendingTTSModel = model
        _currentTTSModel.value = model
        logI("选择TTS模型: ${model.name}")
        
        // 自动更新音色列表
        updateAvailableVoicesForModel(model.modelId)
        
        // 自动选择默认音色（如果当前音色不在新模型中可用）
        updateDefaultVoiceForModel(model.modelId)
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
     * 根据模型ID更新可用的音色列表
     */
    private fun updateAvailableVoicesForModel(modelId: String) {
        viewModelScope.launch {
            try {
                val voices = VoiceConfig.getVoicesForModel(modelId)
                _availableVoices.value = voices
                logI("已更新音色列表，模型: $modelId, 音色数量: ${voices.size}")
            } catch (e: Exception) {
                logI("更新音色列表失败: ${e.message}")
                // 如果更新失败，使用默认音色
                _availableVoices.value = getDefaultVoices()
            }
        }
    }

    /**
     * 为新选择的模型自动选择默认音色
     */
    private fun updateDefaultVoiceForModel(modelId: String) {
        viewModelScope.launch {
            try {
                val voices = VoiceConfig.getVoicesForModel(modelId)
                if (voices.isNotEmpty()) {
                    // 检查当前音色是否在新模型中可用
                    val currentVoiceId = _currentVoice.value
                    val isCurrentVoiceAvailable = voices.any { it.voiceId == currentVoiceId }
                    
                    if (!isCurrentVoiceAvailable) {
                        // 如果当前音色不可用，选择第一个可用的音色
                        val defaultVoice = voices.first()
                        _currentVoice.value = defaultVoice.voiceId
                        pendingVoice = defaultVoice.voiceId
                        logI("已自动选择默认音色: ${defaultVoice.name} (${defaultVoice.voiceId})")
                    }
                }
            } catch (e: Exception) {
                logI("更新默认音色失败: ${e.message}")
            }
        }
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
                    pendingVoice?.let { selectVoice ->
                        // 创建新的TTS配置
                        val newTTSConfig = TTSFactory.getTTSConfigByModel(context, ttsModel).apply {
                            voice = selectVoice
                        }

                        // 重新初始化TTS提供商
                        llmManager.getTTSManager().initializeProvider(newTTSConfig)
                        logI("已更新TTS配置: ${ttsModel.name}, 音色: $selectVoice")
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
     * 获取默认音色列表
     *
     * 垃圾代码
     */
    @Deprecated("垃圾代码")
    private fun getDefaultVoices(): List<VoiceInfo> {
        return listOf(
            VoiceInfo(
                voiceId = "cheerfulvoice-general-male-cn",
                name = "欢快男声-中文",
                language = "zh-CN",
                gender = "male",
                description = "适合日常对话的欢快男声",
                modelId = ModelConfig.MODEL_ID_RIRIXIN_NOVA
            ),
            VoiceInfo(
                voiceId = "cheerfulvoice-general-female-cn",
                name = "欢快女声-中文",
                language = "zh-CN",
                gender = "female",
                description = "适合日常对话的欢快女声",
                modelId = ModelConfig.MODEL_ID_RIRIXIN_NOVA
            ),
            VoiceInfo(
                voiceId = "gentlevoice-general-male-cn",
                name = "温柔男声-中文",
                language = "zh-CN",
                gender = "male",
                description = "适合温柔对话的男声",
                modelId = ModelConfig.MODEL_ID_RIRIXIN_NOVA
            ),
            VoiceInfo(
                voiceId = "gentlevoice-general-female-cn",
                name = "温柔女声-中文",
                language = "zh-CN",
                gender = "female",
                description = "适合温柔对话的女声",
                modelId = ModelConfig.MODEL_ID_RIRIXIN_NOVA
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
            provider = TTSProviderType.RIRIXIN,
            modelId = ModelConfig.MODEL_ID_RIRIXIN_NOVA,
            name = "日日新TTS模型",
            description = "日日新提供的文本转语音服务",
            maxTextLength = 2000,
            supportedLanguages = listOf("zh-CN", "en-US", "ja-JP"),
            availableVoices = VoiceConfig.getVoicesForModel(ModelConfig.MODEL_ID_RIRIXIN_NOVA)
        )
        pendingVoice = "cheerfulvoice-general-male-cn"

        _currentLLMModel.value = pendingLLMModel!!
        _currentTTSModel.value = pendingTTSModel!!
        _currentVoice.value = pendingVoice!!

        // 更新音色列表
        updateAvailableVoicesForModel(ModelConfig.MODEL_ID_RIRIXIN_NOVA)

        logI("已重置设置到默认值")
    }
}
