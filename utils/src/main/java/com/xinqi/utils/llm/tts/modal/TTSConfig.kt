package com.xinqi.utils.llm.tts.modal

/**
 * TTS配置类
 */
data class TTSConfig(
    val provider: TTSProviderType,
    val baseUrl: String,
    val apiKey: String, // 保留兼容性，但建议使用ak和sk
    val ak: String = "", // Access Key ID
    val sk: String = "", // Access Key Secret
    val model: TTSModel,
    val timeout: Long = 30000,
    val voice: String,
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
    RIRIXIN // 商汤日日新
}

/**
 * TTS模型信息
 */
data class TTSModel(
    val modelId: String,
    val name: String,
    val description: String? = null,
    val maxTextLength: Int = 1000,
    val supportedLanguages: List<String> = listOf("zh-CN", "en-US")
)
