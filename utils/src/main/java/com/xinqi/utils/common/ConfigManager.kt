package com.xinqi.utils.common

import android.content.Context
import android.content.SharedPreferences
import com.xinqi.utils.log.logI

/**
 * 配置管理器
 * 统一管理应用的所有配置项，包括LLM、TTS等服务的配置
 */
object ConfigManager {
    
    private const val PREFS_NAME = "aildo_config"
    private const val KEY_LLM_DOUBAO_API_KEY = "llm_doubao_api_key"
    private const val KEY_TTS_RIRIXIN_AK = "tts_ririxin_ak"
    private const val KEY_TTS_RIRIXIN_SK = "tts_ririxin_sk"
    
    private var prefs: SharedPreferences? = null
    
    /**
     * 初始化配置管理器
     */
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    /**
     * 设置LLM豆包配置
     */
    fun setLLMDoubaoConfig(apiKey: String) {
        prefs?.edit()?.apply {
            putString(KEY_LLM_DOUBAO_API_KEY, apiKey)
            apply()
        }
    }
    
    /**
     * 获取LLM豆包API Key
     */
    fun getLLMDoubaoApiKey(): String {
        return (prefs?.getString(KEY_LLM_DOUBAO_API_KEY, "") ?: "")
    }
    
    /**
     * 设置TTS日日新配置
     */
    fun setTTSRirixinConfig(ak: String, sk: String) {
        prefs?.edit()?.apply {
            putString(KEY_TTS_RIRIXIN_AK, ak)
            putString(KEY_TTS_RIRIXIN_SK, sk)
            apply()
        }
    }
    
    /**
     * 获取TTS日日新AK
     */
    fun getTTSRirixinAk(): String {
        return prefs?.getString(KEY_TTS_RIRIXIN_AK, "") ?: ""
    }
    
    /**
     * 获取TTS日日新SK
     */
    fun getTTSRirixinSk(): String {
        return prefs?.getString(KEY_TTS_RIRIXIN_SK, "") ?: ""
    }
    
    /**
     * 清除所有配置
     */
    fun clearAllConfigs() {
        prefs?.edit()?.clear()?.apply()
    }
}
