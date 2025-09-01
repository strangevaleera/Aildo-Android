package com.xinqi.utils.llm.tts.config

import com.xinqi.utils.llm.tts.model.TTSProviderType
import com.xinqi.utils.llm.tts.model.TTSModel
import com.xinqi.utils.log.logI

/**
 * TTS模型配置
 * 集中管理所有TTS模型信息，避免重复定义
 */
object ModelConfig {

    //ririxin
    const val MODEL_ID_RIRIXIN_NOVA = "nova-tts-1"
    const val MODEL_ID_RIRIXIN_SENSE_NOVA = "SenseNova-Audio-Fusion-0603"
    //minimax
    const val MODEL_ID_MINIMAX_SPEECH = "speech-02-hd"

    /**
     * 日日新TTS模型配置
     */
    val RIRIXIN_MODELS = listOf(
        TTSModel(
            provider = TTSProviderType.RIRIXIN,
            modelId = MODEL_ID_RIRIXIN_NOVA,
            name = "日日新TTS模型",
            description = "日日新提供的文本转语音服务",
            maxTextLength = 2000,
            supportedLanguages = listOf("zh-CN", "en-US", "ja-JP"),
            availableVoices = VoiceConfig.getVoicesForModel(MODEL_ID_RIRIXIN_NOVA)
        ),
        TTSModel(
            provider = TTSProviderType.RIRIXIN,
            modelId = MODEL_ID_RIRIXIN_SENSE_NOVA,
            name = "日日新-语音大模型-语音合成（音色融合）",
            description = "日日新提供的文本转语音服务",
            maxTextLength = 2000,
            supportedLanguages = listOf("zh-CN", "en-US", "ja-JP"),
            availableVoices = VoiceConfig.getVoicesForModel(MODEL_ID_RIRIXIN_SENSE_NOVA)
        )
    )
    
    /**
     * Minimax TTS模型配置
     */
    val MINIMAX_MODELS = listOf(
        TTSModel(
            provider = TTSProviderType.MINIMAX,
            modelId = MODEL_ID_MINIMAX_SPEECH,
            name = "Minimax TTS模型",
            description = "Minimax提供的文本转语音服务",
            maxTextLength = 1000,
            supportedLanguages = listOf("zh-CN"),
            availableVoices = VoiceConfig.getVoicesForModel(MODEL_ID_MINIMAX_SPEECH)
        )
    )
    
    /**
     * Azure TTS模型配置
     */
    val AZURE_MODELS = listOf(
        TTSModel(
            provider = TTSProviderType.AZURE,
            modelId = "azure-tts-v1",
            name = "Azure TTS模型",
            description = "微软Azure提供的文本转语音服务",
            maxTextLength = 3000,
            supportedLanguages = listOf("zh-CN", "en-US", "ja-JP", "ko-KR"),
            availableVoices = VoiceConfig.getVoicesForModel("azure-tts-v1")
        )
    )
    
    /**
     * 获取所有支持的TTS模型
     */
    fun getAllModels(): List<TTSModel> {
        return RIRIXIN_MODELS + MINIMAX_MODELS// + AZURE_MODELS
    }
    
    /**
     * 根据提供商类型获取模型列表
     */
    fun getModelsByProvider(provider: TTSProviderType): List<TTSModel> {
        return when (provider) {
            TTSProviderType.RIRIXIN -> RIRIXIN_MODELS
            TTSProviderType.MINIMAX -> MINIMAX_MODELS
            TTSProviderType.AZURE -> AZURE_MODELS
            else -> emptyList()
        }
    }
    
    /**
     * 根据模型ID获取模型信息
     */
    fun getModelById(modelId: String): TTSModel? {
        logI("获取模型，id: $modelId")
        return getAllModels().find { it.modelId == modelId }
    }
    
    /**
     * 获取默认模型
     */
    fun getDefaultModel(): TTSModel {
        return RIRIXIN_MODELS.first()
    }
    
    /**
     * 获取指定提供商的默认模型
     */
    fun getDefaultModelByProvider(provider: TTSProviderType): TTSModel? {
        return getModelsByProvider(provider).firstOrNull()
    }
}
