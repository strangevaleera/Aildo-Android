package com.xinqi.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xinqi.ui.theme.AildoTheme
import com.xinqi.utils.llm.LLMManager
import com.xinqi.utils.llm.modal.LLMModel
import com.xinqi.utils.llm.tts.VoiceInfo
import com.xinqi.utils.llm.tts.modal.TTSFactory
import com.xinqi.utils.llm.tts.modal.TTSModel
import kotlinx.coroutines.launch

/**
 * 模型设置页面
 * 提供LLM模型选择、TTS模型选择和音色选择功能
 */
class ModelSettingsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AildoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ModelSettingsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: ModelSettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val currentLLMModel by viewModel.currentLLMModel.collectAsState()
    val currentTTSModel by viewModel.currentTTSModel.collectAsState()
    val currentVoice by viewModel.currentVoice.collectAsState()
    val availableVoices by viewModel.availableVoices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val llmManager = remember { LLMManager.getInstance(context) }
    
    LaunchedEffect(Unit) {
        viewModel.loadCurrentSettings(llmManager)
        viewModel.loadAvailableVoices(llmManager)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模型设置") },
                navigationIcon = {
                    IconButton(onClick = { /* 返回上一页 */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LLM模型选择
            item {
                LLMModelSelectionCard(
                    currentModel = currentLLMModel,
                    onModelSelect = { model ->
                        scope.launch {
                            viewModel.updateLLMModel(llmManager, model)
                        }
                    }
                )
            }
            
            // TTS模型选择
            item {
                TTSModelSelectionCard(
                    currentModel = currentTTSModel,
                    onModelSelect = { model ->
                        scope.launch {
                            viewModel.updateTTSModel(llmManager, model)
                        }
                    }
                )
            }
            
            // TTS音色选择
            item {
                TTSVoiceSelectionCard(
                    currentVoice = currentVoice,
                    availableVoices = availableVoices,
                    isLoading = isLoading,
                    onVoiceSelect = { voice ->
                        scope.launch {
                            viewModel.updateTTSVoice(llmManager, voice)
                        }
                    }
                )
            }
            
            // 应用设置按钮
            item {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.applySettings(context, llmManager)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("应用设置")
                }
            }
        }
    }
}

/**
 * LLM模型选择卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LLMModelSelectionCard(
    currentModel: LLMModel,
    onModelSelect: (LLMModel) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "LLM模型",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "LLM模型选择",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 模型选择下拉菜单
            var expanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = currentModel.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("选择LLM模型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    LLMModel.entries.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.displayName) },
                            onClick = {
                                onModelSelect(model)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "当前选择: ${currentModel.displayName} (${currentModel.modelId})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * TTS模型选择卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TTSModelSelectionCard(
    currentModel: TTSModel,
    onModelSelect: (TTSModel) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "TTS模型",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "TTS模型选择",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // TTS模型选择下拉菜单
            var expanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = currentModel.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("选择TTS模型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    TTSFactory.getSupportTTSModels().forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.name) },
                            onClick = {
                                onModelSelect(model)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "当前选择: ${currentModel.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (currentModel.description != null) {
                Text(
                    text = currentModel.description?:"🈚️描述",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * TTS音色选择卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TTSVoiceSelectionCard(
    currentVoice: String,
    availableVoices: List<VoiceInfo>,
    isLoading: Boolean,
    onVoiceSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "TTS音色",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "TTS音色选择",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("加载音色列表...")
                }
            } else if (availableVoices.isNotEmpty()) {
                // 音色选择下拉菜单
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = availableVoices.find { it.voiceId == currentVoice }?.name ?: currentVoice,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择音色") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableVoices.forEach { voice ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(voice.name)
                                        Text(
                                            text = "${voice.language} - ${voice.gender}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onVoiceSelect(voice.voiceId)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "当前选择: ${availableVoices.find { it.voiceId == currentVoice }?.name ?: currentVoice}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "暂无可用音色",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
