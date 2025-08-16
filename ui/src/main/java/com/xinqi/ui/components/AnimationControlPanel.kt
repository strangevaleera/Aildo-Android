package com.xinqi.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 动画控制面板组件
 * 提供人物动画的播放控制功能
 */
@Composable
fun AnimationControlPanel(
    isVisible: Boolean,
    currentAnimation: String,
    onAnimationSelect: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) {
        return
    }
    Column(
        modifier = modifier
    ) {
        // 切换按钮
        FloatingActionButton(
            onClick = onToggleVisibility,
            modifier = Modifier
                .padding(16.dp)
                .size(56.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "显示控制面板"
            )
        }
        
        // 动画控制面板
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
                    .width(200.dp)
            ) {
                Text(
                    text = "动画控制",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 动画选项列表
                AnimationOption(
                    icon = Icons.Default.Chat,
                    title = "聊天",
                    id = "chat",
                    isSelected = currentAnimation == "chat",
                    onClick = { onAnimationSelect("chat") }
                )
                
                AnimationOption(
                    icon = Icons.Default.Warning,
                    title = "生气",
                    id = "angry",
                    isSelected = currentAnimation == "angry",
                    onClick = { onAnimationSelect("angry") }
                )
                
                AnimationOption(
                    icon = Icons.Default.Favorite,
                    title = "害羞",
                    id = "shy",
                    isSelected = currentAnimation == "shy",
                    onClick = { onAnimationSelect("shy") }
                )
                
                AnimationOption(
                    icon = Icons.Default.PlayArrow,
                    title = "默认",
                    id = "default",
                    isSelected = currentAnimation == "default",
                    onClick = { onAnimationSelect("default") }
                )
            }
        }
    }
}

/**
 * 动画选项组件
 */
@Composable
private fun AnimationOption(
    icon: ImageVector,
    title: String,
    id: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        
        if (isSelected) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 动画控制数据类
 */
data class AnimationControl(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val description: String
)

/**
 * 预定义的动画控制选项
 */
val AnimationControls = listOf(
    AnimationControl(
        id = "chat",
        title = "聊天",
        icon = Icons.Default.Chat,
        description = "播放聊天动画"
    ),
    AnimationControl(
        id = "angry",
        title = "生气",
        icon = Icons.Default.Warning,
        description = "播放生气动画"
    ),
    AnimationControl(
        id = "shy",
        title = "害羞",
        icon = Icons.Default.Favorite,
        description = "播放害羞动画"
    ),
    AnimationControl(
        id = "default",
        title = "默认",
        icon = Icons.Default.PlayArrow,
        description = "播放默认动画"
    )
)

