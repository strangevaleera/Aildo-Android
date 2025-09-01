package com.xinqi.utils.llm.tts

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/**
 * TTS音频播放器
 * 负责TTS音频文件的播放功能
 * 使用ExoPlayer实现
 */
class TTSPlayer private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "TTSPlayer"
        
        @Volatile
        private var INSTANCE: TTSPlayer? = null
        
        fun getInstance(context: Context): TTSPlayer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TTSPlayer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var exoPlayer: ExoPlayer? = null
    private var isPlaying = false
    private var playingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val eventListeners = mutableListOf<TTSPlayerListener>()
    
    /**
     * TTS播放器事件监听接口
     */
    interface TTSPlayerListener {
        fun onPlayStarted()
        fun onPlayCompleted()
        fun onPlayStopped()
        fun onPlayError(error: String)
    }
    
    fun addEventListener(listener: TTSPlayerListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }
    
    fun removeEventListener(listener: TTSPlayerListener) {
        eventListeners.remove(listener)
    }


    /**
     * 播放音频文件
     */
    fun playAudio(audioFile: File) {
        scope.launch {
            playAudioInternal(audioFile)
        }
    }

    private fun playAudioInternal(audioFile: File) {
        if (isPlaying) {
            logI("音频正在播放中")
            return
        }
        
        try {
            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(context).build().apply {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .build()
                    setAudioAttributes(audioAttributes, true)
                    
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    logI("ExoPlayer准备就绪")
                                }
                                Player.STATE_BUFFERING -> {
                                    logI("ExoPlayer缓冲中")
                                }
                                Player.STATE_ENDED -> {
                                    logI("ExoPlayer播放结束")
                                    scope.launch {
                                        this@TTSPlayer.isPlaying = false
                                        notifyPlayCompleted()
                                        // 清理播放器
                                        exoPlayer?.release()
                                        exoPlayer = null
                                    }
                                }
                                Player.STATE_IDLE -> {
                                    logI("ExoPlayer空闲状态")
                                }
                            }
                        }
                        
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            logE("ExoPlayer播放错误: ${error.message}")
                            scope.launch {
                                this@TTSPlayer.isPlaying = false
                                notifyPlayError("播放音频失败: ${error.message}")
                                exoPlayer?.release()
                                exoPlayer = null
                            }
                        }
                    })

                }
            }
            
            isPlaying = true
            notifyPlayStarted()
            
            playingJob = scope.launch {
                try {
                    val mediaItem = MediaItem.fromUri(audioFile.toUri())
                    
                    exoPlayer?.apply {
                        setMediaItem(mediaItem)
                        prepare()
                        play()
                    }
                    logI("开始播放音频: ${audioFile.absolutePath}")
                } catch (e: Exception) {
                    logE("播放音频失败: ${e.message}")
                    isPlaying = false
                    notifyPlayError("播放音频失败: ${e.message}")
                    // 清理播放器
                    exoPlayer?.release()
                    exoPlayer = null
                }
            }
            
        } catch (e: Exception) {
            logE("初始化音频播放失败: ${e.message}")
            notifyPlayError("初始化音频播放失败: ${e.message}")
        }
    }

    /**
     * 停止播放
     */
    fun stopPlayback() {
        scope.run {
            stopPlaybackInternal()
        }
    }

    fun stopPlaybackInternal() {
        if (!isPlaying) return
        
        try {
            playingJob?.cancel()
            
            exoPlayer?.apply {
                stop()
                release()
            }
            exoPlayer = null
            
            isPlaying = false
            notifyPlayStopped()
            logI("音频播放已停止")
            
        } catch (e: Exception) {
            logE("停止播放失败: ${e.message}")
        }
    }

    /**
     * 检查是否正在播放
     */
    fun isPlaying(): Boolean = isPlaying
    
    /**
     * 释放资源
     */
    fun release() {
        scope.run {
            try {
                playingJob?.cancel()
                exoPlayer?.release()
                exoPlayer = null
                isPlaying = false
                logI("TTSPlayer资源已释放")
            } catch (e: Exception) {
                logE("释放资源失败: ${e.message}")
            }
        }
    }
    
    private fun notifyPlayStarted() {
        eventListeners.forEach { it.onPlayStarted() }
    }
    
    private fun notifyPlayCompleted() {
        eventListeners.forEach { it.onPlayCompleted() }
    }
    
    private fun notifyPlayStopped() {
        eventListeners.forEach { it.onPlayStopped() }
    }
    
    private fun notifyPlayError(error: String) {
        eventListeners.forEach { it.onPlayError(error) }
    }
}
