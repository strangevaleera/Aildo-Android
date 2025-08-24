package com.xinqi.utils.llm.modal

import java.util.UUID

/**
 * 消息角色枚举
 */
enum class MessageRole {
    SYSTEM,     // 系统消息
    USER,       // 用户消息
    ASSISTANT,  // 助手消息
    FUNCTION    // 函数调用消息
}

/**
 * 消息数据类
 */
data class Message(
    val role: MessageRole,
    val content: String,
    val name: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        fun system(content: String, name: String? = null): Message {
            return Message(MessageRole.SYSTEM, content, name)
        }
        
        fun user(content: String, name: String? = null): Message {
            return Message(MessageRole.USER, content, name)
        }
        
        fun assistant(content: String, name: String? = null): Message {
            return Message(MessageRole.ASSISTANT, content, name)
        }
        
        fun function(content: String, name: String): Message {
            return Message(MessageRole.FUNCTION, content, name)
        }
    }
}

/**
 * 对话数据类
 */
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val messages: List<Message> = emptyList(),
    val model: LLMModel? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun addMessage(message: Message): Conversation {
        return copy(
            messages = messages + message,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    fun getLastMessage(): Message? {
        return messages.lastOrNull()
    }
    
    fun getMessageCount(): Int = messages.size
    
    fun clearMessages(): Conversation {
        return copy(
            messages = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
