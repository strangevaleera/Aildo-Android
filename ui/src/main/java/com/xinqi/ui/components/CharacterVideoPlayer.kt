package com.xinqi.ui.components

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

/**
 * 人物视频播放器组件
 * 支持多种动画控制和事件处理
 */
@UnstableApi
@Composable
fun CharacterVideoPlayer(
    character: String,
    animationType: String = "chat",
    onBodyPartClick: (String, Float, Float) -> Unit,
    onVideoReady: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 创建ExoPlayer实例
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // 设置循环播放
            repeatMode = Player.REPEAT_MODE_ALL
            // 自动播放
            playWhenReady = true
            // 设置音量
            volume = 1.0f
        }
    }
    
    // 监听播放器状态
    var isVideoReady by remember { mutableStateOf(false) }
    
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !isVideoReady) {
                    isVideoReady = true
                    onVideoReady()
                }
            }
        })
    }
    
    // 根据角色和动画类型加载对应的视频资源
    LaunchedEffect(character, animationType) {
        val videoUri = getCharacterVideoUri(context, character, animationType)
        val mediaItem = MediaItem.fromUri(videoUri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }
    
    // 在组件销毁时释放播放器
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    // 视频播放器视图
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                // 隐藏播放器控制UI
                useController = false
                // 设置视频缩放模式为填充整个屏幕
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                // 设置背景颜色
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // 获取触摸坐标
                    val x = offset.x / size.width.toFloat()
                    val y = offset.y / size.height.toFloat()
                    
                    // 检测点击的身体部位
                    val bodyPart = detectBodyPart(x, y)
                    onBodyPartClick(bodyPart, x, y)
                }
            }
    )
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

/**
 * 检测点击的身体部位
 * 使用相对坐标系统 (0.0 - 1.0)
 */
private fun detectBodyPart(x: Float, y: Float): String {
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
 * 身体部位信息数据类
 */
data class BodyPartInfo(
    val part: String,
    val details: String,
    val x: Float,
    val y: Float
)
