package com.xinqi.utils.llm.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.xinqi.utils.llm.tts.modal.TTSConfig
import com.xinqi.utils.llm.tts.modal.TTSProviderType
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import com.xinqi.utils.llm.tts.provider.RirixinTTSProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/**
 * TTS（文本转语音）管理器
 * 1. 支持多种TTS服务提供商
 * 2. 统一管理TTS配置和音频播放
 * 3. 支持流式TTS和文件TTS
 */
class TTSManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "TTSManager"
        private const val SAMPLE_RATE = 32000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private var BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        
        @Volatile
        private var INSTANCE: TTSManager? = null
        
        fun getInstance(context: Context): TTSManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TTSManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var playingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val eventListeners = mutableListOf<TTSEventListener>()
    private var currentProvider: TTSProvider? = null
    private var currentConfig: TTSConfig? = null
    
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
            else -> {
                logE("不支持的TTS提供商类型: ${config.provider}")
                null
            }
        }
        
        logI("TTS提供商初始化完成: ${config.provider}")
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
     * 获取当前TTS提供商信息
     */
    fun getCurrentProviderInfo(): String {
        return currentProvider?.getModelInfo() ?: "未初始化"
    }
    
    /**
     * 播放音频文件
     */
    fun playAudio(audioFile: File) {
        if (isPlaying) {
            logI("音频正在播放中")
            return
        }
        
        try {
            val audioAttributes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            } else {
                null
            }
            
            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes!!)
                    .setAudioFormat(AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .setEncoding(AUDIO_FORMAT)
                        .build())
                    .setBufferSizeInBytes(BUFFER_SIZE)
                    .build()
            } else {
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE,
                    AudioTrack.MODE_STREAM
                )
            }
            
            isPlaying = true
            notifyTTSStarted()
            
            playingJob = scope.launch {
                try {
                    audioTrack?.play()
                    
                    val audioData = audioFile.readBytes()
                    val headerSize = 44 // WAV文件头大小
                    val actualAudioData = audioData.copyOfRange(headerSize, audioData.size)
                    
                    audioTrack?.write(actualAudioData, 0, actualAudioData.size)
                    
                    audioTrack?.stop()
                    audioTrack?.release()
                    audioTrack = null
                    
                    isPlaying = false
                    notifyTTSCompleted()
                    
                } catch (e: Exception) {
                    logE("播放音频失败: ${e.message}")
                    isPlaying = false
                    notifyError("播放音频失败: ${e.message}")
                }
            }
            
            logI("开始播放音频: ${audioFile.absolutePath}")
            
        } catch (e: Exception) {
            logE("初始化音频播放失败: ${e.message}")
            notifyError("初始化音频播放失败: ${e.message}")
        }
    }

    fun stopPlayback() {
        if (!isPlaying) return
        
        try {
            playingJob?.cancel()
            
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            
            isPlaying = false
            notifyTTSStopped()
            logI("音频播放已停止")
            
        } catch (e: Exception) {
            logE("停止播放失败: ${e.message}")
        }
    }

    fun isPlaying(): Boolean = isPlaying
    
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
