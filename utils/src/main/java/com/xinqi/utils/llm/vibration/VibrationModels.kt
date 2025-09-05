package com.xinqi.utils.llm.vibration

/**
 * 情感强度枚举
 */
enum class EmotionIntensity {
    VERY_LOW(0.1f),    // 轻微
    LOW(0.3f),         // 低
    MEDIUM(0.5f),      // 中等
    HIGH(0.7f),        // 高
    VERY_HIGH(0.9f);   // 极高
    
    val value: Float
    
    constructor(value: Float) {
        this.value = value
    }
}

/**
 * 情感类型枚举
 */
enum class EmotionType {
    // 正面情感
    HAPPY,           // 开心
    EXCITED,         // 兴奋
    LOVING,          // 爱意
    PLAYFUL,         // 调皮
    ROMANTIC,        // 浪漫
    PASSIONATE,      // 激情
    
    // 负面情感
    SAD,             // 悲伤
    ANGRY,           // 愤怒
    FRUSTRATED,      // 沮丧
    LONELY,          // 孤独
    
    // 中性情感
    CALM,            // 平静
    NEUTRAL,         // 中性
    CURIOUS,         // 好奇
    SURPRISED        // 惊讶
}

/**
 * 震动控制意图枚举
 */
enum class VibrationIntent {
    // 基础控制
    START_VIBRATION,     // 开始震动
    STOP_VIBRATION,      // 停止震动
    PAUSE_VIBRATION,     // 暂停震动
    
    // 强度控制
    INCREASE_INTENSITY,  // 增加强度
    DECREASE_INTENSITY,  // 减少强度
    SET_INTENSITY,       // 设置强度
    
    // 模式控制
    CHANGE_PATTERN,      // 改变模式
    RANDOM_PATTERN,      // 随机模式
    CUSTOM_PATTERN,      // 自定义模式
    
    // 时间控制
    SET_DURATION,        // 设置持续时间
    EXTEND_DURATION,     // 延长持续时间
    SHORT_BURST,         // 短促爆发
    
    // 特殊效果
    RHYTHMIC,           // 节拍模式
    WAVE,               // 波浪模式
    PULSE,              // 脉冲模式
    ESCALATION          // 渐进模式
}

/**
 * 情感分析结果
 */
data class EmotionAnalysisResult(
    val emotion: EmotionType,
    val intensity: EmotionIntensity,
    val confidence: Float,
    val keywords: List<String>
)

/**
 * 震动控制参数
 */
data class VibrationControlParams(
    val motorId: Int = 0,                    // 马达ID
    val mode: Int = 1,                       // 震动模式
    val intensity: Int = 50,                 // 强度 (0-100)
    val duration: Long = 5000L,              // 持续时间(毫秒)
    val pattern: VibrationPattern? = null,   // 震动模式
    val escalation: Boolean = false,         // 是否渐进
    val random: Boolean = false              // 是否随机
)

/**
 * 震动模式
 */
data class VibrationPattern(
    val name: String,                        // 模式名称
    val intensitySequence: List<Int>,        // 强度序列
    val durationSequence: List<Long>,        // 持续时间序列
    val repeat: Boolean = false              // 是否重复
)

/**
 * 对话上下文
 */
data class ConversationContext(
    val recentMessages: List<Message> = emptyList(),
    val currentMood: EmotionType = EmotionType.NEUTRAL,
    val lastVibrationParams: VibrationControlParams? = null,
    val userPreferences: UserPreferences = UserPreferences(),
    val sessionStartTime: Long = System.currentTimeMillis()
)

/**
 * 消息
 */
data class Message(
    val content: String,
    val timestamp: Long,
    val sender: MessageSender,
    val emotion: EmotionAnalysisResult? = null
)

/**
 * 消息发送者
 */
enum class MessageSender {
    USER, AI
}

/**
 * 用户偏好
 */
data class UserPreferences(
    val preferredIntensity: Int = 50,
    val preferredDuration: Long = 5000L,
    val favoritePatterns: List<String> = emptyList(),
    val sensitivity: Float = 1.0f,
    val maxIntensity: Int = 100,
    val maxDuration: Long = 30000L,
    val autoMode: Boolean = true,
    val learningEnabled: Boolean = true
)

/**
 * 用户反馈
 */
enum class UserFeedback {
    TOO_WEAK,       // 太弱
    TOO_STRONG,     // 太强
    TOO_SHORT,      // 太短
    TOO_LONG,       // 太长
    PERFECT,        // 完美
    NOT_GOOD        // 不好
}

/**
 * 环境上下文
 */
