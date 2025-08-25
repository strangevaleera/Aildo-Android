package com.xinqi.aiildo

import android.content.Context
import com.xinqi.utils.common.ConfigManager
import com.xinqi.utils.log.logI

/**
 * 配置初始化器
 * 在应用启动时从BuildConfig读取配置并设置到ConfigManager中
 */
object ConfigInitializer {
    
    /**
     * 初始化所有配置
     */
    fun init(context: Context) {
        logI("=== Config Initialization ===")

        ConfigManager.init(context)

        // 初始化火山配置
        ConfigManager.setLLMDoubaoConfig(
            apiKey = BuildConfig.LLM_DOUBAO_API_KEY,
        )
        
        // 初始化商汤配置
        ConfigManager.setTTSRirixinConfig(
            ak = BuildConfig.TTS_RIRIXIN_AK,
            sk = BuildConfig.TTS_RIRIXIN_SK
        )

        // 初始化minimax配置
        ConfigManager.setTTSMinimaxConfig(
            apiKey = BuildConfig.TTS_MINIMAX_API_KEY,
            groupId = BuildConfig.TTS_MINIMAX_GROUP_ID
        )
        
        logI("=== Config Initialization Complete ===")
    }
}
