package com.xinqi.aiildo

import android.content.Context
import com.xinqi.utils.common.ConfigManager
import com.xinqi.utils.log.logI

/**
 * 配置项初始化
 * - 应用启动时从BuildConfig读取配置并设置到ConfigManager中
 */
object ConfigInitializer {

    fun init(context: Context) {
        logI("=== Config Initialization ===")

        ConfigManager.init(context)

        // 火山
        ConfigManager.setLLMDoubaoConfig(
            apiKey = BuildConfig.LLM_DOUBAO_API_KEY,
        )
        
        // 商汤
        ConfigManager.setTTSRirixinConfig(
            ak = BuildConfig.TTS_RIRIXIN_AK,
            sk = BuildConfig.TTS_RIRIXIN_SK
        )

        // minimax
        ConfigManager.setTTSMinimaxConfig(
            apiKey = BuildConfig.TTS_MINIMAX_API_KEY,
            groupId = BuildConfig.TTS_MINIMAX_GROUP_ID
        )
        
        logI("=== Config Initialization Complete ===")
    }
}