enum class EnvironmentalContext {
    QUIET_ENVIRONMENT,   // 安静环境
    NOISY_ENVIRONMENT,   // 嘈杂环境
    PUBLIC_SPACE,        // 公共场所
    PRIVATE_SPACE,       // 私人空间
    UNKNOWN              // 未知
}

/**
 * 震动模式预设
 */
object VibrationPatterns {
    
    val RHYTHMIC_PATTERN = VibrationPattern(
        name = "节拍模式",
        intensitySequence = listOf(30, 60, 30, 60, 30, 60),
        durationSequence = listOf(500L, 500L, 500L, 500L, 500L, 500L),
        repeat = true
    )
    
    val WAVE_PATTERN = VibrationPattern(
        name = "波浪模式",
        intensitySequence = listOf(20, 40, 60, 80, 60, 40, 20),
        durationSequence = listOf(800L, 800L, 800L, 800L, 800L, 800L, 800L),
        repeat = true
    )
    
    val PULSE_PATTERN = VibrationPattern(
        name = "脉冲模式",
        intensitySequence = listOf(0, 80, 0, 80, 0, 80),
        durationSequence = listOf(200L, 300L, 200L, 300L, 200L, 300L),
        repeat = true
    )
    
    val ESCALATION_PATTERN = VibrationPattern(
        name = "渐进模式",
        intensitySequence = listOf(20, 30, 40, 50, 60, 70, 80, 90),
        durationSequence = listOf(1000L, 1000L, 1000L, 1000L, 1000L, 1000L, 1000L, 1000L),
        repeat = false
    )
    
    val GENTLE_PATTERN = VibrationPattern(
        name = "温柔模式",
        intensitySequence = listOf(15, 25, 15, 25, 15, 25),
        durationSequence = listOf(1000L, 1000L, 1000L, 1000L, 1000L, 1000L),
        repeat = true
    )
    
    val INTENSE_PATTERN = VibrationPattern(
        name = "强烈模式",
        intensitySequence = listOf(70, 80, 90, 80, 70, 80, 90, 80),
        durationSequence = listOf(400L, 400L, 400L, 400L, 400L, 400L, 400L, 400L),
        repeat = true
    )
    
    val RANDOM_PATTERN = VibrationPattern(
        name = "随机模式",
        intensitySequence = generateRandomSequence(),
        durationSequence = generateRandomDurationSequence(),
        repeat = true
    )
    
    private fun generateRandomSequence(): List<Int> {
        return (1..8).map { (20..90).random() }
    }
    
    private fun generateRandomDurationSequence(): List<Long> {
        return (1..8).map { (300L..1000L).random() }
    }
}

/**
 * 情感到震动模式的映射
 */
object EmotionToPatternMapper {
    
    private val emotionPatternMap = mapOf(
        EmotionType.HAPPY to VibrationPatterns.RHYTHMIC_PATTERN,
        EmotionType.EXCITED to VibrationPatterns.INTENSE_PATTERN,
        EmotionType.LOVING to VibrationPatterns.GENTLE_PATTERN,
        EmotionType.PLAYFUL to VibrationPatterns.RANDOM_PATTERN,
        EmotionType.ROMANTIC to VibrationPatterns.WAVE_PATTERN,
        EmotionType.PASSIONATE to VibrationPatterns.ESCALATION_PATTERN,
        EmotionType.CALM to VibrationPatterns.GENTLE_PATTERN,
        EmotionType.SAD to VibrationPatterns.GENTLE_PATTERN,
        EmotionType.ANGRY to VibrationPatterns.INTENSE_PATTERN
    )
    
    fun getPatternForEmotion(emotion: EmotionType): VibrationPattern? {
        return emotionPatternMap[emotion]
    }
    
    fun getDefaultPattern(): VibrationPattern {
        return VibrationPatterns.RHYTHMIC_PATTERN
    }
}

/**
 * 震动控制结果
 */
data class VibrationControlResult(
    val success: Boolean,
    val params: VibrationControlParams,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 学习数据
 */
data class LearningData(
    val emotion: EmotionAnalysisResult,
    val intent: VibrationIntent,
    val params: VibrationControlParams,
    val userFeedback: UserFeedback? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 用户学习偏好
 */
data class UserLearningPreferences(
    val learnedIntensity: Float = 1.0f,
    val learnedDuration: Float = 1.0f,
    val preferredEmotions: List<EmotionType> = emptyList(),
    val avoidedEmotions: List<EmotionType> = emptyList(),
    val feedbackHistory: List<LearningData> = emptyList()
)

