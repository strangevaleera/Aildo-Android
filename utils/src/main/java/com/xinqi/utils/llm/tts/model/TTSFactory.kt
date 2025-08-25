package com.xinqi.utils.llm.tts.model

import android.content.Context
import com.xinqi.utils.common.ConfigManager
import com.xinqi.utils.llm.tts.TTSProvider
import com.xinqi.utils.llm.tts.provider.RirixinTTSProvider
import com.xinqi.utils.llm.tts.provider.MinimaxProvider
import com.xinqi.utils.llm.tts.config.ModelConfig
import com.xinqi.utils.llm.tts.config.VoiceConfig

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
            TTSProviderType.MINIMAX -> MinimaxProvider(context, config)
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
     * 创建Minimax TTS配置
     */
    fun createMinimaxConfig(
        context: Context,
        baseUrl: String = "https://api.minimax.chat/v1"
    ): TTSConfig {
        // 确保ConfigManager已初始化
        ConfigManager.init(context)

        val finalApiKey = ConfigManager.getTTSMinimaxApiKey()
        val finalGroupId = ConfigManager.getTTSMinimaxGroupId()
        
        // 从配置文件获取Minimax模型
        val minimaxModel = ModelConfig.getModelById(ModelConfig.MODEL_ID_MINIMAX_SPEECH)
            ?: throw IllegalStateException("Minimax模型配置不存在")
        
        return TTSConfig(
            provider = TTSProviderType.MINIMAX,
            baseUrl = baseUrl,
            apiKey = finalApiKey,
            ak = "",
            sk = "",
            groupId = finalGroupId,
            model = minimaxModel,
            voice = minimaxModel.availableVoices.firstOrNull()?.voiceId ?: "male-qn-qingse",
            defaultSpeed = 1.0f,
            defaultPitch = 1.0f,
            defaultVolume = 1.0f,
            outputFormat = "wav",
            sampleRate = 16000
        )
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
        
        // 从配置文件获取日日新模型
        val ririxinModel = ModelConfig.getModelById(ModelConfig.MODEL_ID_RIRIXIN_NOVA)
            ?: throw IllegalStateException("日日新模型配置不存在")
        
        return TTSConfig(
            provider = TTSProviderType.RIRIXIN,
            baseUrl = baseUrl,
            apiKey = "",
            ak = finalAk,
            sk = finalSk,
            model = ririxinModel,
            voice = ririxinModel.availableVoices.firstOrNull()?.voiceId ?: "cheerfulvoice-general-male-cn",
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
        // 从配置文件获取日日新模型
        val ririxinModel = ModelConfig.getModelById(ModelConfig.MODEL_ID_RIRIXIN_NOVA)
            ?: throw IllegalStateException("日日新模型配置不存在")
        
        return TTSConfig(
            provider = TTSProviderType.RIRIXIN,
            baseUrl = baseUrl,
            apiKey = apiKey,
            ak = "",
            sk = "",
            model = ririxinModel,
            voice = ririxinModel.availableVoices.firstOrNull()?.voiceId ?: "cheerfulvoice-general-male-cn",
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
        // 从配置文件获取Azure模型
        val azureModel = ModelConfig.getModelById("azure-tts-v1")
            ?: throw IllegalStateException("Azure模型配置不存在")
        
        return TTSConfig(
            provider = TTSProviderType.AZURE,
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = azureModel,
            voice = azureModel.availableVoices.firstOrNull()?.voiceId ?: "zh-CN-XiaoxiaoNeural",
            defaultSpeed = 1.0f,
            defaultPitch = 1.0f,
            defaultVolume = 1.0f,
            outputFormat = "wav",
            sampleRate = 16000
        )
    }

    fun getSupportTTSModels(): List<TTSModel> {
        // 从配置文件获取所有支持的TTS模型
        return ModelConfig.getAllModels()
    }

    fun getDefaultTTSModel(): TTSModel {
        // 从配置文件获取默认模型
        return ModelConfig.getDefaultModel()
    }

    /**
     * 根据TTS模型创建对应的TTS配置
     */
    fun getTTSConfigByModel(context: Context, ttsModel: TTSModel): TTSConfig {
        return when (ttsModel.provider) {
            TTSProviderType.RIRIXIN -> createRirixinConfig(context)
            TTSProviderType.MINIMAX -> createMinimaxConfig(context)
            TTSProviderType.AZURE -> {
                // 这里需要传入实际的API Key和region
                createAzureConfig("your_azure_api_key", "eastasia")
            }
            else -> createRirixinConfig(context) // 默认使用日日新
        }
    }
    
    /**
     * 根据模型ID获取模型信息
     */
    fun getTTSModelById(modelId: String): TTSModel? {
        return ModelConfig.getModelById(modelId)
    }
    
    /**
     * 根据提供商类型获取所有支持的模型
     */
    fun getTTSModelsByProvider(provider: TTSProviderType): List<TTSModel> {
        return ModelConfig.getModelsByProvider(provider)
    }
    
    /**
     * 根据模型获取支持的音色列表
     */
    fun getAvailableVoicesByModel(modelId: String): List<VoiceInfo> {
        return VoiceConfig.getVoicesForModel(modelId)
    }
    
    /**
     * 根据模型更新TTS配置中的音色选择
     */
    fun updateTTSConfigVoice(config: TTSConfig, modelId: String): TTSConfig {
        val voices = getAvailableVoicesByModel(modelId)
        if (voices.isNotEmpty()) {
            // 如果当前选择的音色不在新模型的音色列表中，则选择第一个可用的音色
            val currentVoice = config.voice
            val isValidVoice = voices.any { it.voiceId == currentVoice }
            
            if (!isValidVoice) {
                config.voice = voices.first().voiceId
            }
        }
        return config
    }
}
