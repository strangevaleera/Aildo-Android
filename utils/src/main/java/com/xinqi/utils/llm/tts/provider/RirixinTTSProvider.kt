package com.xinqi.utils.llm.tts.provider

import android.content.Context
import com.google.gson.Gson
import com.xinqi.utils.llm.tts.*
import com.xinqi.utils.llm.tts.auth.JWTTokenGenerator
import com.xinqi.utils.llm.tts.modal.TTSConfig
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * 商汤日日新TTS提供商
 * 使用JWT token鉴权
 * 参考文档: https://www.sensecore.cn/help/docs/model-as-a-service/nova/overview/Authorization
 */
class RirixinTTSProvider(
    private val context: Context,
    private val config: TTSConfig
) : TTSProvider {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .readTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .writeTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .build()
    
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    // 缓存JWT token
    private var cachedToken: String? = null
    
    /**
     * 获取有效的JWT token
     */
    private fun getValidToken(): String {
        // 如果token不存在或即将过期，重新生成
        if (cachedToken == null || JWTTokenGenerator.isTokenExpiringSoon(cachedToken!!)) {
            try {
                cachedToken = JWTTokenGenerator.generateToken(
                    ak = config.ak.ifEmpty { config.apiKey }, // 兼容旧版本
                    sk = config.sk.ifEmpty { config.apiKey }  // 兼容旧版本
                )
                logI("生成新的JWT token")
            } catch (e: Exception) {
                logE("生成JWT token失败: ${e.message}")
                // 如果生成失败，使用原始的apiKey作为fallback
                cachedToken = config.apiKey
            }
        }
        return cachedToken!!
    }
    
    override suspend fun textToSpeech(
        text: String,
        voice: String,
        speed: Float,
        pitch: Float,
        volume: Float
    ): File? {
        return try {
            logI("日日新TTS开始转换: $text")
            
            val requestBody = buildTTSRequest(text, voice, speed, pitch, volume)
            val request = Request.Builder()
                .url(config.baseUrl)
                .addHeader("Authorization", "Bearer ${getValidToken()}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.bytes()
                if (responseBody != null) {
                    val audioFile = saveAudioFile(responseBody, text)
                    logI("日日新TTS转换成功: ${audioFile.absolutePath}")
                    audioFile
                } else {
                    logE("日日新TTS响应体为空")
                    null
                }
            } else {
                val errorBody = response.body?.string()
                logE("日日新TTS请求失败: ${response.code} - ${response.message}, 错误信息: $errorBody")
                null
            }
            
        } catch (e: Exception) {
            logE("日日新TTS异常: ${e.message}")
            null
        }
    }
    
    override fun textToSpeechStream(
        text: String,
        voice: String,
        speed: Float,
        pitch: Float,
        volume: Float,
        onChunk: (ByteArray, Boolean) -> Unit
    ) {
        try {
            val requestBody = buildTTSRequest(text, voice, speed, pitch, volume, stream = true)
            val request = Request.Builder()
                .url("${config.baseUrl}/stream") // 流式接口
                .addHeader("Authorization", "Bearer ${getValidToken()}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            logI("日日新TTS流式转换开始: $text")
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                response.body?.source()?.let { source ->
                    while (true) {
                        val chunk = source.readByteArray(1024)
                        if (chunk.isEmpty()) {
                            onChunk(ByteArray(0), true)
                            break
                        } else {
                            onChunk(chunk, false)
                        }
                    }
                    logI("日日新TTS流式转换完成")
                }
            } else {
                val errorBody = response.body?.string()
                logE("日日新TTS流式请求失败: ${response.code}, 错误信息: $errorBody")
                onChunk(ByteArray(0), true)
            }
            
        } catch (e: Exception) {
            logE("日日新TTS流式转换异常: ${e.message}")
            onChunk(ByteArray(0), true)
        }
    }
    
    override suspend fun getAvailableVoices(): List<VoiceInfo> {
        return getDefaultNova1Voices()
    }

    //todo: move to config file
    fun getDefaultNova1Voices(): List<VoiceInfo> {
        return listOf(
            VoiceInfo("cheerfulvoice-general-male-cn", "通用-中文-男", "中文及中英文混合", "male", "阳光男声"),
            VoiceInfo("sweetvoice-general-female-cn", "通用-中文-女", "中文及中英文混合", "female", "飒爽女声"),
            )
    }

    suspend fun getAvailableVoicesOnline(): List<VoiceInfo> {
        return try {
            val request = Request.Builder()
                .url("${config.baseUrl}/voices")
                .addHeader("Authorization", "Bearer ${getValidToken()}")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseVoicesResponse(responseBody)
            } else {
                val errorBody = response.body?.string()
                logE("获取日日新TTS语音列表失败: ${response.code}, 错误信息: $errorBody")
                emptyList()
            }

        } catch (e: Exception) {
            logE("获取日日新TTS语音列表异常: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun isAvailable(): Boolean {
        return try {
            val request = Request.Builder()
                .url("${config.baseUrl}/health")
                .addHeader("Authorization", "Bearer ${getValidToken()}")
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            logE("检查日日新TTS可用性失败: ${e.message}")
            false
        }
    }
    
    override fun getModelInfo(): String {
        return "日日新TTS模型 (${config.model.modelId})"
    }
    
    override fun getSupportedFormats(): List<String> {
        return listOf("wav", "mp3", "pcm")
    }
    
    private fun buildTTSRequest(
        text: String,
        voice: String,
        speed: Float,
        pitch: Float,
        volume: Float,
        stream: Boolean = false
    ): RequestBody {
        val requestData = mapOf(
            "model" to config.model.modelId,
            "input" to text,
            "voice" to voice,
            "speed" to speed,
            // 根据日日新API文档，暂时不支持pitch和volume
            // "pitch" to pitch,
            // "volume" to volume,
            "response_format" to config.outputFormat,
            // 根据日日新API文档，暂时不支持sample_rate
            // "sample_rate" to config.sampleRate,
            "stream" to stream
        )
        
        val json = gson.toJson(requestData)
        logI("日日新TTS请求数据: $json")
        return json.toRequestBody(mediaType)
    }
    
    private fun saveAudioFile(audioData: ByteArray, text: String): File {
        val fileName = "ririxin_tts_${System.currentTimeMillis()}.${config.outputFormat}"
        val file = File(context.cacheDir, fileName)
        
        FileOutputStream(file).use { fos ->
            fos.write(audioData)
        }
        
        return file
    }
    
    private fun parseVoicesResponse(responseBody: String?): List<VoiceInfo> {
        if (responseBody.isNullOrEmpty()) return emptyList()
        
        return try {
            val response = gson.fromJson(responseBody, RirixinVoicesResponse::class.java)
            response.voices?.map { voice ->
                VoiceInfo(
                    voiceId = voice.voice_id,
                    name = voice.name,
                    language = voice.language,
                    gender = voice.gender,
                    description = voice.description
                )
            } ?: emptyList()
        } catch (e: Exception) {
            logE("解析日日新TTS语音列表失败: ${e.message}")
            emptyList()
        }
    }
    
    data class RirixinVoicesResponse(
        val voices: List<RirixinVoice>?
    )
    
    data class RirixinVoice(
        val voice_id: String,
        val name: String,
        val language: String,
        val gender: String,
        val description: String?
    )
}
