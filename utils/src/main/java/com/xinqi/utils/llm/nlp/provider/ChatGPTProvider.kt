package com.xinqi.utils.llm.nlp.provider

import com.google.gson.Gson
import com.xinqi.utils.llm.LLMConfig
import com.xinqi.utils.llm.model.Message

import com.xinqi.utils.llm.model.PromptTemplate
import com.xinqi.utils.llm.nlp.LLMProvider
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * ChatGPT集成器
 *
 * @see <a href=""> </a>
 * */
class ChatGPTProvider(private val config: LLMConfig) : LLMProvider {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .readTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .writeTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .build()
    
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    override suspend fun chat(messages: List<Message>, promptTemplate: PromptTemplate?): String {
        try {
            val requestBody = buildChatRequest(messages, promptTemplate)
            val request = Request.Builder()
                .url("${config.baseUrl}/chat/completions")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            logI("发送聊天请求")
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                logI("ChatGPT响应成功: $responseBody")
                return parseChatResponse(responseBody)
            } else {
                logE("ChatGPT请求失败: ${response.code} - ${response.message}")
                throw Exception("ChatGPT请求失败: ${response.code}")
            }
            
        } catch (e: Exception) {
            logE("ChatGPT聊天异常: ${e.message}")
            throw e
        }
    }
    
    override fun chatStream(
        messages: List<Message>, 
        promptTemplate: PromptTemplate?,
        onChunk: (String, Boolean) -> Unit
    ) {
        try {
            val requestBody = buildChatRequest(messages, promptTemplate, stream = true)
            val request = Request.Builder()
                .url("${config.baseUrl}/chat/completions")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            logI("发送流式聊天请求")
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                response.body?.source()?.let { source ->
                    var fullResponse = ""
                    while (true) {
                        val line = source.readUtf8LineStrict()
                        if (line.startsWith("data: ")) {
                            val data = line.substring(6)
                            if (data == "[DONE]") {
                                onChunk("", true)
                                break
                            } else {
                                val chunk = parseStreamChunk(data)
                                if (chunk.isNotEmpty()) {
                                    fullResponse += chunk
                                    onChunk(chunk, false)
                                }
                            }
                        }
                    }
                    logI("ChatGPT流式响应完成: $fullResponse")
                }
            } else {
                logE("ChatGPT流式请求失败: ${response.code}")
                throw Exception("ChatGPT流式请求失败: ${response.code}")
            }
            
        } catch (e: Exception) {
            logE("ChatGPT流式聊天异常: ${e.message}")
            throw e
        }
    }
    
    override suspend fun generateText(prompt: String, maxTokens: Int, temperature: Float): String {
        val messages = listOf(Message.user(prompt))
        return chat(messages, null)
    }
    
    override suspend fun isAvailable(): Boolean {
        return try {
            val request = Request.Builder()
                .url("${config.baseUrl}/models")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            logE("检查ChatGPT可用性失败: ${e.message}")
            false
        }
    }
    
    override fun getModelInfo(): String {
        return "ChatGPT模型 (${config.model.modelId})"
    }
    
    private fun buildChatRequest(
        messages: List<Message>, 
        promptTemplate: PromptTemplate?,
        stream: Boolean = false
    ): RequestBody {
        val systemPrompt = promptTemplate?.template ?: "你是一个有用的AI助手"
        
        val requestData = mapOf(
            "model" to config.model.modelId,
            "messages" to messages.map { message ->
                mapOf(
                    "role" to message.role.name.lowercase(),
                    "content" to if (message.role.name == "SYSTEM") systemPrompt else message.content
                )
            },
            "max_tokens" to config.maxTokens,
            "temperature" to config.temperature,
            "top_p" to config.topP,
            "frequency_penalty" to config.frequencyPenalty,
            "presence_penalty" to config.presencePenalty,
            "stream" to stream
        )
        
        val json = gson.toJson(requestData)
        logI("ChatGPT请求数据: $json")
        return json.toRequestBody(mediaType)
    }
    
    private fun parseChatResponse(responseBody: String?): String {
        if (responseBody.isNullOrEmpty()) return ""
        
        return try {
            val response = gson.fromJson(responseBody, ChatGPTResponse::class.java)
            response.choices?.firstOrNull()?.message?.content ?: ""
        } catch (e: Exception) {
            logE("解析ChatGPT响应失败: ${e.message}")
            ""
        }
    }
    
    private fun parseStreamChunk(data: String): String {
        return try {
            val response = gson.fromJson(data, ChatGPTStreamResponse::class.java)
            response.choices?.firstOrNull()?.delta?.content ?: ""
        } catch (e: Exception) {
            logE("解析ChatGPT流式响应失败: ${e.message}")
            ""
        }
    }
    
    data class ChatGPTResponse(
        val choices: List<Choice>?,
        val usage: Usage?
    )
    
    data class Choice(
        val message: MessageContent,
        val finish_reason: String?
    )
    
    data class MessageContent(
        val content: String,
        val role: String
    )
    
    data class Usage(
        val prompt_tokens: Int,
        val completion_tokens: Int,
        val total_tokens: Int
    )
    
    data class ChatGPTStreamResponse(
        val choices: List<StreamChoice>?
    )
    
    data class StreamChoice(
        val delta: StreamDelta,
        val finish_reason: String?
    )
    
    data class StreamDelta(
        val content: String?,
        val role: String?
    )
}
