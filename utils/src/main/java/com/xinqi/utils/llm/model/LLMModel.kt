package com.xinqi.utils.llm.model

/**
 * LLM模型枚举
 * 选中的三方基座大模型
 */
enum class LLMModel(val displayName: String, val modelId: String) {
    DOUBAO("豆包", "doubao-1-5-thinking-pro-250415"),
    CHATGPT_3_5("ChatGPT-3.5", "gpt-3.5-turbo"),
    CHATGPT_4("ChatGPT-4", "gpt-4"),
    CHATGPT_4_TURBO("ChatGPT-4-Turbo", "gpt-4-turbo-preview"),
    CLAUDE_3_SONNET("Claude-3-Sonnet", "claude-3-sonnet-20240229"),
    CLAUDE_3_HAIKU("Claude-3-Haiku", "claude-3-haiku-20240307"),
    GEMINI_PRO("Gemini-Pro", "gemini-pro"),
    QWEN_TURBO("通义千问-Turbo", "qwen-turbo"),
    QWEN_PLUS("通义千问-Plus", "qwen-plus"),
    SPARK_DESK("讯飞星火", "spark-desk"),
    ERNIE_BOT("文心一言", "ernie-bot"),
    ERNIE_BOT_TURBO("文心一言-Turbo", "ernie-bot-turbo");
    
    companion object {
        fun fromModelId(modelId: String): LLMModel? {
            return values().find { it.modelId == modelId }
        }
    }
}
