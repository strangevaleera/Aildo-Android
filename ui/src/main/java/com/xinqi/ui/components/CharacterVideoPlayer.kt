package com.xinqi.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.xinqi.ui.character.CharacterModel
import kotlinx.coroutines.delay
import com.xinqi.utils.log.logI

/**
 * 人物视频播放器组件
 */
@UnstableApi
@Composable
fun CharacterVideoPlayer(
    character: String,
    animationType: String = "chat",
    onBodyPartClick: (String, Float, Float) -> Unit,
    onContinuousClick: ((Int, String, Float, Float) -> Unit)? = null,
    onLongPress: ((String, Float, Float) -> Unit)? = null,
    onVideoReady: () -> Unit = {},
    playMode: PlayMode = PlayMode.LOOP,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    //连续点击检测
    var clickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    var lastClickPosition by remember { mutableStateOf(Pair(0f, 0f)) }
    val continuousClickThreshold = 500L //连续点击时间阈值ms
    val maxClickCount = 5
    //视频过渡管理
    val videoTransitionManager = remember { VideoTransitionManager(context) }
    val currentPlayer = remember { videoTransitionManager.initializeMainPlayer() }
    var currentVideoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }
    var isVideoReady by remember { mutableStateOf(false) }
    LaunchedEffect(currentPlayer) {
        currentPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !isVideoReady) {
                    isVideoReady = true
                    onVideoReady()
                }
                
                // 处理播放完成事件
                if (playbackState == Player.STATE_ENDED) {
                    when (playMode) {
                        PlayMode.ONCE -> {
                            currentPlayer.pause()
                        }
                        PlayMode.LOOP -> {
                            currentPlayer.seekTo(0)
                            currentPlayer.play()
                        }
                    }
                }
            }
        })
    }
    //预加载视频资源
    LaunchedEffect(Unit) {
        videoTransitionManager.preloadVideos()
    }
    // 根据角色和动画类型加载对应的视频资源
    LaunchedEffect(character, animationType) {
        val newVideoInfo = VideoInfo(character, animationType)
        if (currentVideoInfo != newVideoInfo) {
            videoTransitionManager.switchVideo(
                character = character,
                animation = animationType,
                playMode = playMode,
                onTransitionStart = {
                    isTransitioning = true
                },
                onTransitionComplete = {
                    isTransitioning = false
                    currentVideoInfo = newVideoInfo
                }
            )
        }
    }

    LaunchedEffect(clickCount) {
        if (clickCount > 0) {
            delay(continuousClickThreshold)
            if (clickCount > 0) {
                if (clickCount >= 2) {
                    val (x, y) = lastClickPosition
                    val bodyPart = detectBodyPart(x, y)
                    handleContinuousClick(clickCount, bodyPart, x, y, onContinuousClick)
                }
                clickCount = 0
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoTransitionManager.release()
        }
    }
    
    Box(modifier = modifier) {
        //主播放器
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = currentPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    keepScreenOn = true
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures (
                        onLongPress = {
                            offset ->
                                //获取触摸坐标
                                val x = offset.x / size.width.toFloat()
                                val y = offset.y / size.height.toFloat()

                                val bodyPart = detectBodyPart(x, y)
                                onLongPress?.invoke(bodyPart, x, y)
                        },

                        onTap = {
                            offset ->
                                // 处理点击事件
                                val currentTime = System.currentTimeMillis()
                                val x = offset.x / size.width.toFloat()
                                val y = offset.y / size.height.toFloat()
                                
                                // 位置相近且时间间隔短：连续点击事件
                                if (currentTime - lastClickTime < continuousClickThreshold && 
                                    isPositionClose(x, y, lastClickPosition.first, lastClickPosition.second)) {
                                    clickCount++
                                    if (clickCount > maxClickCount) {
                                        clickCount = maxClickCount
                                    }
                                } else {
                                    clickCount = 1
                                }
                                
                                lastClickTime = currentTime
                                lastClickPosition = Pair(x, y)

                                //检测点击的身体部位
                                val bodyPart = detectBodyPart(x, y)
                                onBodyPartClick(bodyPart, x, y)
                        }
                    )
                }
        )
        
        //过渡遮罩层 减少黑屏问题
        AnimatedVisibility(
            visible = isTransitioning,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f),
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f)
                            )
                        )
                    )
            )
        }
        
        if (isTransitioning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f))
            ) {
                //加载动画
            }
        }
    }
}

