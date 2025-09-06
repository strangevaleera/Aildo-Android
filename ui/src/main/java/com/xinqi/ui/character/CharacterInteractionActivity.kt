package com.xinqi.ui.character

import android.content.Intent
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
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.xinqi.feature.LLMIntegrator
import com.xinqi.ui.components.AnimationControlPanel
import com.xinqi.ui.components.CharacterVideoPlayer
import com.xinqi.ui.theme.AildoTheme
import com.xinqi.utils.common.ioScope
import com.xinqi.utils.log.logI
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.alpha

/**
 * 人物交互界面
 */
@UnstableApi
class CharacterInteractionActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 蓝牙管理器将在Composable中初始化
        
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

        ioScope.launch {
            LLMIntegrator.init(baseContext)
        }
    }
}

@UnstableApi
@Composable
@Preview
fun CharacterInteractionScreen(
    modifier: Modifier = Modifier,
    viewModel: CharacterInteractionViewModel = viewModel()
) {
    val currentCharacter by viewModel.currentCharacter.collectAsState()
    val currentAnimation by viewModel.currentAnimation.collectAsState()
    val showCharacterSelector by viewModel.showCharacterSelector.collectAsState()
    val bluetoothConnected by viewModel.bluetoothConnected.collectAsState()
    val context = LocalContext.current
    
    // 初始化蓝牙管理器
    LaunchedEffect(Unit) {
        viewModel.initializeBluetoothManager(context)
    }
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
            playMode = when (currentAnimation) {
                "angry" -> com.xinqi.ui.components.PlayMode.ONCE
                "shy" -> com.xinqi.ui.components.PlayMode.ONCE
                else -> com.xinqi.ui.components.PlayMode.LOOP
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
            onToggleVisibility = {  },
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

/*        // 大模型对话测试按钮（右下角）
        LLMTrigger(modifier = Modifier
            .padding(16.dp)
            .align(Alignment.BottomEnd)
        ) {
            //LLMIntegrator.testQuery(context, "")
            //LLMIntegrator.testQueryStream(context, "")
        }*/

        // 模型设置按钮（左边）
        ModelSettingsButton(modifier = Modifier
            .padding(16.dp)
            .align(Alignment.CenterStart)
        ) {
            com.xinqi.ui.PageJumper.openModelSettingsPage(context)
        }

        // 底部对话界面
        ChatInterface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

/**
 * 蓝牙状态指示
 */
@Composable
private fun BluetoothStatusIndicator(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Card(
        onClick = {
            // 点击打开蓝牙管理页面
            val intent = Intent(context, com.xinqi.ui.bluetooth.BluetoothManagerActivity::class.java)
            context.startActivity(intent)
        },
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
            Icon(
                imageVector = if (isConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                contentDescription = null,
                tint = if (isConnected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isConnected) "已连接" else "未连接",
                style = MaterialTheme.typography.labelSmall,
                color = if (isConnected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onErrorContainer
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
        }
    } else {
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

/**
 * llm对话启动
 */
@Composable
private fun LLMTrigger(
    modifier: Modifier,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp)
                .clickable { onClick()}
            , verticalAlignment = Alignment.CenterVertically
        ) {
            Icons.Default.Face
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "llm测试",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * 模型设置按钮
 */
@Composable
private fun ModelSettingsButton(
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icons.Default.Settings
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "模型设置",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * 底部对话界面
 */
@Composable
private fun ChatInterface(
    modifier: Modifier = Modifier
) {
    var userInput by remember { mutableStateOf("") }
    // 连续句子逐条展示的消息列表（最多保留2条，上一条淡出后移除）
    data class MessageItem(val id: Long, val text: String, val isFading: Boolean)
    val messages = remember { mutableStateListOf<MessageItem>() }
    var nextId by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }
    var branchChoices by remember { mutableStateOf(LLMIntegrator.getBranchChoices().map { it.label to it.prompt }) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier.imePadding(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 回复展示框
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                messages.forEachIndexed { index, item ->
                    val alpha by animateFloatAsState(
                        targetValue = if (item.isFading) 0f else 1f,
                        animationSpec = tween(durationMillis = 600),
                        label = "fadeAlpha"
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = item.text,
                            modifier = Modifier
                                .padding(12.dp)
                                .alpha(alpha),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // 分支按钮（如果有）
            if (branchChoices.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    branchChoices.take(2).forEach { (label, prompt) ->
                        Button(
                            onClick = {
                                if (!isLoading) {
                                    isLoading = true
                                    // 点击后立即隐藏本轮分支
                                    branchChoices = emptyList()
                                    LLMIntegrator.query(
                                        context = context,
                                        query = prompt,
                                        onResponse = { response ->
                                            // 将多行按顺序推入，累积显示所有句子，依次播放音频
                                            val lines = response.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
                                            scope.launch {
                                                // 改为在播放阶段逐条添加与移除，保证同一时间只显示一条对白
                                                // 所有文字显示完成后，依次播放每句的音频（基于播放完成回调串联）
                                                scope.launch {
                                                    var index = 0
                                                    fun playNext() {
                                                        if (index >= lines.size) {
                                                            isLoading = false
                                                            branchChoices = LLMIntegrator.getBranchChoices().map { it.label to it.prompt }
                                                            return
                                                        }
                                                        val line = lines[index]
                                                        // 添加当前句子作为唯一可见条目
                                                        messages.clear()
                                                        messages.add(MessageItem(id = nextId++, text = line, isFading = false))
                                                        LLMIntegrator.mLLmManager.textToSpeech(
                                                            text = line,
                                                            speed = 1.0f,
                                                            pitch = 1.0f
                                                        ) { audioFile ->
                                                            if (audioFile != null) {
                                                                LLMIntegrator.mLLmManager.getTTSManager().playAudio(audioFile) {
                                                                    // 如果不是最后一句，则淡出后移除；最后一句保持驻留
                                                                    val isLast = (index == lines.size - 1)
                                                                    if (!isLast && messages.isNotEmpty()) {
                                                                        val last = messages.last()
                                                                        val idx = messages.lastIndex
                                                                        messages[idx] = last.copy(isFading = true)
                                                                        scope.launch {
                                                                            delay(600)
                                                                            messages.clear()
                                                                            index += 1
                                                                            playNext()
                                                                        }
                                                                    } else {
                                                                        // 最后一句：不清空，等待下一轮开始时由新句替换
                                                                        index += 1
                                                                        playNext()
                                                                    }
                                                                }
                                                            } else {
                                                                // 无音频也保持推进
                                                                val isLast = (index == lines.size - 1)
                                                                if (!isLast) messages.clear()
                                                                index += 1
                                                                playNext()
                                                            }
                                                        }
                                                    }
                                                    playNext()
                                                }
                                            }
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            // 输入框和发送按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 文本输入框
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text("请输入您的问题...") },
                    singleLine = true,
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // 发送按钮
                Button(
                    onClick = {
                        if (userInput.isNotEmpty() && !isLoading) {
                            isLoading = true
                            val toSend = userInput
                            userInput = ""

                            LLMIntegrator.query(
                                context = context,
                                query = toSend,
                                onResponse = { response ->
                                    // 将多行按顺序推入，累积显示所有句子，依次播放音频
                                    val lines = response.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
                                    scope.launch {
                                        // 改为在播放阶段逐条添加与移除，保证同一时间只显示一条对白
                                        // 所有文字显示完成后，依次播放每句的音频（基于播放完成回调串联）
                                        scope.launch {
                                            var index = 0
                                            fun playNext() {
                                                if (index >= lines.size) {
                                                    isLoading = false
                                                    branchChoices = LLMIntegrator.getBranchChoices().map { it.label to it.prompt }
                                                    return
                                                }
                                                val line = lines[index]
                                                // 添加当前句子作为唯一可见条目
                                                messages.clear()
                                                messages.add(MessageItem(id = nextId++, text = line, isFading = false))
                                                LLMIntegrator.mLLmManager.textToSpeech(
                                                    text = line,
                                                    speed = 1.0f,
                                                    pitch = 1.0f
                                                ) { audioFile ->
                                                    if (audioFile != null) {
                                                        LLMIntegrator.mLLmManager.getTTSManager().playAudio(audioFile) {
                                                            // 如果不是最后一句，则淡出后移除；最后一句保持驻留
                                                            val isLast = (index == lines.size - 1)
                                                            if (!isLast && messages.isNotEmpty()) {
                                                                val last = messages.last()
                                                                val idx = messages.lastIndex
                                                                messages[idx] = last.copy(isFading = true)
                                                                scope.launch {
                                                                    delay(600)
                                                                    messages.clear()
                                                                    index += 1
                                                                    playNext()
                                                                }
                                                            } else {
                                                                // 最后一句：不清空，等待下一轮开始时由新句替换
                                                                index += 1
                                                                playNext()
                                                            }
                                                        }
                                                    } else {
                                                        // 无音频也保持推进
                                                        val isLast = (index == lines.size - 1)
                                                        if (!isLast) messages.clear()
                                                        index += 1
                                                        playNext()
                                                    }
                                                }
                                            }
                                            playNext()
                                        }
                                    }
                                }
                            )
                        }
                    },
                    enabled = userInput.isNotEmpty() && !isLoading,
                    modifier = Modifier.wrapContentSize()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("发送")
                    }
                }
            }
        }
    }
}