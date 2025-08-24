package com.xinqi.utils.llm

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.xinqi.utils.llm.asr.ASRManager
import com.xinqi.utils.llm.modal.LLMModel
import com.xinqi.utils.llm.modal.Message
import com.xinqi.utils.llm.modal.PromptManager
import com.xinqi.utils.llm.modal.PromptTemplate
import com.xinqi.utils.llm.nlp.NLPManager
import com.xinqi.utils.llm.tts.TTSFactory
import com.xinqi.utils.llm.tts.TTSManager
import com.xinqi.utils.log.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * LLM主管理器
 * 整合ASR、NLP、TTS能力，提供统一的接口
 */
class LLMManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "LLMManager"
        
        @Volatile
        private var INSTANCE: LLMManager? = null
        
        fun getInstance(context: Context): LLMManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LLMManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val asrManager = ASRManager.getInstance(context)
    private val nlpManager = NLPManager.getInstance(context)
    private val ttsManager = TTSManager.getInstance(context)
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var currentConfig: LLMConfig? = null
    private var currentModel: LLMModel = LLMModel.DOUBAO
    
    private val eventListeners = mutableListOf<LLMEventListener>()
    
    /**
     * LLM事件监听器接口.
     */
    interface LLMEventListener {
        fun onModelChanged(model: LLMModel)
        fun onConfigUpdated(config: LLMConfig)
        fun onError(error: String)
    }
    
    fun addEventListener(listener: LLMEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }
    
    fun removeEventListener(listener: LLMEventListener) {
        eventListeners.remove(listener)
    }

    fun initializeModel(context: Context, model: LLMModel) {
        //1. init llm model
        runCatching {
            val config = LLMConfig.getDefaultConfig(context, model)
            currentConfig = config
            currentModel = model

            nlpManager.initializeModelProvider(model, config)

            logI("已初始化模型: ${model.displayName}")
            notifyModelChanged(model)
            notifyConfigUpdated(config)
        }.onFailure {
            logI("初始化模型失败: ${it.message}")
            notifyError("初始化模型失败: ${it.message}")
        }
        //2. init tts model
        initTts(context)
    }

    private fun initTts(context: Context) {
        val config = TTSFactory.createRirixinConfig(context)
        ttsManager.initializeProvider(config)

        ttsManager.addEventListener(object : TTSManager.TTSEventListener {
            override fun onTTSStarted() {

            }
            override fun onTTSCompleted() {

            }
            override fun onTTSStopped() {

            }
            override fun onTTSProgress(progress: Float) {

            }
            override fun onError(error: String) {

            }
        })
    }
    
    /**
     * 语音对话（ASR -> NLP -> TTS）
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun voiceChat(
        onSpeechRecognized: (String) -> Unit,
        onResponseGenerated: (String) -> Unit,
        onAudioGenerated: (File?) -> Unit
    ) {
        // 1. ASR: 开始语音识别
        asrManager.addEventListener(object : ASRManager.ASREventListener {
            override fun onRecordingStarted() {
                logI("开始录音")
            }
            
            override fun onRecordingStopped() {
                logI("录音结束")
            }
            
            override fun onAudioDataReceived(audioData: ByteArray) {
                // 实时语音处理
            }
            
            override fun onRecognitionResult(result: String, confidence: Float) {
                logI("语音识别结果: $result (置信度: $confidence)")
                onSpeechRecognized(result)
                
                // 2. NLP: 生成AI对话响应
                val messages = listOf(Message.user(result))
                nlpManager.chat(messages, currentModel, null) { response ->
                    logI("AI响应: $response")
                    onResponseGenerated(response)
                    
                    // 3. TTS: 转换为语音
                    ttsManager.textToSpeech(response) { audioFile ->
                        logI("TTS完成: ${audioFile?.absolutePath}")
                        onAudioGenerated(audioFile)
                    }
                }
            }
            
            override fun onError(error: String) {
                logI("ASR错误: $error")
                notifyError("语音识别错误: $error")
            }
        })
        
        // 开始录音
        try {
            asrManager.startRecording()
        } catch (e: SecurityException) {
            logI("录音权限被拒: ${e.message}")
            notifyError("录音权限被拒，检查应用权限设置")
        }
    }
    
    /**
     * 文本输入对话
     */
    fun textChat(
        text: String,
        promptTemplate: PromptTemplate? = null,
        onResponse: (String) -> Unit
    ) {
        logI("LLMManager.textChat 开始，输入: $text, 当前模型: ${currentModel.displayName}")
        val messages = listOf(Message.user(text))
        
        val wrappedCallback = { response: String ->
            logI("LLMManager.textChat 收到回调响应: $response")
            onResponse(response)
        }
        
        nlpManager.chat(messages, currentModel, promptTemplate, wrappedCallback)
    }
    
    /**
     * 流式文本对话
     */
    fun textChatStream(
        text: String,
        promptTemplate: PromptTemplate? = null,
        onChunk: (String, Boolean) -> Unit
    ) {
        val messages = listOf(Message.user(text))
        scope.launch {
            nlpManager.chatStream(messages, currentModel, promptTemplate, onChunk)
        }
    }
    
    /**
     * 文本转语音
     */
    fun textToSpeech(
        text: String,
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        onComplete: (java.io.File?) -> Unit
    ) {
        ttsManager.textToSpeech(text, null, speed, pitch, onComplete = onComplete)
    }
    
    /**
     * 语音转文本
     */
    fun speechToText(
        onResult: (String, Float) -> Unit,
        onError: (String) -> Unit
    ) {
        asrManager.addEventListener(object : ASRManager.ASREventListener {
            override fun onRecordingStarted() {}
            override fun onRecordingStopped() {}
            override fun onAudioDataReceived(audioData: ByteArray) {}
            
            override fun onRecognitionResult(result: String, confidence: Float) {
                onResult(result, confidence)
            }
            
            override fun onError(error: String) {
                onError(error)
            }
        })
        
        try {
            asrManager.startRecording()
        } catch (e: SecurityException) {
            logI("录音权限被拒绝: ${e.message}")
            notifyError("录音权限被拒绝，请检查应用权限设置")
        }
    }
    
    /**
     * 停止语音识别
     */
    fun stopSpeechRecognition() {
        asrManager.stopRecording()
    }
    
    /**
     * 停止语音播放
     */
    fun stopSpeechPlayback() {
        ttsManager.stopPlayback()
    }
    
    /**
     * 切换模型
     */
    fun switchModel(model: LLMModel) {
        if (currentModel != model) {
            currentModel = model
            notifyModelChanged(model)
            logI("切换到模型: ${model.displayName}")
        }
    }
    
    /**
     * 更新配置
     */
    fun updateConfig(config: LLMConfig) {
        currentConfig = config
        currentModel = config.model
        
        nlpManager.initializeModelProvider(config.model, config)
        
        logI("配置已更新: ${config.model.displayName}")
        notifyConfigUpdated(config)
    }
    
    /**
     * 获取当前模型
     */
    fun getCurrentModel(): LLMModel = currentModel
    
    /**
     * 获取当前配置
     */
    fun getCurrentConfig(): LLMConfig? = currentConfig
    
    /**
     * 获取可用的模型列表
     */
    fun getAvailableModels(): List<LLMModel> {
        return nlpManager.getAvailableModels()
    }
    
    /**
     * 获取Prompt管理器
     */
    fun getPromptManager(): PromptManager {
        return nlpManager.getPromptManager()
    }
    
    /**
     * 检查模型是否可用
     */
    fun isModelAvailable(model: LLMModel): Boolean {
        return nlpManager.isModelAvailable(model)
    }

    fun getASRManager(): ASRManager = asrManager

    fun getNLPManager(): NLPManager = nlpManager

    fun getTTSManager(): TTSManager = ttsManager
    
    private fun notifyModelChanged(model: LLMModel) {
        eventListeners.forEach { it.onModelChanged(model) }
    }
    
    private fun notifyConfigUpdated(config: LLMConfig) {
        eventListeners.forEach { it.onConfigUpdated(config) }
    }
    
    private fun notifyError(error: String) {
        eventListeners.forEach { it.onError(error) }
    }
}
