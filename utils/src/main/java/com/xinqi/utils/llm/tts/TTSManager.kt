package com.xinqi.utils.llm.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * TTS（文本转语音）管理器
 * 1. 支持多种TTS服务
 * 2. 本地音频播放
 */
class TTSManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "TTSManager"
        private const val SAMPLE_RATE = 22050
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private var BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        
        @Volatile
        private var INSTANCE: TTSManager? = null
        
        fun getInstance(context: Context): TTSManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TTSManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var playingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val eventListeners = mutableListOf<TTSEventListener>()
    
    /**
     * TTS事件监听器接口
     */
    interface TTSEventListener {
        fun onTTSStarted()
        fun onTTSCompleted()
        fun onTTSStopped()
        fun onTTSProgress(progress: Float)
        fun onError(error: String)
    }
    
    fun addEventListener(listener: TTSEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }
    
    fun removeEventListener(listener: TTSEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * 文本转语音
     */
    fun textToSpeech(
        text: String,
        voice: String = "zh-CN-XiaoxiaoNeural",
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        onComplete: (File?) -> Unit
    ) {
        scope.launch {
            try {
                logI("开始文本转语音: $text")
                notifyTTSStarted()
                
                // 这里可以集成具体的TTS服务
                // 例如：百度TTS、讯飞TTS、阿里云TTS、Azure TTS等
                val audioFile = performTTS(text, voice, speed, pitch)
                
                if (audioFile != null) {
                    logI("TTS完成，音频文件: ${audioFile.absolutePath}")
                    notifyTTSCompleted()
                    onComplete(audioFile)
                } else {
                    logE("TTS失败")
                    notifyError("TTS转换失败")
                    onComplete(null)
                }
                
            } catch (e: Exception) {
                logE("TTS异常: ${e.message}")
                notifyError("TTS异常: ${e.message}")
                onComplete(null)
            }
        }
    }
    
    /**
     * TTS转换
     */
    private suspend fun performTTS(
        text: String, 
        voice: String, 
        speed: Float, 
        pitch: Float
    ): File? {
        // TODO: 集成具体的TTS服务
        // 例如：
        // - 百度语音合成
        // - 讯飞语音合成
        // - 阿里云语音合成
        // - Azure语音合成
        // - Google TTS

        // 创建模拟音频文件
        return createMockAudioFile(text)
    }

    private fun createMockAudioFile(text: String): File? {
        return try {
            val fileName = "tts_${System.currentTimeMillis()}.wav"
            val file = File(context.cacheDir, fileName)
            
            // 创建简单的WAV文件头
            val sampleRate = SAMPLE_RATE
            val duration = 2 // 2秒
            val numSamples = sampleRate * duration
            val dataSize = numSamples * 2 // 16位 = 2字节
            
            val header = createWavHeader(sampleRate, dataSize)
            val audioData = createMockAudioData(numSamples)
            
            FileOutputStream(file).use { fos ->
                fos.write(header)
                fos.write(audioData)
            }
            
            logI("模拟音频文件已创建: ${file.absolutePath}")
            file
            
        } catch (e: IOException) {
            logE("创建模拟音频文件失败: ${e.message}")
            null
        }
    }
    
    /**
     * 创建WAV文件头
     */
    private fun createWavHeader(sampleRate: Int, dataSize: Int): ByteArray {
        val header = ByteArray(44)
        var offset = 0
        
        // RIFF头
        "RIFF".toByteArray().copyInto(header, offset)
        offset += 4
        
        // 文件大小
        val fileSize = dataSize + 36
        writeLittleEndianInt(header, offset, fileSize)
        offset += 4
        
        // WAVE标识
        "WAVE".toByteArray().copyInto(header, offset)
        offset += 4
        
        // fmt子块
        "fmt ".toByteArray().copyInto(header, offset)
        offset += 4
        
        // 子块大小
        writeLittleEndianInt(header, offset, 16)
        offset += 4
        
        // 音频格式 (PCM = 1)
        writeLittleEndianShort(header, offset, 1)
        offset += 2
        
        // 声道数 (单声道 = 1)
        writeLittleEndianShort(header, offset, 1)
        offset += 2
        
        // 采样率
        writeLittleEndianInt(header, offset, sampleRate)
        offset += 4
        
        // 字节率
        val byteRate = sampleRate * 2 // 单声道16位
        writeLittleEndianInt(header, offset, byteRate)
        offset += 4
        
        // 块对齐
        writeLittleEndianShort(header, offset, 2)
        offset += 2
        
        // 位深度
        writeLittleEndianShort(header, offset, 16)
        offset += 2
        
        // data子块
        "data".toByteArray().copyInto(header, offset)
        offset += 4
        
        // 数据大小
        writeLittleEndianInt(header, offset, dataSize)
        
        return header
    }
    
    /**
     * 创建模拟音频数据
     */
    private fun createMockAudioData(numSamples: Int): ByteArray {
        val audioData = ByteArray(numSamples * 2)
        var offset = 0
        
        for (i in 0 until numSamples) {
            val frequency = 440.0
            val amplitude = 0.3
            val sample = (amplitude * Math.sin(2 * Math.PI * frequency * i / SAMPLE_RATE) * 32767).toInt()
            
            // 写入16位小端序
            audioData[offset++] = (sample and 0xFF).toByte()
            audioData[offset++] = (sample shr 8 and 0xFF).toByte()
        }
        
        return audioData
    }
    
    /**
     * 写入小端序整数
     */
    private fun writeLittleEndianInt(array: ByteArray, offset: Int, value: Int) {
        array[offset] = (value and 0xFF).toByte()
        array[offset + 1] = (value shr 8 and 0xFF).toByte()
        array[offset + 2] = (value shr 16 and 0xFF).toByte()
        array[offset + 3] = (value shr 24 and 0xFF).toByte()
    }
    
    /**
     * 写入小端序短整数
     */
    private fun writeLittleEndianShort(array: ByteArray, offset: Int, value: Int) {
        array[offset] = (value and 0xFF).toByte()
        array[offset + 1] = (value shr 8 and 0xFF).toByte()
    }
    
    /**
     * 播放音频文件
     */
    fun playAudio(audioFile: File) {
        if (isPlaying) {
            logI("音频正在播放中")
            return
        }
        
        try {
            val audioAttributes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            } else {
                null
            }
            
            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes!!)
                    .setAudioFormat(AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .setEncoding(AUDIO_FORMAT)
                        .build())
                    .setBufferSizeInBytes(BUFFER_SIZE)
                    .build()
            } else {
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE,
                    AudioTrack.MODE_STREAM
                )
            }
            
            isPlaying = true
            notifyTTSStarted()
            
            playingJob = scope.launch {
                try {
                    audioTrack?.play()
                    
                    val audioData = audioFile.readBytes()
                    val headerSize = 44 // WAV文件头大小
                    val actualAudioData = audioData.copyOfRange(headerSize, audioData.size)
                    
                    audioTrack?.write(actualAudioData, 0, actualAudioData.size)
                    
                    audioTrack?.stop()
                    audioTrack?.release()
                    audioTrack = null
                    
                    isPlaying = false
                    notifyTTSCompleted()
                    
                } catch (e: Exception) {
                    logE("播放音频失败: ${e.message}")
                    isPlaying = false
                    notifyError("播放音频失败: ${e.message}")
                }
            }
            
            logI("开始播放音频: ${audioFile.absolutePath}")
            
        } catch (e: Exception) {
            logE("初始化音频播放失败: ${e.message}")
            notifyError("初始化音频播放失败: ${e.message}")
        }
    }

    fun stopPlayback() {
        if (!isPlaying) return
        
        try {
            playingJob?.cancel()
            
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            
            isPlaying = false
            notifyTTSStopped()
            logI("音频播放已停止")
            
        } catch (e: Exception) {
            logE("停止播放失败: ${e.message}")
        }
    }

    fun isPlaying(): Boolean = isPlaying
    
    private fun notifyTTSStarted() {
        eventListeners.forEach { it.onTTSStarted() }
    }
    
    private fun notifyTTSCompleted() {
        eventListeners.forEach { it.onTTSCompleted() }
    }
    
    private fun notifyTTSStopped() {
        eventListeners.forEach { it.onTTSStopped() }
    }
    
    private fun notifyTTSProgress(progress: Float) {
        eventListeners.forEach { it.onTTSProgress(progress) }
    }
    
    private fun notifyError(error: String) {
        eventListeners.forEach { it.onError(error) }
    }
}
