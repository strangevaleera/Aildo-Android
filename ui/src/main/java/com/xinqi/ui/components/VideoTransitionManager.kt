package com.xinqi.ui.components

import android.content.Context
import android.media.MediaCodec
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.xinqi.utils.log.logI
import kotlinx.coroutines.*

/**
 * 视频过渡管理器
 * 专门处理视频切换时的黑屏问题
 */
@UnstableApi
class VideoTransitionManager(
    private val context: Context
) {
    // 主播放器
    private var currentPlayer: ExoPlayer? = null
    // 预加载播放器池
    private val preloadPool = mutableMapOf<String, ExoPlayer>()
    // 过渡状态
    private var isTransitioning = false
    private val preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 初始化主播放器
     */
    fun initializeMainPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            volume = 1.0f
            // 设置缓冲参数，减少黑屏
            setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        }.also { currentPlayer = it }
    }
    
    /**
     * 预加载视频资源
     */
    fun preloadVideos(characters: List<String>, animations: List<String>) {
        preloadScope.launch {
            characters.forEach { character ->
                animations.forEach { animation ->
                    val key = "${character}_${animation}"
                    if (!preloadPool.containsKey(key)) {
                        try {
                            val player = ExoPlayer.Builder(context).build().apply {
                                playWhenReady = false
                                volume = 0f
                                // 设置预加载参数
                                setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                            }
                            
                            val videoUri = getCharacterVideoUri(context, character, animation)
                            val mediaItem = MediaItem.fromUri(videoUri)
                            player.setMediaItem(mediaItem)
                            player.prepare()
                            
                            preloadPool[key] = player
                            logI("VideoTransitionManager", "预加载成功: $key")
                        } catch (e: Exception) {
                            logI("VideoTransitionManager", "预加载失败: $key, 错误: ${e.message}")
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 执行优化的视频切换
     */
    fun switchVideo(
        character: String,
        animation: String,
        playMode: PlayMode,
        onTransitionStart: () -> Unit = {},
        onTransitionComplete: () -> Unit = {}
    ) {
        if (isTransitioning) {
            logI("VideoTransitionManager", "正在切换中，忽略切换请求")
            return
        }
        
        val currentPlayer = currentPlayer ?: return
        isTransitioning = true
        onTransitionStart()
        
        try {
            // 检查是否有预加载的视频
            val preloadKey = "${character}_${animation}"
            val preloadedPlayer = preloadPool[preloadKey]
            
            if (preloadedPlayer != null && preloadedPlayer.playbackState == Player.STATE_READY) {
                // 使用预加载的视频进行无缝切换
                performSeamlessSwitch(currentPlayer, preloadedPlayer, playMode)
            } else {
                // 传统切换方式，优化了过渡效果
                performOptimizedSwitch(currentPlayer, character, animation, playMode)
            }
            
            logI("VideoTransitionManager", "视频切换完成: $preloadKey")
        } catch (e: Exception) {
            logI("VideoTransitionManager", "视频切换失败: ${e.message}")
            // 回退到传统方式
            performOptimizedSwitch(currentPlayer, character, animation, playMode)
        } finally {
            isTransitioning = false
            onTransitionComplete()
        }
    }
    
    /**
     * 执行无缝视频切换
     */
    private fun performSeamlessSwitch(
        currentPlayer: ExoPlayer,
        preloadedPlayer: ExoPlayer,
        playMode: PlayMode
    ) {
        // 设置预加载播放器的播放模式
        preloadedPlayer.repeatMode = when (playMode) {
            PlayMode.ONCE -> Player.REPEAT_MODE_OFF
            PlayMode.LOOP -> Player.REPEAT_MODE_ALL
        }
        // 将预加载的播放器设置为当前播放器
        preloadedPlayer.playWhenReady = true
        preloadedPlayer.volume = 1.0f
        // 更新当前播放器引用
        this.currentPlayer = preloadedPlayer
        // 释放旧的播放器
        currentPlayer.release()
        // 从预加载池中移除已使用的播放器
        val keyToRemove = preloadPool.entries.find { it.value == preloadedPlayer }?.key
        keyToRemove?.let { preloadPool.remove(it) }
        // 重新预加载该视频
        keyToRemove?.let { key ->
            val parts = key.split("_")
            if (parts.size == 2) {
                preloadVideos(listOf(parts[0]), listOf(parts[1]))
            }
        }
    }
    
    /**
     * 执行优化的视频切换
     */
    private fun performOptimizedSwitch(
        player: ExoPlayer,
        character: String,
        animation: String,
        playMode: PlayMode
    ) {
        // 先暂停当前播放
        player.pause()
        // 设置新的媒体项
        val videoUri = getCharacterVideoUri(context, character, animation)
        val mediaItem = MediaItem.fromUri(videoUri)
        player.repeatMode = when (playMode) {
            PlayMode.ONCE -> Player.REPEAT_MODE_OFF
            PlayMode.LOOP -> Player.REPEAT_MODE_ALL
        }
        player.setMediaItem(mediaItem)
        player.prepare()
        // 延迟恢复播放，确保视频已准备好
        player.playWhenReady = true
    }
    
    /**
     * 获取当前播放器
     */
    fun getCurrentPlayer(): ExoPlayer? = currentPlayer
    
    /**
     * 检查是否正在切换
     */
    fun isTransitioning(): Boolean = isTransitioning
    
    /**
     * 释放资源
     */
    fun release() {
        preloadScope.cancel()
        currentPlayer?.release()
        preloadPool.values.forEach { it.release() }
        preloadPool.clear()
    }
    
    /**
     * 根据角色和动画类型获取视频资源URI
     */
    private fun getCharacterVideoUri(
        context: Context, 
        character: String, 
        animationType: String
    ): String {
        val baseUri = "android.resource://${context.packageName}/raw"
        
        return when (character) {
            "fig1" -> when (animationType) {
                "chat" -> "$baseUri/fig1_chat"
                "angry" -> "$baseUri/fig1_angry"
                "shy" -> "$baseUri/fig1_shy_bottom"
                else -> "$baseUri/fig1_chat"
            }
            "fig2" -> when (animationType) {
                "chat" -> "$baseUri/fig2_chat"
                "angry" -> "$baseUri/fig2_angry"
                "shy" -> "$baseUri/fig2_shy"
                else -> "$baseUri/fig2_chat"
            }
            "fig3" -> when (animationType) {
                "chat" -> "$baseUri/fig3_chat"
                "angry" -> "$baseUri/fig3_angry"
                "shy" -> "$baseUri/fig3_shy"
                else -> "$baseUri/fig3_chat"
            }
            else -> "$baseUri/fig1_chat"
        }
    }
}
