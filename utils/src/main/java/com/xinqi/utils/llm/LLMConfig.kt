package com.xinqi.utils.llm

import com.xinqi.utils.llm.modal.LLMModel

/**
 * LLM配置类
 * 管理各种模型的配置参数
 */
data class LLMConfig(
    val model: LLMModel,
    val apiKey: String,
    val baseUrl: String,
    val maxTokens: Int = 2048,
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val timeout: Long = 30000L, // 30秒超时
    val readWriteTimeout: Long = 5000L, // 5秒超时
    val customHeaders: Map<String, String> = emptyMap()
) {
    companion object {
        fun getDefaultConfig(model: LLMModel): LLMConfig {
            return when (model) {
                LLMModel.DOUBAO -> LLMConfig(
                    model = model,
                    //todo: use config file
                    apiKey = "15ec61d7-f4be-4af1-91c3-bf873a14a06a",
                    baseUrl = "https://ark.cn-beijing.volces.com/api/v3",
                    maxTokens = 4096,
                    temperature = 0.7f
                )
                LLMModel.CHATGPT_3_5, LLMModel.CHATGPT_4, LLMModel.CHATGPT_4_TURBO -> LLMConfig(
                    model = model,
                    apiKey = "",
                    baseUrl = "https://api.openai.com/v1",
                    maxTokens = 4096,
                    temperature = 0.7f
                )
                LLMModel.CLAUDE_3_SONNET, LLMModel.CLAUDE_3_HAIKU -> LLMConfig(
                    model = model,
                    apiKey = "",
                    baseUrl = "https://api.anthropic.com/v1",
                    maxTokens = 4096,
                    temperature = 0.7f
                )
                LLMModel.GEMINI_PRO -> LLMConfig(
                    model = model,
                    apiKey = "",
                    baseUrl = "https://generativelanguage.googleapis.com/v1beta",
                    maxTokens = 8192,
                    temperature = 0.7f
                )
                LLMModel.QWEN_TURBO, LLMModel.QWEN_PLUS -> LLMConfig(
                    model = model,
                    apiKey = "",
                    baseUrl = "https://dashscope.aliyuncs.com/api/v1",
                    maxTokens = 4096,
                    temperature = 0.7f
                )
                LLMModel.SPARK_DESK -> LLMConfig(
                    model = model,
                    apiKey = "",
                    baseUrl = "https://spark-api.xf-yun.com/v3.1",
                    maxTokens = 4096,
                    temperature = 0.7f
                )
                LLMModel.ERNIE_BOT, LLMModel.ERNIE_BOT_TURBO -> LLMConfig(
                    model = model,
                    apiKey = "",
                    baseUrl = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1",
                    maxTokens = 4096,
                    temperature = 0.7f
                )
            }
        }
    }
}
