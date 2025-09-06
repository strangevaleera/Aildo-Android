package com.xinqi.utils.llm.vibration

import android.content.Context
import com.xinqi.utils.bt.AildoBluetoothManager
import com.xinqi.utils.log.logI
import com.xinqi.utils.log.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * 对话转震动控制主控制器
 * 将大模型对话回复文本转化为震动控制指令
 */
class ConversationToVibrationController private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: ConversationToVibrationController? = null
        
        fun getInstance(context: Context): ConversationToVibrationController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConversationToVibrationController(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val emotionAnalyzer = EmotionAnalyzer()
    private val intentRecognizer = IntentRecognizer()
    private val parameterGenerator = VibrationParameterGenerator()
    private val bluetoothManager = AildoBluetoothManager.getInstance(context)
    
    private var conversationContext = ConversationContext()
    
    /**
     * 处理对话回复并生成震动控制
     */
    fun processConversationResponse(
        userMessage: String,
        aiResponse: String
    ): VibrationControlParams? {
        try {
            logI("处理对话回复: $aiResponse")
            
            // 1. 更新对话上下文
            updateConversationContext(userMessage, aiResponse)
            
            // 2. 分析AI回复的情感
            val emotion = emotionAnalyzer.analyzeEmotion(aiResponse)
            logI("情感分析结果: ${emotion.emotion}, 强度: ${emotion.intensity}, 置信度: ${emotion.confidence}")
            
            // 3. 识别控制意图
            val intent = intentRecognizer.recognizeIntent(aiResponse, emotion)
            logI("识别意图: $intent")
            
            // 4. 生成震动参数
            val params = parameterGenerator.generateParameters(
                emotion, intent, conversationContext
            )
            logI("生成震动参数: intensity=${params.intensity}, duration=${params.duration}, pattern=${params.pattern?.name}")
            
            // 5. 应用用户偏好
            val adjustedParams = applyUserPreferences(params)
            
            // 6. 发送控制指令
            sendVibrationCommand(adjustedParams)
            
            return adjustedParams
            
        } catch (e: Exception) {
            logE("处理对话回复时出错: ${e.message}")
            return null
        }
    }
    
    /**
     * 更新对话上下文
     */
    private fun updateConversationContext(userMessage: String, aiResponse: String) {
        val userMsg = Message(
            content = userMessage,
            timestamp = System.currentTimeMillis(),
            sender = MessageSender.USER
        )
        
        val aiMsg = Message(
            content = aiResponse,
            timestamp = System.currentTimeMillis(),
            sender = MessageSender.AI,
            emotion = emotionAnalyzer.analyzeEmotion(aiResponse)
        )
        
        conversationContext = conversationContext.copy(
            recentMessages = (conversationContext.recentMessages + userMsg + aiMsg)
                .takeLast(10), // 保留最近10条消息
            currentMood = aiMsg.emotion?.emotion ?: conversationContext.currentMood
        )
    }
    
    /**
     * 应用用户偏好
     */
    private fun applyUserPreferences(params: VibrationControlParams): VibrationControlParams {
        val prefs = conversationContext.userPreferences
        
        return params.copy(
            intensity = (params.intensity * prefs.sensitivity).toInt().coerceIn(0, 100),
            duration = if (prefs.preferredDuration > 0) prefs.preferredDuration else params.duration
        )
    }
    
    /**
     * 发送震动控制指令
     */
    private fun sendVibrationCommand(params: VibrationControlParams) {
        try {
            if (params.pattern != null) {
                // 发送模式震动
                sendPatternVibration(params)
            } else {
                // 发送简单震动
                bluetoothManager.controlVibration(
                    motorId = params.motorId,
                    mode = params.mode,
                    intensity = params.intensity
                )
                logI("发送简单震动: intensity=${params.intensity}")
            }
        } catch (e: Exception) {
            logE("发送震动指令时出错: ${e.message}")
        }
    }
    
    /**
     * 发送模式震动
     */
    private fun sendPatternVibration(params: VibrationControlParams) {
        val pattern = params.pattern!!
        
        // 异步执行模式震动
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logI("开始执行震动模式: ${pattern.name}")
                
                for (i in pattern.intensitySequence.indices) {
                    bluetoothManager.controlVibration(
                        motorId = params.motorId,
                        mode = params.mode,
                        intensity = pattern.intensitySequence[i]
                    )
                    
                    delay(pattern.durationSequence[i])
                }
                
                // 如果需要重复，递归调用
                if (pattern.repeat && params.duration > 0) {
                    delay(1000) // 间隔1秒
                    sendPatternVibration(params)
                }
                
                logI("震动模式执行完成")
                
            } catch (e: Exception) {
                logE("执行震动模式时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 设置用户偏好
     */
    fun setUserPreferences(preferences: UserPreferences) {
        conversationContext = conversationContext.copy(userPreferences = preferences)
        logI("用户偏好已更新: sensitivity=${preferences.sensitivity}")
    }
    
    /**
     * 获取当前对话上下文
     */
    fun getConversationContext(): ConversationContext = conversationContext
    
    /**
     * 重置对话上下文
     */
    fun resetConversationContext() {
        conversationContext = ConversationContext()
        logI("对话上下文已重置")
    }
}

/**
 * 情感分析器
 */
class EmotionAnalyzer {
    
    private val emotionKeywords = mapOf(
        EmotionType.HAPPY to listOf("开心", "高兴", "快乐", "兴奋", "激动", "愉快", "欢乐", "喜悦"),
        EmotionType.EXCITED to listOf("兴奋", "激动", "刺激", "狂热", "亢奋", "振奋", "激昂"),
        EmotionType.LOVING to listOf("爱", "喜欢", "爱意", "温柔", "甜蜜", "宠爱", "疼爱", "怜爱"),
        EmotionType.PLAYFUL to listOf("调皮", "顽皮", "可爱", "有趣", "好玩", "淘气", "俏皮", "活泼"),
        EmotionType.ROMANTIC to listOf("浪漫", "温馨", "甜蜜", "温柔", "爱意", "柔情", "缠绵", "温存"),
        EmotionType.PASSIONATE to listOf("激情", "热烈", "狂热", "强烈", "激烈", "炽热", "狂热", "奔放"),
        EmotionType.SAD to listOf("难过", "伤心", "悲伤", "失落", "沮丧", "忧郁", "哀伤", "痛苦"),
        EmotionType.ANGRY to listOf("生气", "愤怒", "恼火", "烦躁", "不满", "愤慨", "暴怒", "狂怒"),
        EmotionType.CALM to listOf("平静", "安静", "温和", "轻柔", "舒缓", "宁静", "安详", "平和")
    )
    
    private val intensityKeywords = mapOf(
        EmotionIntensity.VERY_LOW to listOf("轻微", "一点点", "轻柔", "温和", "舒缓"),
        EmotionIntensity.LOW to listOf("温柔", "温和", "轻柔", "舒缓", "平静"),
        EmotionIntensity.MEDIUM to listOf("适中", "中等", "一般", "正常", "普通"),
        EmotionIntensity.HIGH to listOf("强烈", "激烈", "热烈", "狂热", "刺激"),
        EmotionIntensity.VERY_HIGH to listOf("非常", "极其", "超级", "极度", "疯狂")
    )
    
    fun analyzeEmotion(text: String): EmotionAnalysisResult {
        val keywords = extractKeywords(text)
        val emotionScores = calculateEmotionScores(keywords)
        val intensity = calculateIntensity(text, emotionScores)
        val confidence = calculateConfidence(emotionScores)
        
        return EmotionAnalysisResult(
            emotion = getDominantEmotion(emotionScores),
            intensity = intensity,
            confidence = confidence,
            keywords = keywords
        )
    }
    
    private fun extractKeywords(text: String): List<String> {
        return emotionKeywords.flatMap { (_, keywords) ->
            keywords.filter { text.contains(it) }
        }
    }
    
    private fun calculateEmotionScores(keywords: List<String>): Map<EmotionType, Float> {
        val scores = mutableMapOf<EmotionType, Float>()
        
        for ((emotion, emotionKeywords) in emotionKeywords) {
            val score = emotionKeywords.count { keywords.contains(it) }.toFloat()
            if (score > 0) {
                scores[emotion] = score
            }
        }
        
        return scores
    }
    
    private fun calculateIntensity(text: String, emotionScores: Map<EmotionType, Float>): EmotionIntensity {
        for ((intensity, keywords) in intensityKeywords) {
            if (keywords.any { text.contains(it) }) {
                return intensity
            }
        }
        
        // 根据情感类型推断强度
        val dominantEmotion = getDominantEmotion(emotionScores)
        return when (dominantEmotion) {
            EmotionType.PASSIONATE, EmotionType.EXCITED -> EmotionIntensity.HIGH
            EmotionType.CALM, EmotionType.LOVING -> EmotionIntensity.LOW
            else -> EmotionIntensity.MEDIUM
        }
    }
    
    private fun calculateConfidence(emotionScores: Map<EmotionType, Float>): Float {
        if (emotionScores.isEmpty()) return 0.0f
        
        val maxScore = emotionScores.values.maxOrNull() ?: 0.0f
        val totalScore = emotionScores.values.sum()
        
        return if (totalScore > 0) maxScore / totalScore else 0.0f
    }
    
    private fun getDominantEmotion(emotionScores: Map<EmotionType, Float>): EmotionType {
        return emotionScores.maxByOrNull { it.value }?.key ?: EmotionType.NEUTRAL
    }
}

/**
 * 意图识别器
 */
class IntentRecognizer {
    
    private val intentPatterns = mapOf(
        VibrationIntent.START_VIBRATION to listOf(
            "开始", "启动", "开启", "打开", "震动", "振动", "启动", "开始吧"
        ),
        VibrationIntent.STOP_VIBRATION to listOf(
            "停止", "关闭", "结束", "暂停", "停下", "停止吧", "结束吧"
        ),
        VibrationIntent.INCREASE_INTENSITY to listOf(
            "加强", "增加", "提高", "更强烈", "更刺激", "强烈一点", "加强一些"
        ),
        VibrationIntent.DECREASE_INTENSITY to listOf(
            "减弱", "减少", "降低", "轻柔", "温和", "温柔一点", "轻柔一些"
        ),
        VibrationIntent.CHANGE_PATTERN to listOf(
            "换", "改变", "切换", "模式", "方式", "换个", "换一种"
        ),
        VibrationIntent.RANDOM_PATTERN to listOf(
            "随机", "随意", "随便", "任意", "自由", "随你", "任意模式"
        ),
        VibrationIntent.RHYTHMIC to listOf(
            "节拍", "节奏", "律动", "韵律", "节律", "有节奏", "节拍感"
        ),
        VibrationIntent.WAVE to listOf(
            "波浪", "起伏", "波动", "荡漾", "涟漪", "波浪式", "起伏感"
        ),
        VibrationIntent.PULSE to listOf(
            "脉冲", "跳动", "搏动", "脉动", "心跳", "脉冲式", "跳动感"
        ),
        VibrationIntent.ESCALATION to listOf(
            "渐进", "逐渐", "慢慢", "逐步", "递增", "渐进式", "慢慢加强"
        )
    )
    
    fun recognizeIntent(text: String, emotion: EmotionAnalysisResult): VibrationIntent {
        // 基于关键词匹配识别意图
        for ((intent, patterns) in intentPatterns) {
            if (patterns.any { text.contains(it) }) {
                return intent
            }
        }
        
        // 基于情感推断意图
        return inferIntentFromEmotion(emotion)
    }
    
    private fun inferIntentFromEmotion(emotion: EmotionAnalysisResult): VibrationIntent {
        return when (emotion.emotion) {
            EmotionType.EXCITED, EmotionType.PASSIONATE -> VibrationIntent.INCREASE_INTENSITY
            EmotionType.CALM, EmotionType.LOVING -> VibrationIntent.DECREASE_INTENSITY
            EmotionType.PLAYFUL -> VibrationIntent.RANDOM_PATTERN
            EmotionType.ROMANTIC -> VibrationIntent.WAVE
            else -> VibrationIntent.START_VIBRATION
        }
    }
}

/**
 * 震动参数生成器
 */
class VibrationParameterGenerator {
    
    fun generateParameters(
        emotion: EmotionAnalysisResult,
        intent: VibrationIntent,
        context: ConversationContext
    ): VibrationControlParams {
        
        val baseIntensity = calculateBaseIntensity(emotion)
        val duration = calculateDuration(emotion, intent)
        val pattern = generatePattern(emotion, intent)
        
        return VibrationControlParams(
            motorId = 0,
            mode = getModeFromIntent(intent),
            intensity = baseIntensity,
            duration = duration,
            pattern = pattern,
            escalation = shouldEscalate(emotion, intent),
            random = intent == VibrationIntent.RANDOM_PATTERN
        )
    }
    
    private fun calculateBaseIntensity(emotion: EmotionAnalysisResult): Int {
        val baseIntensity = when (emotion.intensity) {
            EmotionIntensity.VERY_LOW -> 20
            EmotionIntensity.LOW -> 35
            EmotionIntensity.MEDIUM -> 50
            EmotionIntensity.HIGH -> 70
            EmotionIntensity.VERY_HIGH -> 90
        }
        
        // 根据情感类型调整
        val emotionMultiplier = when (emotion.emotion) {
            EmotionType.EXCITED, EmotionType.PASSIONATE -> 1.2f
            EmotionType.CALM, EmotionType.LOVING -> 0.8f
            EmotionType.PLAYFUL -> 1.1f
            EmotionType.ROMANTIC -> 0.9f
            else -> 1.0f
        }
        
        return (baseIntensity * emotionMultiplier).toInt().coerceIn(0, 100)
    }
    
    private fun calculateDuration(emotion: EmotionAnalysisResult, intent: VibrationIntent): Long {
        val baseDuration = when (emotion.intensity) {
            EmotionIntensity.VERY_LOW -> 2000L
            EmotionIntensity.LOW -> 3000L
            EmotionIntensity.MEDIUM -> 5000L
            EmotionIntensity.HIGH -> 8000L
            EmotionIntensity.VERY_HIGH -> 12000L
        }
        
        return when (intent) {
            VibrationIntent.SHORT_BURST -> 1000L
            VibrationIntent.EXTEND_DURATION -> baseDuration * 2
            else -> baseDuration
        }
    }
    
    private fun generatePattern(emotion: EmotionAnalysisResult, intent: VibrationIntent): VibrationPattern? {
        return when (intent) {
            VibrationIntent.RHYTHMIC -> VibrationPattern(
                name = "节拍模式",
                intensitySequence = listOf(30, 60, 30, 60, 30, 60),
                durationSequence = listOf(500L, 500L, 500L, 500L, 500L, 500L),
                repeat = true
            )
            VibrationIntent.WAVE -> VibrationPattern(
                name = "波浪模式",
                intensitySequence = listOf(20, 40, 60, 80, 60, 40, 20),
                durationSequence = listOf(800L, 800L, 800L, 800L, 800L, 800L, 800L),
                repeat = true
            )
            VibrationIntent.PULSE -> VibrationPattern(
                name = "脉冲模式",
                intensitySequence = listOf(0, 80, 0, 80, 0, 80),
                durationSequence = listOf(200L, 300L, 200L, 300L, 200L, 300L),
                repeat = true
            )
            else -> null
        }
    }
    
    private fun getModeFromIntent(intent: VibrationIntent): Int {
        return when (intent) {
            VibrationIntent.START_VIBRATION -> 1
            VibrationIntent.STOP_VIBRATION -> 0
            VibrationIntent.INCREASE_INTENSITY -> 2
            VibrationIntent.DECREASE_INTENSITY -> 3
            VibrationIntent.CHANGE_PATTERN -> 4
            VibrationIntent.RANDOM_PATTERN -> 5
            VibrationIntent.RHYTHMIC -> 6
            VibrationIntent.WAVE -> 7
            VibrationIntent.PULSE -> 8
            VibrationIntent.ESCALATION -> 9
            else -> 1
        }
    }
    
    private fun shouldEscalate(emotion: EmotionAnalysisResult, intent: VibrationIntent): Boolean {
        return intent == VibrationIntent.ESCALATION || 
               (emotion.emotion == EmotionType.PASSIONATE && emotion.intensity == EmotionIntensity.HIGH)
    }
}

