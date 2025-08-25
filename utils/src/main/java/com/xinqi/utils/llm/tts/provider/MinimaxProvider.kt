package com.xinqi.utils.llm.tts.provider

import android.content.Context
import com.xinqi.utils.common.ConfigManager
import com.xinqi.utils.llm.tts.TTSProvider
import com.xinqi.utils.llm.tts.config.ModelConfig
import com.xinqi.utils.llm.tts.model.TTSConfig
import com.xinqi.utils.llm.tts.config.VoiceConfig
import com.xinqi.utils.llm.tts.model.VoiceInfo
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class MinimaxProvider(
    private val context: Context,
    private val config: TTSConfig
) : TTSProvider {
    
    companion object {
        private const val TAG = "MinimaxProvider"
        private const val BASE_URL = "https://api.minimaxi.com"
        private const val TTS_ENDPOINT = "/v1/t2a_v2"
        
        // 支持的音频格式
        private val SUPPORTED_FORMATS = listOf("mp3", "wav", "flac")
    }

    /**
     * 语音设置配置
     */
    data class VoiceSetting(
        val voice_id: String,
        val speed: Float,
        val vol: Float,
        val pitch: Float,
        val emotion: String
    )

    /**
     * 音频设置配置
     */
    data class AudioSetting(
        val sample_rate: Int,
        val bitrate: Int,
        val format: String,
        val channel: Int
    )

    /**
     * 发音字典配置
     */
    data class PronunciationDict(
        val tone: List<String>
    )
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(config.timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(config.timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
        .writeTimeout(config.timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()
    
    override suspend fun textToSpeech(
        text: String,
        voice: String,
        speed: Float,
        pitch: Float,
        volume: Float,
        emotion: String?
    ): File? = withContext(Dispatchers.IO) {
        try {
            logI(TAG, "开始TTS转换: text=$text, voice=$voice, speed=$speed, pitch=$pitch, volume=$volume")
            
            // 构建请求参数
            val requestBody = buildTTSRequestBody(text, voice, speed, pitch, volume, emotion)
            // 构建请求
            val request = Request.Builder()
                .url("$BASE_URL$TTS_ENDPOINT?GroupId=${config.groupId}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", generateAuthHeader())
                .post(requestBody)
                .build()
            
            // 执行请求
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logE(TAG + "TTS请求失败: ${response.code} - ${response.message}")
                    return@withContext null
                }
                
                val responseBody = response.body?.string()
                logI(TAG, "TTS响应: $responseBody")
                
                // 解析响应
                val audioData = parseTTSResponse(responseBody)
                if (audioData != null) {
                    // 保存音频文件
                    val audioFile = saveAudioFile(audioData, text)
                    logI(TAG, "TTS转换成功，保存到: ${audioFile.absolutePath}")
                    return@withContext audioFile
                }
            }
            
            logE(TAG + "TTS转换失败")
            return@withContext null
            
        } catch (e: Exception) {
            logE(TAG + "TTS转换异常", e)
            return@withContext null
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
        //todo: MINIMAX流式接入：https://platform.minimaxi.com/document/%E5%90%8C%E6%AD%A5%E8%AF%AD%E9%9F%B3%E5%90%88%E6%88%90?key=66719005a427f0c8a5701643
    }
    
    override suspend fun getAvailableVoices(): List<VoiceInfo> = withContext(Dispatchers.IO) {
        try {
            logI(TAG, "获取可用语音列表")
            // 从配置文件获取Minimax模型支持的音色
            return@withContext VoiceConfig.getVoicesForModel(ModelConfig.MODEL_ID_MINIMAX_SPEECH)
        } catch (e: Exception) {
            logE(TAG + "获取语音列表异常" , e)
            return@withContext emptyList()
        }
    }
    
    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val apiKey = ConfigManager.getTTSMinimaxApiKey()
            val groupId = ConfigManager.getTTSMinimaxGroupId()
            
            if (apiKey.isBlank() || groupId.isBlank()) {
                logI(TAG, "Minimax配置不完整")
                return@withContext false
            }
            
            val testRequest = buildTTSRequestBody("测试", "male-qn-qingse", 1.0f, 1.0f, 1.0f, "happy")
            val request = Request.Builder()
                .url("$BASE_URL$TTS_ENDPOINT")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", generateAuthHeader())
                .post(testRequest)
                .build()
            
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
            
        } catch (e: Exception) {
            logE(TAG + "检查服务可用性异常", e)
            return@withContext false
        }
    }
    
    override fun getModelInfo(): String {
        return "Minimax TTS - 支持中文语音合成，提供多种音色选择"
    }
    
    override fun getSupportedFormats(): List<String> {
        return SUPPORTED_FORMATS
    }

    /**
     * 构建TTS请求体
     */
    private fun buildTTSRequestBody(
        text: String,
        voice: String,
        speed: Float,
        pitch: Float,
        volume: Float,
        emotion: String?
    ): RequestBody {
        val voiceSetting = VoiceSetting(
            voice_id = voice,
            speed = speed,
            vol = volume,
            pitch = pitch,
            emotion = emotion?: "angry" //参数范围["happy", "sad", "angry", "fearful", "disgusted", "surprised", "neutral"]
        )

        val audioSetting = AudioSetting(
            sample_rate = 32000,
            bitrate = 128000,
            format = "mp3",
            channel = 1
        )

        val pronunciationDict = PronunciationDict(
            tone = listOf("处理/(chu3)(li3)", "危险/dangerous")
        )

        val jsonObject = JSONObject().apply {
            put("model", config.model.modelId)
            put("text", text)
            put("stream", false)
            put("voice_setting", JSONObject().apply {
                put("voice_id", voiceSetting.voice_id)
                put("speed", voiceSetting.speed)
                put("vol", voiceSetting.vol)
                put("pitch", voiceSetting.pitch)
                put("emotion", voiceSetting.emotion)
            })
            put("audio_setting", JSONObject().apply {
                put("sample_rate", audioSetting.sample_rate)
                put("bitrate", audioSetting.bitrate)
                put("format", audioSetting.format)
                put("channel", audioSetting.channel)
            })
            put("pronunciation_dict", JSONObject().apply {
                put("tone", JSONArray(pronunciationDict.tone))
            })
        }

        return jsonObject.toString().toRequestBody("application/json".toMediaType())
    }
    
    /**
     * 生成认证头
     */
    private fun generateAuthHeader(): String {
        val apiKey = config.apiKey.ifBlank { ConfigManager.getTTSMinimaxApiKey() }

        // Minimax使用简单的Bearer Token认证
        return "Bearer $apiKey"
    }
    
    /**
     * 解析TTS响应
     */
    private fun parseTTSResponse(responseBody: String?): ByteArray? {
        if (responseBody.isNullOrBlank()) return null
        
        return try {
            val jsonObject = JSONObject(responseBody)
            
            // 检查是否有错误
            if (jsonObject.has("error")) {
                val error = jsonObject.getJSONObject("error")
                logE(TAG + "TTS API错误: ${error.optString("message", "未知错误")}")
                return null
            }
            
            // 获取音频数据
            val audioData = jsonObject.optString("audio_data", "")
            if (audioData.isNotBlank()) {
                // 如果是Base64编码的音频数据
                android.util.Base64.decode(audioData, android.util.Base64.DEFAULT)
            } else {
                // 如果是二进制数据，直接返回
                responseBody.toByteArray()
            }
            
        } catch (e: Exception) {
            logE(TAG + "解析TTS响应异常", e)
            null
        }
    }
    
    /**
     * 保存音频文件
     */
    private fun saveAudioFile(audioData: ByteArray, text: String): File {
        val fileName = "minimax_tts_${System.currentTimeMillis()}.${config.outputFormat}"
        val audioFile = File(context.cacheDir, fileName)
        
        FileOutputStream(audioFile).use { fos ->
            fos.write(audioData)
            fos.flush()
        }
        
        return audioFile
    }
}