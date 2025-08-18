package com.xinqi.ui.character

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * 人物交互界面
 * 包含全屏视频播放和人物选择功能
 * 备选组件
 */
@UnstableApi
@Composable
fun CharacterInteractionScreen(
    modifier: Modifier = Modifier,
    onCharacterSelect: (String) -> Unit = {},
    onBodyPartClick: (String, Float, Float) -> Unit = { _, _, _ -> }
) {
    var showCharacterSelector by remember { mutableStateOf(false) }
    var currentCharacter by remember { mutableStateOf("fig1") }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 视频播放器
        CharacterVideoPlayer(
            character = currentCharacter,
            onBodyPartClick = onBodyPartClick,
            modifier = Modifier.fillMaxSize()
        )
        
        // 人物选择按钮（左上角）
        CharacterSelectionButton(
            isVisible = showCharacterSelector,
            onToggle = { showCharacterSelector = !showCharacterSelector },
            onCharacterSelect = { character ->
                currentCharacter = character
                showCharacterSelector = false
                onCharacterSelect(character)
            },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        )
    }
}

/**
 * 人物视频播放器组件
 */
@UnstableApi
@Composable
private fun CharacterVideoPlayer(
    character: String,
    onBodyPartClick: (String, Float, Float) -> Unit,
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
        }
    }
    
    // 根据角色加载对应的视频资源
    LaunchedEffect(character) {
        val videoUri = getCharacterVideoUri(context, character)
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
                // 设置视频缩放模式
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // 获取触摸坐标
                    val x = offset.x / size.width.toFloat()
                    val y = offset.y / size.height.toFloat()
                    
                    // 检测点击的身体部位
                    //val bodyPart = detectBodyPart(character, x, y)
                    //onBodyPartClick(character, )
                }
            }
    )
}

/**
 * 人物选择按钮组件
 */
@Composable
private fun CharacterSelectionButton(
    isVisible: Boolean,
    onToggle: () -> Unit,
    onCharacterSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        // 人物选择面板
        Column(
            modifier = modifier
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            Text(
                text = "选择人物",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 人物列表
            CharacterModel.CHARACTERS.forEach { character ->
                CharacterOption(
                    name = character.displayName,
                    id = character.id,
                    iconRes = character.iconRes,
                    onClick = { onCharacterSelect(character.id) }
                )
            }
        }
    } else {
        // 隐藏状态下的触发按钮
        FloatingActionButton(
            onClick = onToggle,
            modifier = modifier.size(56.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "选择人物"
            )
        }
    }
}

/**
 * 人物选项组件
 */
@Composable
private fun CharacterOption(
    name: String,
    id: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = name,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 根据角色获取视频资源URI
 */
private fun getCharacterVideoUri(context: Context, character: String): String {
    val characterConfig = CharacterModel.getCharacter(character)
    val animationConfig = characterConfig?.animations?.firstOrNull { it.type == "chat" }
    
    return if (animationConfig != null) {
        "android.resource://${context.packageName}/raw/${animationConfig.videoRes}"
    } else {
        // 默认返回fig1_chat
        "android.resource://${context.packageName}/raw/fig1_chat"
    }
}

/**
 * 检测点击的身体部位
 */
private fun detectBodyPart(character: String, x: Float, y: Float): String? {
    val bodyPart = CharacterModel.detectBodyPart(character, x, y)
    return bodyPart?.id
}