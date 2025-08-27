package com.xinqi.utils.llm.tts.config

import com.xinqi.utils.llm.tts.model.TTSProviderType
import com.xinqi.utils.llm.tts.model.VoiceInfo

/**
 * TTS音色配置
 * 集中管理所有TTS模型的音色信息
 */
object VoiceConfig {
    
    /**
     * 日日新TTS音色配置
     */
    val RIRIXIN_VOICES = mapOf(
        ModelConfig.MODEL_ID_RIRIXIN_NOVA to listOf(
            VoiceInfo("cheerfulvoice-general-male-cn", "欢快男声", "zh-CN", "male", "欢快的男声，适合活泼场景",  ModelConfig.MODEL_ID_RIRIXIN_NOVA),
            VoiceInfo("cheerfulvoice-general-female-cn", "欢快女声", "zh-CN", "female", "欢快的女声，适合活泼场景",  ModelConfig.MODEL_ID_RIRIXIN_NOVA),
            VoiceInfo("gentlevoice-general-male-cn", "温柔男声", "zh-CN", "male", "温柔的男声，适合温馨场景",  ModelConfig.MODEL_ID_RIRIXIN_NOVA),
            VoiceInfo("gentlevoice-general-female-cn", "温柔女声", "zh-CN", "female", "温柔的女声，适合温馨场景",  ModelConfig.MODEL_ID_RIRIXIN_NOVA),
            VoiceInfo("seriousvoice-general-male-cn", "严肃男声", "zh-CN", "male", "严肃的男声，适合正式场景",  ModelConfig.MODEL_ID_RIRIXIN_NOVA),
            VoiceInfo("seriousvoice-general-female-cn", "严肃女声", "zh-CN", "female", "严肃的女声，适合正式场景",  ModelConfig.MODEL_ID_RIRIXIN_NOVA)
        ),
        ModelConfig.MODEL_ID_RIRIXIN_SENSE_NOVA to listOf(
            VoiceInfo("guy_nangong", "挚爱男攻", "zh-CN", "male", "挚爱男攻", ModelConfig.MODEL_ID_RIRIXIN_SENSE_NOVA),
            VoiceInfo("man_weiyan", "威严霸总", "zh-CN", "male", "威严霸总", ModelConfig.MODEL_ID_RIRIXIN_SENSE_NOVA),
            VoiceInfo("guy_qingshuang", "清爽帅哥", "zh-CN", "male", "清爽帅哥", ModelConfig.MODEL_ID_RIRIXIN_SENSE_NOVA),
            VoiceInfo("male_shenqing", "深情男友", "zh-CN", "male", "深情男友", ModelConfig.MODEL_ID_RIRIXIN_SENSE_NOVA),
            VoiceInfo("man_qiangyu", "强欲霸总", "zh-CN", "male", "强欲霸总", ModelConfig.MODEL_ID_RIRIXIN_SENSE_NOVA),
        )
    )
    
    /**
     * Minimax TTS音色配置
     */
    val MINIMAX_VOICES = mapOf(
        ModelConfig.MODEL_ID_MINIMAX_SPEECH to listOf(
            VoiceInfo("male-qn-qingse", "青涩男声", "zh-CN", "male", "青涩男声，适合年轻男性角色", ModelConfig.MODEL_ID_MINIMAX_SPEECH),
            VoiceInfo("female-shaonv", "少女音色", "zh-CN", "female", "少女音色", ModelConfig.MODEL_ID_MINIMAX_SPEECH),
            VoiceInfo("male-qn-badao", "霸道青年音色", "zh-CN", "male", "霸道青年音色", ModelConfig.MODEL_ID_MINIMAX_SPEECH),
            VoiceInfo("male-qn-jingying", "精英青年音色", "zh-CN", "male", "精英青年音色", ModelConfig.MODEL_ID_MINIMAX_SPEECH),
            VoiceInfo("male-qn-daxuesheng", "青年大学生音色", "zh-CN", "male", "青年大学生音色", ModelConfig.MODEL_ID_MINIMAX_SPEECH),
            VoiceInfo("badao_shaoye", "霸道少爷", "zh-CN", "male", "霸道少爷", ModelConfig.MODEL_ID_MINIMAX_SPEECH)
        )
    )
    
    /**
     * Azure TTS音色配置
     */
    val AZURE_VOICES = mapOf(
        "azure-tts-v1" to listOf(
            VoiceInfo("zh-CN-XiaoxiaoNeural", "晓晓", "zh-CN", "female", "Azure中文女声", "azure-tts-v1"),
            VoiceInfo("zh-CN-YunxiNeural", "云希", "zh-CN", "male", "Azure中文男声", "azure-tts-v1"),
            VoiceInfo("en-US-JennyNeural", "Jenny", "en-US", "female", "Azure英文女声", "azure-tts-v1"),
            VoiceInfo("en-US-GuyNeural", "Guy", "en-US", "male", "Azure英文男声", "azure-tts-v1")
        )
    )
    
    /**
     * 获取指定模型的所有音色
     */
    fun getVoicesForModel(modelId: String): List<VoiceInfo> {
        return when {
            RIRIXIN_VOICES.containsKey(modelId) -> RIRIXIN_VOICES[modelId] ?: emptyList()
            MINIMAX_VOICES.containsKey(modelId) -> MINIMAX_VOICES[modelId] ?: emptyList()
            AZURE_VOICES.containsKey(modelId) -> AZURE_VOICES[modelId] ?: emptyList()
            else -> emptyList()
        }
    }
    
    /**
     * 获取指定提供商的所有音色
     */
    fun getVoicesForProvider(provider: TTSProviderType): List<VoiceInfo> {
        return when (provider) {
            TTSProviderType.RIRIXIN -> RIRIXIN_VOICES.values.flatten()
            TTSProviderType.MINIMAX -> MINIMAX_VOICES.values.flatten()
            TTSProviderType.AZURE -> AZURE_VOICES.values.flatten()
            else -> emptyList()
        }
    }
    
    /**
     * 获取所有音色
     */
    fun getAllVoices(): List<VoiceInfo> {
        return RIRIXIN_VOICES.values.flatten() + 
               MINIMAX_VOICES.values.flatten() + 
               AZURE_VOICES.values.flatten()
    }
    
    /**
     * 根据音色ID查找音色信息
     */
    fun findVoiceById(voiceId: String): VoiceInfo? {
        return getAllVoices().find { it.voiceId == voiceId }
    }
    
    /**
     * 根据音色ID查找所属模型
     */
    fun findModelByVoiceId(voiceId: String): String? {
        return findVoiceById(voiceId)?.modelId
    }
    
    /**
     * 验证音色是否在指定模型中可用
     */
    fun isVoiceAvailableInModel(voiceId: String, modelId: String): Boolean {
        val voices = getVoicesForModel(modelId)
        return voices.any { it.voiceId == voiceId }
    }

    fun getDefaultVoiceByModel(modelId: String): String {
        return when (modelId) {
            ModelConfig.MODEL_ID_MINIMAX_SPEECH -> "male-qn-qingse"
            ModelConfig.MODEL_ID_RIRIXIN_NOVA -> "cheerfulvoice-general-male-cn"
            ModelConfig.MODEL_ID_RIRIXIN_SENSE_NOVA -> "fusion-voice-1"
            else -> {
                // 如果没有找到匹配的模型，返回第一个可用的音色
                val voices = getVoicesForModel(modelId)
                voices.firstOrNull()?.voiceId ?: ""
            }
        }
    }
}