private fun performSeamlessSwitch(
    currentPlayer: ExoPlayer,
    preloadedPlayer: ExoPlayer
) {
    //将预加载的播放器设置为当前播放器
    preloadedPlayer.playWhenReady = true
    preloadedPlayer.volume = 1.0f
    
    //释放旧的播放器
    currentPlayer.release()
}

private fun performOptimizedSwitch(
    context: Context,
    player: ExoPlayer,
    character: String,
    animationType: String,
    playMode: PlayMode
) {
    player.pause()
    val videoUri = getCharacterVideoUri(context, character, animationType)
    val mediaItem = MediaItem.fromUri(videoUri)
    player.repeatMode = when (playMode) {
        PlayMode.ONCE -> Player.REPEAT_MODE_OFF
        PlayMode.LOOP -> Player.REPEAT_MODE_ALL
    }
    player.setMediaItem(mediaItem)
    player.prepare()
    player.playWhenReady = true
}


enum class PlayMode {
    LOOP,
    ONCE
}

data class VideoInfo(
    val character: String,
    val animationType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as VideoInfo
        return character == other.character && animationType == other.animationType
    }
    
    override fun hashCode(): Int {
        var result = character.hashCode()
        result = 31 * result + animationType.hashCode()
        return result
    }
}

/**
 * 检查两个位置是否相近
 */
private fun isPositionClose(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
    val distanceThreshold = 0.1f // 位置相近的阈值（相对坐标的10%）
    val distance = kotlin.math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))
    return distance < distanceThreshold
}

/**
 * 处理连续点击事件
 */
private fun handleContinuousClick(
    clickCount: Int, 
    bodyPart: String, 
    x: Float, 
    y: Float,
    onContinuousClick: ((Int, String, Float, Float) -> Unit)?
) {
    onContinuousClick?.invoke(clickCount, bodyPart, x, y)
    when (clickCount) {
        2 -> {
            logI("CharacterVideoPlayer", "双击检测到 - 部位: $bodyPart, 位置: ($x, $y)")
        }
        3 -> {
            logI("CharacterVideoPlayer", "三击检测到 - 部位: $bodyPart, 位置: ($x, $y)")
        }
        4 -> {
            logI("CharacterVideoPlayer", "四击检测到 - 部位: $bodyPart, 位置: ($x, $y)")
        }
        5 -> {
            logI("CharacterVideoPlayer", "五击检测到 - 部位: $bodyPart, 位置: ($x, $y)")
        }
    }
}

/**
 * 根据角色和动画类型获取视频资源URI
 */
private fun getCharacterVideoUri(
    context: android.content.Context, 
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

/**
 * 检测点击的身体部位
 * 使用相对坐标系统 (0.0 - 1.0)
 */
private fun detectBodyPart(x: Float, y: Float): String {
    // 使用CharacterModel的默认身体部位检测逻辑
    // todo: 这里可以根据需要传入具体的角色ID来获取更精确的配置
    return when {
        y < 0.25f -> "head"      // 头部区域 (0-25%)
        y < 0.65f -> "body"      // 身体区域 (25-65%)
        else -> "legs"            // 腿部区域 (65-100%)
    }
}

/**
 * 获取身体部位的详细位置信息
 */
fun getBodyPartDetails(x: Float, y: Float): BodyPartInfo {
    val part = detectBodyPart(x, y)
    val details = when (part) {
        "head" -> when {
            x < 0.3f -> "head_left"
            x > 0.7f -> "head_right"
            else -> "head_center"
        }
        "body" -> when {
            x < 0.3f -> "body_left"
            x > 0.7f -> "body_right"
            else -> "body_center"
        }
        "legs" -> when {
            x < 0.3f -> "legs_left"
            x > 0.7f -> "legs_right"
            else -> "legs_center"
        }
        else -> "unknown"
    }
    
    return BodyPartInfo(
        part = part,
        details = details,
        x = x,
        y = y
    )
}

/**
 * 根据角色ID获取身体部位的详细位置信息
 */
fun getBodyPartDetails(characterId: String, x: Float, y: Float): BodyPartInfo? {
    val bodyPart = CharacterModel.detectBodyPart(characterId, x, y)
    val clickArea = bodyPart?.let { part ->
        CharacterModel.getClickAreaResponse(characterId, part.id, x, y)
    }
    return if (bodyPart != null && clickArea != null) {
        BodyPartInfo(
            part = bodyPart.id,
            details = clickArea.response,
            x = x,
            y = y
        )
    } else {
        // 回退到默认逻辑
        getBodyPartDetails(x, y)
    }
}

/**
 * 身体部位信息数据类
 */
data class BodyPartInfo(
    val part: String,
    val details: String,
    val x: Float,
    val y: Float
)
