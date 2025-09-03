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
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import okhttp3.internal.tls.OkHostnameVerifier

/**
 * 豆包模型,火山云接入
 * @see <a href="https://www.volcengine.com/product/ark">火山引擎 </a>
 */
class DoubaoProvider(private val config: LLMConfig) : LLMProvider {
    
    /**
     * 创建信任所有证书的TrustManager（仅用于测试）
     */
    private fun createTrustAllCerts(): Array<TrustManager> {
        return arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .readTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .writeTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .dns(object : okhttp3.Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> {
                try {
                    logI("DNS解析: $hostname")
                    
                    // 如果是豆包域名，提供备选IP地址
                    if (hostname == "ark.cn-beijing.volces.com") {
                        try {
                            // 先尝试正常DNS解析
                            val addresses = java.net.InetAddress.getAllByName(hostname).toList()
                            logI("DNS解析成功: $hostname -> ${addresses.map { it.hostAddress }}")
                            return addresses
                        } catch (e: Exception) {
                            logE("正常DNS解析失败，使用备选IP: ${e.message}")
                            // 如果DNS解析失败，使用备选IP地址
                            val fallbackIP = "101.126.30.253"
                            val fallbackAddress = java.net.InetAddress.getByName(fallbackIP)
                            logI("使用备选IP: $fallbackIP")
                            return listOf(fallbackAddress)
                        }
                    } else {
                        // 其他域名正常解析
                        val addresses = java.net.InetAddress.getAllByName(hostname).toList()
                        logI("DNS解析成功: $hostname -> ${addresses.map { it.hostAddress }}")
                        return addresses
                    }
                } catch (e: Exception) {
                    logE("DNS解析失败: $hostname, 错误: ${e.message}")
                    throw e
                }
            }
        })
        .hostnameVerifier { _, _ -> true } // 忽略主机名验证
        .addInterceptor { chain ->
            val request = chain.request()
            logI("豆包请求: ${request.url}")
            chain.proceed(request)
        }
        .build()
    
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    override suspend fun chat(messages: List<Message>, promptTemplate: PromptTemplate?): String {
        try {
            logI("DoubaoProvider.chat 开始执行")
            logI("消息数量: ${messages.size}")
            logI("API地址: ${config.baseUrl}")
            logI("API密钥: ${config.apiKey.take(8)}...")
            
            val requestBody = buildChatRequest(messages, promptTemplate)
            val request = Request.Builder()
                .url("${config.baseUrl}/chat/completions")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            logI("发送豆包聊天请求到: ${config.baseUrl}")
            logI("请求头: Authorization=Bearer ${config.apiKey.take(8)}...")
            logI("请求体: ${gson.toJson(requestBody)}")
            
            val response = client.newCall(request).execute()
            logI("豆包响应状态码: ${response.code}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                logI("豆包响应成功: $responseBody")
                if (responseBody.isNullOrEmpty()) {
                    throw Exception("豆包响应体为空")
                }
                return parseChatResponse(responseBody)
            } else {
                val errorBody = response.body?.string() ?: "无错误详情"
                logE("豆包请求失败: ${response.code} - ${response.message} - $errorBody")
                throw Exception("豆包请求失败: ${response.code} - $errorBody")
            }
            
        } catch (e: UnknownHostException) {
            val errorMsg = "无法解析豆包API地址: ${config.baseUrl}，请检查网络连接和API地址配置"
            logE(errorMsg)
            logE("异常类型: ${e.javaClass.simpleName}")
            logE("异常堆栈: ${e.stackTraceToString()}")
            throw Exception(errorMsg)
        } catch (e: Exception) {
            val errorMsg = "豆包聊天异常: ${e.message}"
            logE(errorMsg)
            logE("异常类型: ${e.javaClass.simpleName}")
            logE("异常堆栈: ${e.stackTraceToString()}")
            throw Exception(errorMsg)
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
            
            logI("发送豆包流式聊天请求到: ${config.baseUrl}")
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
                    logI("豆包流式响应完成: $fullResponse")
                }
            } else {
                logE("豆包流式请求失败: ${response.code}")
                throw Exception("豆包流式请求失败: ${response.code}")
            }
            
        } catch (e: UnknownHostException) {
            val errorMsg = "无法解析豆包API地址: ${config.baseUrl}，请检查网络连接和API地址配置"
            logE(errorMsg)
            onChunk(errorMsg, true)
        } catch (e: Exception) {
            val errorMsg = "豆包流式聊天异常: ${e.message}"
            logE(errorMsg)
            onChunk(errorMsg, true)
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
            
            logI("检查豆包可用性: ${config.baseUrl}")
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: UnknownHostException) {
            logE("无法解析豆包API地址: ${config.baseUrl}")
            false
        } catch (e: Exception) {
            logE("检查豆包可用性失败: ${e.message}")
            false
        }
    }
    
    override fun getModelInfo(): String {
        return "豆包模型 (${config.model.modelId}) - API地址: ${config.baseUrl}"
    }
    
    private fun buildChatRequest(
        messages: List<Message>, 
        promptTemplate: PromptTemplate?,
        stream: Boolean = false
    ): okhttp3.RequestBody {
        // 构建消息列表
        val messageList = mutableListOf<Map<String, String>>()
        // 增加系统提示词prompt
        val systemPrompt = promptTemplate?.template ?: "你是一个有用的AI助手"
        messageList.add(
            mapOf(
                "role" to "system",
                "content" to systemPrompt
            )
        )
        
        // 用户消息
        messageList.addAll(messages.map { message ->
            mapOf(
                "role" to message.role.name.lowercase(),
                "content" to message.content
            )
        })
        
        val requestData = mapOf(
            "model" to config.model.modelId,
            "messages" to messageList,
            "max_tokens" to config.maxTokens,
            "temperature" to config.temperature,
            "top_p" to config.topP,
            "stream" to stream
        )
        
        val json = gson.toJson(requestData)
        logI("豆包请求数据: $json")
        return json.toRequestBody(mediaType)
    }
    
    private fun parseChatResponse(responseBody: String?): String {
        if (responseBody.isNullOrEmpty()) {
            logE("response empty")
            return ""
        }
        
        return try {
            logI("response body: $responseBody")
            val response = gson.fromJson(responseBody, DoubaoResponse::class.java)
            logI("Json解析成功: choices数量=${response.choices?.size}")
            
            val content = response.choices?.firstOrNull()?.message?.content
            logI("提取的内容: $content")
            
            if (content.isNullOrEmpty()) {
                logE("响应内容为空，完整响应: $responseBody")
                return "解析响应内容为空"
            }
            
            content
        } catch (e: Exception) {
            logE("解析豆包响应失败: ${e.message}")
            logE("原始响应体: $responseBody")
            e.printStackTrace()
            "解析响应失败: ${e.message}"
        }
    }
    
    private fun parseStreamChunk(data: String): String {
        return try {
            val response = gson.fromJson(data, DoubaoStreamResponse::class.java)
            response.choices?.firstOrNull()?.delta?.content ?: ""
        } catch (e: Exception) {
            logE("解析豆包流式响应失败: ${e.message}")
            ""
        }
    }
    
    // 豆包响应数据类
    data class DoubaoResponse(
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
    
    // 豆包流式响应数据类
    data class DoubaoStreamResponse(
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
