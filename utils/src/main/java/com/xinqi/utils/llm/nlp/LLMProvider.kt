package com.xinqi.utils.llm.nlp

import com.xinqi.utils.llm.model.Message
import com.xinqi.utils.llm.model.PromptTemplate

/**
 * LLM统一模型接口, 接入三方模型需适配
 */
interface LLMProvider {
    
    /**
     * 聊天对话
     */
    suspend fun chat(messages: List<Message>, promptTemplate: PromptTemplate? = null): String
    
    /**
     * 流式聊天对话
     */
    fun chatStream(
        messages: List<Message>, 
        promptTemplate: PromptTemplate? = null,
        onChunk: (String, Boolean) -> Unit
    )
    
    /**
     * 文本生成
     */
    suspend fun generateText(prompt: String, maxTokens: Int = 1000, temperature: Float = 0.7f): String

    suspend fun isAvailable(): Boolean

    fun getModelInfo(): String
}
