package com.xinqi.utils.llm.tts.model

/**
 * TTS配置类
 */
data class TTSConfig(
    val provider: TTSProviderType,
    val baseUrl: String,
    val apiKey: String, // 保留兼容性，但建议使用ak和sk
    val ak: String = "", // Access Key ID
    val sk: String = "", // Access Key Secret
    val groupId: String = "", // Group ID (用于Minimax等)
    val model: TTSModel,
    val timeout: Long = 30000,
    var voice: String,
    val defaultSpeed: Float = 1.0f,
    val defaultPitch: Float = 1.0f,
    val defaultVolume: Float = 1.0f,
    val outputFormat: String = "wav",
    val sampleRate: Int = 16000
)

/**
 * TTS提供商类型
 */
enum class TTSProviderType {
    AZURE,
    BAIDU,
    XUNFEI,
    ALIYUN,
    GOOGLE,
    RIRIXIN, // 商汤日日新
    MINIMAX // Minimax
}

/**
 * TTS模型信息
 */
data class TTSModel(
    val provider: TTSProviderType,
    val modelId: String,
    val name: String,
    val description: String? = null,
    val maxTextLength: Int = 1000,
    val supportedLanguages: List<String> = listOf("zh-CN", "en-US"),
    val availableVoices: List<VoiceInfo> = emptyList() // 该模型支持的音色列表
)

/**
 * 语音信息
 */
data class VoiceInfo(
    val voiceId: String,
    val name: String,
    val language: String,
    val gender: String,
    val description: String? = null,
    val modelId: String = "" // 关联的模型ID
)

/**
 * TTS提供商信息
 */
data class TTSProviderInfo(
    val provider: TTSProviderType,
    val name: String,
    val description: String? = null,
    val baseUrl: String,
    val supportedModels: List<TTSModel> = emptyList(),
    val defaultModel: TTSModel? = null
)
