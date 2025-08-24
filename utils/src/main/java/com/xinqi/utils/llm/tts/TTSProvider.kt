package com.xinqi.utils.llm.tts

import java.io.File

/**
 * TTS统一模型接口, 接入三方TTS模型需适配
 */
interface TTSProvider {
    
    /**
     * 文本转语音
     */
    suspend fun textToSpeech(
        text: String,
        voice: String,
        speed: Float,
        pitch: Float,
        volume: Float
    ): File?
    
    /**
     * 流式文本转语音
     */
    fun textToSpeechStream(
        text: String,
        voice: String,
        speed: Float,
        pitch: Float,
        volume: Float,
        onChunk: (ByteArray, Boolean) -> Unit
    )
    
    /**
     * 获取可用的语音列表
     */
    suspend fun getAvailableVoices(): List<VoiceInfo>
    
    /**
     * 检查服务是否可用
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * 获取模型信息
     */
    fun getModelInfo(): String
    
    /**
     * 获取支持的音频格式
     */
    fun getSupportedFormats(): List<String>
}

/**
 * 语音信息
 */
data class VoiceInfo(
    val voiceId: String,
    val name: String,
    val language: String,
    val gender: String,
    val description: String? = null
)
