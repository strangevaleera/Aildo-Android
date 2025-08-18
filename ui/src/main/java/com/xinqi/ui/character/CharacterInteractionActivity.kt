package com.xinqi.ui.character

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.xinqi.ui.components.AnimationControlPanel
import com.xinqi.ui.components.CharacterVideoPlayer
import com.xinqi.ui.theme.AildoTheme
import com.xinqi.utils.log.logI

/**
 * 人物交互界面
 */
@UnstableApi
class CharacterInteractionActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AildoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CharacterInteractionScreen()
                }
            }
        }
    }
}

@UnstableApi
@Composable
fun CharacterInteractionScreen(
    modifier: Modifier = Modifier,
    viewModel: CharacterInteractionViewModel = viewModel()
) {
    val currentCharacter by viewModel.currentCharacter.collectAsState()
    val currentAnimation by viewModel.currentAnimation.collectAsState()
    val showCharacterSelector by viewModel.showCharacterSelector.collectAsState()
    val bluetoothConnected by viewModel.bluetoothConnected.collectAsState()
    val context = LocalContext.current
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 视频播放器
        CharacterVideoPlayer(
            character = currentCharacter,
            animationType = currentAnimation,
            onBodyPartClick = { bodyPart, x, y ->
                viewModel.onBodyPartClick(context, currentCharacter, bodyPart, x, y)
            },
            onLongPress = { bodyPart, x, y ->
                viewModel.onBodyPartLongPress(context, currentCharacter, bodyPart, x, y)
            },
            onContinuousClick = { count, bodyPart, x, y ->
                viewModel.onContinuousClick(context, currentCharacter, count, bodyPart, x, y)
            },
            // 根据动画类型设置播放模式
            playMode = when (currentAnimation) {
                "angry" -> com.xinqi.ui.components.PlayMode.ONCE  // 生气动画只播放一次
                "shy" -> com.xinqi.ui.components.PlayMode.ONCE   // 害羞动画只播放一次
                else -> com.xinqi.ui.components.PlayMode.LOOP    // 其他动画循环播放
            },
            onVideoReady = {
                logI("视频准备就绪")
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 人物选择按钮（左上角）
        CharacterSelectionButton(
            isVisible = showCharacterSelector,
            onToggle = { viewModel.toggleCharacterSelector() },
            onCharacterSelect = { character ->
                viewModel.selectCharacter(character)
                viewModel.hideCharacterSelector()
            },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        )
        
        // 动画控制面板（右侧）
        AnimationControlPanel(
            isVisible = false,
            currentAnimation = currentAnimation,
            onAnimationSelect = { animationType ->
                viewModel.playAnimation(animationType)
            },
            onToggleVisibility = { /* 添加状态管理 */ },
            modifier = Modifier
                .align(Alignment.CenterEnd)
        )
        
        // 蓝牙状态指示器（右上角）
        BluetoothStatusIndicator(
            isConnected = bluetoothConnected,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd)
        )
    }
}

/**
 * 蓝牙状态指示器
 */
@Composable
private fun BluetoothStatusIndicator(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icons.Default.Bluetooth
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isConnected) "已连接" else "未连接",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
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
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "选择人物",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 人物列表 - 使用CharacterModel配置
                CharacterModel.CHARACTERS.forEach { character ->
                    CharacterOption(
                        name = character.displayName,
                        id = character.id,
                        iconRes = character.iconRes,
                        onClick = { onCharacterSelect(character.id) }
                    )
                }
            }
        }
    } else {
        // 隐藏状态下的触发按钮
        FloatingActionButton(
            onClick = onToggle,
            modifier = Modifier.wrapContentSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(
                text = "换男友",
                modifier = Modifier.wrapContentSize(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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