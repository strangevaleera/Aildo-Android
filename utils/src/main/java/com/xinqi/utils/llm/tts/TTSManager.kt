package com.xinqi.utils.llm.tts

import android.content.Context
import com.xinqi.utils.llm.tts.model.SAMPLE_RATE
import com.xinqi.utils.llm.tts.model.TTSConfig
import com.xinqi.utils.llm.tts.model.TTSProviderType
import com.xinqi.utils.llm.tts.model.VoiceInfo
import com.xinqi.utils.llm.tts.provider.MinimaxProvider
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import com.xinqi.utils.llm.tts.provider.RirixinTTSProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * TTS（文本转语音）管理器
 * 1. 支持多种TTS服务提供商
 * 2. 统一管理TTS配置
 * 3. 支持流式TTS和文件TTS
 * 4. 使用TTSPlayer进行音频播放
 */
class TTSManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "TTSManager"
        private const val CHANNEL_CONFIG = 2 // 立体声
        private const val AUDIO_FORMAT = 2 // 16位PCM

        @Volatile
        private var INSTANCE: TTSManager? = null
        
        fun getInstance(context: Context): TTSManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TTSManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * 不同模型返回音频配置可能不同，动态调整
         * */
        fun resetPlayer(channel: Int) {
            // ExoPlayer会自动处理音频配置，这里保留接口兼容性
        }
    }

    private val eventListeners = mutableListOf<TTSEventListener>()
    private var currentProvider: TTSProvider? = null
    private var currentConfig: TTSConfig? = null
    
    // 使用TTSPlayer进行音频播放
    private val ttsPlayer = TTSPlayer.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    /**
     * TTS事件监听器接口
     */
    interface TTSEventListener {
        fun onTTSStarted()
        fun onTTSCompleted()
        fun onTTSStopped()
        fun onTTSProgress(progress: Float)
        fun onError(error: String)
    }
    
    fun addEventListener(listener: TTSEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }
    
    fun removeEventListener(listener: TTSEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * 初始化TTS提供商
     */
    fun initializeProvider(config: TTSConfig) {
        currentConfig = config
        currentProvider = when (config.provider) {
            TTSProviderType.RIRIXIN -> RirixinTTSProvider(context, config)
            TTSProviderType.MINIMAX -> MinimaxProvider(context, config)
            else -> {
                logE("不支持的TTS提供商类型: ${config.provider}")
                null
            }
        }
        
        // 设置TTSPlayer的事件监听器
        ttsPlayer.addEventListener(object : TTSPlayer.TTSPlayerListener {
            override fun onPlayStarted() {
                notifyTTSStarted()
            }
            
            override fun onPlayCompleted() {
                notifyTTSCompleted()
            }
            
            override fun onPlayStopped() {
                notifyTTSStopped()
            }
            
            override fun onPlayError(error: String) {
                notifyError(error)
            }
        })
    }
    
    /**
     * 文本转语音
     */
    fun textToSpeech(
        text: String,
        voice: String? = null,
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        volume: Float = 1.0f,
        onComplete: (File?) -> Unit
    ) {
        if (currentProvider == null) {
            logE("TTS提供商未初始化")
            notifyError("TTS提供商未初始化")
            onComplete(null)
            return
        }

        if (currentConfig == null || currentProvider == null) {
            logE("当前配置， $currentConfig 当前服务商, $currentProvider 异常")
            return
        }
        
        scope.launch {
            try {
                logI("开始文本转语音: $text")
                notifyTTSStarted()
                val config = currentConfig!!
                val actualVoice = voice ?: config.voice

                val audioFile = currentProvider!!.textToSpeech(
                    text, actualVoice, speed, pitch, volume
                )
                if (audioFile != null) {
                    logI("TTS完成，音频文件: ${audioFile.absolutePath}")
                    notifyTTSCompleted()
                    onComplete(audioFile)
                } else {
                    logE("TTS失败")
                    notifyError("TTS转换失败")
                    onComplete(null)
                }
                
            } catch (e: Exception) {
                logE("TTS异常: ${e.message}")
                notifyError("TTS异常: ${e.message}")
                onComplete(null)
            }
        }
    }
    
    /**
     * 流式文本转语音
     */
    fun textToSpeechStream(
        text: String,
        voice: String? = null,
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        volume: Float = 1.0f,
        onChunk: (ByteArray, Boolean) -> Unit
    ) {
        if (currentProvider == null) {
            logE("TTS提供商未初始化")
            notifyError("TTS提供商未初始化")
            onChunk(ByteArray(0), true)
            return
        }
        
        val config = currentConfig!!
        val actualVoice = voice ?: config.voice
        
        currentProvider!!.textToSpeechStream(
            text, actualVoice, speed, pitch, volume, onChunk
        )
    }
    
    /**
     * 获取可用的语音列表
     */
    suspend fun getAvailableVoices(): List<VoiceInfo> {
        return currentProvider?.getAvailableVoices() ?: emptyList()
    }
    
    /**
     * 检查TTS服务是否可用
     */
    suspend fun isTTSAvailable(): Boolean {
        return currentProvider?.isAvailable() ?: false
    }
    
    /**
     * 播放音频文件
     */
    fun playAudio(audioFile: File) {
        ttsPlayer.playAudio(audioFile)
    }

    /**
     * 停止播放
     */
    fun stopPlayback() {
        ttsPlayer.stopPlayback()
    }

    /**
     * 检查是否正在播放
     */
    fun isPlaying(): Boolean = ttsPlayer.isPlaying()
    
    /**
     * 获取当前TTS提供商
     */
    fun getCurrentProvider(): TTSProvider? = currentProvider
    
    /**
     * 获取当前TTS配置
     */
    fun getCurrentConfig(): TTSConfig? = currentConfig
    
    /**
     * 获取模型信息
     */
    fun getModelInfo(): String {
        return currentProvider?.getModelInfo() ?: "未初始化"
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            ttsPlayer.release()
            logI("TTSManager资源已释放")
        } catch (e: Exception) {
            logE("释放资源失败: ${e.message}")
        }
    }
    
    private fun notifyTTSStarted() {
        eventListeners.forEach { it.onTTSStarted() }
    }
    
    private fun notifyTTSCompleted() {
        eventListeners.forEach { it.onTTSCompleted() }
    }
    
    private fun notifyTTSStopped() {
        eventListeners.forEach { it.onTTSStopped() }
    }
    
    private fun notifyTTSProgress(progress: Float) {
        eventListeners.forEach { it.onTTSProgress(progress) }
    }
    
    private fun notifyError(error: String) {
        eventListeners.forEach { it.onError(error) }
    }
}
