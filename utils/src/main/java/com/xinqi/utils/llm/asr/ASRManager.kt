package com.xinqi.utils.llm.asr

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.xinqi.utils.log.logE
import com.xinqi.utils.log.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ASRManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ASRManager"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private var BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        
        @Volatile
        private var INSTANCE: ASRManager? = null
        
        fun getInstance(context: Context): ASRManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ASRManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val eventListeners = mutableListOf<ASREventListener>()
    
    /**
     * ASR事件监听器接口
     */
    interface ASREventListener {
        fun onRecordingStarted()
        fun onRecordingStopped()
        fun onAudioDataReceived(audioData: ByteArray)
        fun onRecognitionResult(result: String, confidence: Float)
        fun onError(error: String)
    }
    
    fun addEventListener(listener: ASREventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }
    
    fun removeEventListener(listener: ASREventListener) {
        eventListeners.remove(listener)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(): Boolean {
        if (isRecording) {
            logI("录音已在进行中")
            return false
        }
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                logE("AudioRecord初始化失败")
                notifyError("AudioRecord初始化失败")
                return false
            }
            
            isRecording = true
            notifyRecordingStarted()
            
            recordingJob = scope.launch {
                val buffer = ByteArray(BUFFER_SIZE)
                audioRecord?.startRecording()
                
                while (isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                    if (readSize > 0) {
                        val audioData = buffer.copyOf(readSize)
                        notifyAudioDataReceived(audioData)
                    }
                }
            }
            
            logI("开始录音")
            return true
            
        } catch (e: Exception) {
            logE("启动录音失败: ${e.message}")
            notifyError("启动录音失败: ${e.message}")
            return false
        }
    }
    
    /**
     * 停止录音
     */
    fun stopRecording() {
        if (!isRecording) return
        
        try {
            isRecording = false
            recordingJob?.cancel()
            
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            notifyRecordingStopped()
            logI("停止录音")
            
        } catch (e: Exception) {
            logE("停止录音失败: ${e.message}")
        }
    }
    
    /**
     * 保存音频数据到文件
     */
    fun saveAudioToFile(audioData: ByteArray, fileName: String): File? {
        return try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(audioData)
            }
            logI("音频已保存到: ${file.absolutePath}")
            file
        } catch (e: IOException) {
            logE("保存音频文件失败: ${e.message}")
            null
        }
    }
    
    /**
     * 从音频文件识别语音
     */
    fun recognizeFromFile(audioFile: File, callback: (String, Float) -> Unit) {
        scope.launch {
            try {
                // 这里可以集成具体的语音识别服务
                // 例如：百度语音、讯飞语音、阿里云语音等
                val result = performRecognition(audioFile)
                mainHandler.post {
                    callback(result.first, result.second)
                }
            } catch (e: Exception) {
                logE("文件识别失败: ${e.message}")
                mainHandler.post {
                    callback("", 0.0f)
                }
            }
        }
    }
    
    /**
     * 执行语音识别
     * 这里需要集成具体的语音识别服务
     */
    private suspend fun performRecognition(audioFile: File): Pair<String, Float> {
        // TODO: 集成具体的语音识别服务
        // 例如：
        // - 百度语音识别
        // - 讯飞语音识别
        // - 阿里云语音识别
        // - 腾讯云语音识别
        
        // 模拟识别结果
        // delay(1000) // This line was commented out in the original file, so it's commented out here.
        return Pair("这是模拟的语音识别结果", 0.85f)
    }
    
    /**
     * 实时语音识别
     */
    fun startRealTimeRecognition() {
        // TODO: 实现实时语音识别
        // 可以集成WebSocket或其他实时通信方式
        logI("开始实时语音识别")
    }
    
    /**
     * 停止实时语音识别
     */
    fun stopRealTimeRecognition() {
        // TODO: 停止实时语音识别
        logI("停止实时语音识别")
    }
    
    /**
     * 检查录音权限
     */
    fun checkRecordingPermission(): Boolean {
        // TODO: 检查录音权限
        return true
    }
    
    /**
     * 获取录音状态
     */
    fun isRecording(): Boolean = isRecording
    
    // 通知方法
    private fun notifyRecordingStarted() {
        eventListeners.forEach { it.onRecordingStarted() }
    }
    
    private fun notifyRecordingStopped() {
        eventListeners.forEach { it.onRecordingStopped() }
    }
    
    private fun notifyAudioDataReceived(audioData: ByteArray) {
        eventListeners.forEach { it.onAudioDataReceived(audioData) }
    }
    
    private fun notifyRecognitionResult(result: String, confidence: Float) {
        eventListeners.forEach { it.onRecognitionResult(result, confidence) }
    }
    
    private fun notifyError(error: String) {
        eventListeners.forEach { it.onError(error) }
    }
}
