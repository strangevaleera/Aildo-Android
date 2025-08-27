package com.xinqi.ui.components

import android.content.Context
import android.media.MediaCodec
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.xinqi.ui.character.CharacterModel
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
    private var isTransitioning = false
    private val preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initializeMainPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            volume = 1.0f
            setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        }.also { currentPlayer = it }
    }
    
    /**
     * 预加载视频资源
     */
    fun preloadVideos(characters: MutableList<String> = mutableListOf (), animations: MutableList<String> = mutableListOf ()) {
        preloadScope.launch {
            if (characters.isNotEmpty() && animations.isNotEmpty()) {
                // 加载单独选中项目
                characters.forEach { character ->
                    animations.forEach { animation ->
                        val key = "${character}_${animation}"
                        if (!preloadPool.containsKey(key)) {
                            try {
                                val player = ExoPlayer.Builder(context).build().apply {
                                    playWhenReady = false
                                    volume = 0f
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
            } else {
                // 尝试加载其他所有未加载
                val loadCharacters = CharacterModel.CHARACTERS
                loadCharacters.forEach { character ->
                    character.animations.forEach { animation ->
                        val key = "${character.id}_${animation.type}"
                        if (!preloadPool.containsKey(key)) {
                            try {
                                val player = ExoPlayer.Builder(context).build().apply {
                                    playWhenReady = false
                                    volume = 0f
                                    setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                }

                                val videoUri = getCharacterVideoUri(context, character.id, animation.type)
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
    }

    /**
     * 动画视频切换
     */
    fun switchVideo(
        character: String,
        animation: String,
        playMode: PlayMode,
        onTransitionStart: () -> Unit = {},
        onTransitionComplete: () -> Unit = {}
    ) {
        if (isTransitioning) {
            logI("VideoTransitionManager", "isTransitioning")
            return
        }
        
        val currentPlayer = currentPlayer ?: return
        isTransitioning = true
        onTransitionStart()
        
        try {
            val preloadKey = "${character}_${animation}"
            val preloadedPlayer = preloadPool[preloadKey]
            
            if (preloadedPlayer != null && preloadedPlayer.playbackState == Player.STATE_READY) {
                performSeamlessSwitch(currentPlayer, preloadedPlayer, playMode)
            } else {
                performOptimizedSwitch(currentPlayer, character, animation, playMode)
            }
            logI("VideoTransitionManager", "视频切换完成: $preloadKey")
        } catch (e: Exception) {
            logI("VideoTransitionManager", "视频切换失败: ${e.message}")
            performOptimizedSwitch(currentPlayer, character, animation, playMode)
        } finally {
            isTransitioning = false
            onTransitionComplete()
        }
    }

    private fun performSeamlessSwitch(
        currentPlayer: ExoPlayer,
        preloadedPlayer: ExoPlayer,
        playMode: PlayMode
    ) {
        preloadedPlayer.repeatMode = when (playMode) {
            PlayMode.ONCE -> Player.REPEAT_MODE_OFF
            PlayMode.LOOP -> Player.REPEAT_MODE_ALL
        }
        preloadedPlayer.playWhenReady = true
        preloadedPlayer.volume = 1.0f
        this.currentPlayer = preloadedPlayer
        currentPlayer.release()
        // 从预加载池中移除已使用的播放器
        val keyToRemove = preloadPool.entries.find { it.value == preloadedPlayer }?.key
        keyToRemove?.let { preloadPool.remove(it) }
        keyToRemove?.let { key ->
            val parts = key.split("_")
            if (parts.size == 2) {
                preloadVideos(mutableListOf(parts[0]), mutableListOf(parts[1]))
            }
        }
    }

    private fun performOptimizedSwitch(
        player: ExoPlayer,
        character: String,
        animation: String,
        playMode: PlayMode
    ) {
        player.pause()
        val videoUri = getCharacterVideoUri(context, character, animation)
        val mediaItem = MediaItem.fromUri(videoUri)
        player.repeatMode = when (playMode) {
            PlayMode.ONCE -> Player.REPEAT_MODE_OFF
            PlayMode.LOOP -> Player.REPEAT_MODE_ALL
        }
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun getCurrentPlayer(): ExoPlayer? = currentPlayer

    fun isTransitioning(): Boolean = isTransitioning

    fun release() {
        preloadScope.cancel()
        currentPlayer?.release()
        preloadPool.values.forEach { it.release() }
        preloadPool.clear()
    }

    private fun getCharacterVideoUri(
        context: Context, 
        character: String, 
        animationType: String
    ): String {
        val baseUri = "android.resource://${context.packageName}/raw"
        val characterConfig = CharacterModel.getCharacter(character)
        val animationConfig = characterConfig?.animations?.find { it.type == animationType }
        
        return if (animationConfig != null) {
            val resourceName = context.resources.getResourceEntryName(animationConfig.videoRes)
            "$baseUri/$resourceName"
        } else {
            "$baseUri/fig1_chat"
        }
    }
}

