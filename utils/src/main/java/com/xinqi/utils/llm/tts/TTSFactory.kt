package com.xinqi.utils.llm.tts

import android.content.Context
import com.xinqi.utils.common.ConfigManager
import com.xinqi.utils.llm.tts.provider.RirixinTTSProvider

/**
 * TTS工厂类
 * 根据配置创建相应的TTS提供商
 */
object TTSFactory {
    
    /**
     * 创建TTS提供商
     */
    fun createProvider(context: Context, config: TTSConfig): TTSProvider? {
        return when (config.provider) {
            TTSProviderType.RIRIXIN -> RirixinTTSProvider(context, config)
            TTSProviderType.AZURE -> {
                // TODO: 实现Azure TTS提供商
                null
            }
            TTSProviderType.BAIDU -> {
                // TODO: 实现百度TTS提供商
                null
            }
            TTSProviderType.XUNFEI -> {
                // TODO: 实现讯飞TTS提供商
                null
            }
            TTSProviderType.ALIYUN -> {
                // TODO: 实现阿里云TTS提供商
                null
            }
            TTSProviderType.GOOGLE -> {
                // TODO: 实现Google TTS提供商
                null
            }
        }
    }
    
    /**
     * 创建日日新TTS配置（使用AK和SK）
     * 推荐使用此方法，符合日日新官方鉴权要求
     */
    fun createRirixinConfig(
        context: Context,
        ak: String? = null,
        sk: String? = null,
        baseUrl: String = "https://api.sensenova.cn/v1/audio/speech"
    ): TTSConfig {
        // 确保ConfigManager已初始化
        ConfigManager.init(context)
        
        val finalAk = ak ?: ConfigManager.getTTSRirixinAk()
        val finalSk = sk ?: ConfigManager.getTTSRirixinSk()
        
        return TTSConfig(
            provider = TTSProviderType.RIRIXIN,
            baseUrl = baseUrl,
            apiKey = "",
            ak = finalAk,
            sk = finalSk,
            model = TTSModel(
                modelId = "nova-tts-1", // SenseNova-Audio-Fusion-0603
                name = "日日新TTS模型",
                description = "日日新提供的文本转语音服务",
                maxTextLength = 2000,
                supportedLanguages = listOf("zh-CN", "en-US", "ja-JP")
            ),
            voice = "cheerfulvoice-general-male-cn",
            defaultSpeed = 1.0f,
            defaultPitch = 1.0f,
            defaultVolume = 1.0f,
            outputFormat = "wav",
            sampleRate = 16000
        )
    }
    
    /**
     * 创建日日新TTS配置（兼容旧版本，使用apiKey）
     * 注意：此方法不推荐使用，建议使用AK和SK
     */
    @Deprecated("use ak and sk instead of static api key")
    fun createRirixinConfigLegacy(
        apiKey: String,
        baseUrl: String = "https://api.sensenova.cn/v1/audio/speech"
    ): TTSConfig {
        return TTSConfig(
            provider = TTSProviderType.RIRIXIN,
            baseUrl = baseUrl,
            apiKey = apiKey,
            ak = "",
            sk = "",
            model = TTSModel(
                modelId = "nova-tts-1",
                name = "日日新TTS模型",
                description = "日日新提供的文本转语音服务",
                maxTextLength = 2000,
                supportedLanguages = listOf("zh-CN", "en-US", "ja-JP")
            ),
            voice = "cheerfulvoice-general-male-cn",
            defaultSpeed = 1.0f,
            defaultPitch = 1.0f,
            defaultVolume = 1.0f,
            outputFormat = "wav",
            sampleRate = 16000
        )
    }
    
    /**
     * 创建Azure TTS配置
     */
    fun createAzureConfig(
        apiKey: String,
        region: String,
        baseUrl: String = "https://${region}.tts.speech.microsoft.com"
    ): TTSConfig {
        return TTSConfig(
            provider = TTSProviderType.AZURE,
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = TTSModel(
                modelId = "azure-tts-v1",
                name = "Azure TTS模型",
                description = "微软Azure提供的文本转语音服务",
                maxTextLength = 3000,
                supportedLanguages = listOf("zh-CN", "en-US", "ja-JP", "ko-KR")
            ),
            voice = "",
            defaultSpeed = 1.0f,
            defaultPitch = 1.0f,
            defaultVolume = 1.0f,
            outputFormat = "wav",
            sampleRate = 16000
        )
    }
}
