package com.xinqi.utils.llm.nlp

import android.content.Context
import com.xinqi.utils.llm.LLMConfig
import com.xinqi.utils.llm.model.LLMModel
import com.xinqi.utils.llm.model.Message
import com.xinqi.utils.llm.model.PromptManager
import com.xinqi.utils.llm.model.PromptTemplate
import com.xinqi.utils.llm.nlp.provider.ChatGPTProvider
import com.xinqi.utils.llm.nlp.provider.ClaudeProvider
import com.xinqi.utils.llm.nlp.provider.DoubaoProvider
import com.xinqi.utils.llm.nlp.provider.ErnieBotProvider
import com.xinqi.utils.llm.nlp.provider.GeminiProvider
import com.xinqi.utils.llm.nlp.provider.QwenProvider
import com.xinqi.utils.llm.nlp.provider.SparkDeskProvider
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * NLP（自然语言处理）管理器
 * 对集成各种大模型提供统一的NLP接口
 */
class NLPManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "NLPManager"
        
        @Volatile
        private var INSTANCE: NLPManager? = null
        
        fun getInstance(context: Context): NLPManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NLPManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val modelProviders = ConcurrentHashMap<LLMModel, LLMProvider>()
    private val promptManager = PromptManager()
    
    private val eventListeners = mutableListOf<NLPEventListener>()
    
    /**
     * NLP事件监听器接口
     */
    interface NLPEventListener {
        fun onChatResponse(response: String, model: LLMModel)
        fun onStreamResponse(chunk: String, model: LLMModel, isComplete: Boolean)
        fun onError(error: String, model: LLMModel)
        fun onModelChanged(model: LLMModel)
    }
    
    fun addEventListener(listener: NLPEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }
    
    fun removeEventListener(listener: NLPEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * 初始化
     */
    fun initializeModelProvider(model: LLMModel, config: LLMConfig) {
        val provider = when (model) {
            LLMModel.DOUBAO -> DoubaoProvider(config)
            LLMModel.CHATGPT_3_5, LLMModel.CHATGPT_4, LLMModel.CHATGPT_4_TURBO -> ChatGPTProvider(
                config
            )
            LLMModel.CLAUDE_3_SONNET, LLMModel.CLAUDE_3_HAIKU -> ClaudeProvider(config)
            LLMModel.GEMINI_PRO -> GeminiProvider(config)
            LLMModel.QWEN_TURBO, LLMModel.QWEN_PLUS -> QwenProvider(config)
            LLMModel.SPARK_DESK -> SparkDeskProvider(config)
            LLMModel.ERNIE_BOT, LLMModel.ERNIE_BOT_TURBO -> ErnieBotProvider(config)
        }
        
        modelProviders[model] = provider
        logI("已初始化模型: ${model.displayName}")
    }
    
    /**
     * 聊天对话
     */
    fun chat(
        messages: List<Message>,
        model: LLMModel,
        promptTemplate: PromptTemplate? = null,
        callback: (String) -> Unit
    ) {
        val provider = modelProviders[model]
        if (provider == null) {
            logE("模型未初始化: ${model.displayName}")
            notifyError("模型未初始化: ${model.displayName}", model)
            return
        }
        
        scope.launch {
            try {
                val response = provider.chat(messages, promptTemplate)
                logI("NLPManager.chat provider.chat返回: $response")
                
                withContext(Dispatchers.Main) {
                    callback(response)
                    logI("NLPManager.chat callback完成")
                    notifyChatResponse(response, model)
                }
            } catch (e: Exception) {
                logE("聊天失败: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    notifyError("聊天失败: ${e.message}", model)
                }
            }
        }
    }
    
    /**
     * 流式聊天对话
     */
    fun chatStream(
        messages: List<Message>,
        model: LLMModel,
        promptTemplate: PromptTemplate? = null,
        onChunk: (String, Boolean) -> Unit
    ) {
        val provider = modelProviders[model]
        if (provider == null) {
            logE("模型未初始化: ${model.displayName}")
            notifyError("模型未初始化: ${model.displayName}", model)
            return
        }
        
        try {
            provider.chatStream(messages, promptTemplate) { chunk, isComplete ->
                scope.launch {
                    withContext(Dispatchers.Main) {
                        onChunk(chunk, isComplete)
                        notifyStreamResponse(chunk, model, isComplete)
                    }
                }
            }
        } catch (e: Exception) {
            logE("流式聊天失败: ${e.message}")
            scope.launch {
                withContext(Dispatchers.Main) {
                    notifyError("流式聊天失败: ${e.message}", model)
                }
            }
        }
    }
    
    /**
     * 文本生成
     */
    fun generateText(
        prompt: String,
        model: LLMModel,
        maxTokens: Int = 1000,
        temperature: Float = 0.7f,
        callback: (String) -> Unit
    ) {
        val messages = listOf(Message.user(prompt))
        chat(messages, model, null, callback)
    }
    
    /**
     * 文本摘要
     */
    fun summarizeText(
        text: String,
        model: LLMModel,
        maxLength: Int = 200,
        callback: (String) -> Unit
    ) {
        val prompt = "请对以下文本进行摘要，摘要长度不超过${maxLength}字：\n\n$text"
        val messages = listOf(Message.user(prompt))
        chat(messages, model, null, callback)
    }
    
    /**
     * 文本翻译
     */
    fun translateText(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        model: LLMModel,
        callback: (String) -> Unit
    ) {
        val prompt = "请将以下${sourceLanguage}文本翻译成${targetLanguage}：\n\n$text"
        val messages = listOf(Message.user(prompt))
        chat(messages, model, null, callback)
    }
    
    /**
     * 情感分析
     */
    fun analyzeSentiment(
        text: String,
        model: LLMModel,
        callback: (String) -> Unit
    ) {
        val prompt = "请分析以下文本的情感倾向，并给出详细分析：\n\n$text"
        val messages = listOf(Message.user(prompt))
        chat(messages, model, null, callback)
    }
    
    /**
     * 关键词提取
     */
    fun extractKeywords(
        text: String,
        model: LLMModel,
        maxKeywords: Int = 10,
        callback: (String) -> Unit
    ) {
        val prompt = "请从以下文本中提取${maxKeywords}个关键词：\n\n$text"
        val messages = listOf(Message.user(prompt))
        chat(messages, model, null, callback)
    }

    fun getAvailableModels(): List<LLMModel> {
        return modelProviders.keys.toList()
    }

    fun isModelAvailable(model: LLMModel): Boolean {
        return modelProviders.containsKey(model)
    }

    fun getPromptManager(): PromptManager = promptManager

    fun switchModel(model: LLMModel) {
        if (isModelAvailable(model)) {
            notifyModelChanged(model)
            logI("切换到模型: ${model.displayName}")
        } else {
            logE("模型不可用: ${model.displayName}")
        }
    }
    
    private fun notifyChatResponse(response: String, model: LLMModel) {
        eventListeners.forEach { it.onChatResponse(response, model) }
    }
    
    private fun notifyStreamResponse(chunk: String, model: LLMModel, isComplete: Boolean) {
        eventListeners.forEach { it.onStreamResponse(chunk, model, isComplete) }
    }
    
    private fun notifyError(error: String, model: LLMModel) {
        eventListeners.forEach { it.onError(error, model) }
    }
    
    private fun notifyModelChanged(model: LLMModel) {
        eventListeners.forEach { it.onModelChanged(model) }
    }
}
