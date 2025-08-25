package com.xinqi.utils.llm.model

/**
 * Prompt模板管理
 */
data class PromptTemplate(
    val name: String,
    val description: String,
    val template: String,
    val variables: List<String> = emptyList(),
    val category: String = "通用",
    val isDefault: Boolean = false
) {
    /**
     * 替换变量
     */
    fun render(variables: Map<String, String>): String {
        var result = template
        this.variables.forEach { variable ->
            val value = variables[variable] ?: "{{$variable}}"
            result = result.replace("{{$variable}}", value)
        }
        return result
    }
    
    companion object {
        // 默认模板
        val DEFAULT_TEMPLATES = listOf(
            PromptTemplate(
                name = "通用助手",
                description = "通用的AI助手对话模板",
                template = "你是一个有用的AI助手，请根据用户的问题提供准确、有帮助的回答。",
                category = "通用",
                isDefault = true
            ),
            PromptTemplate(
                name = "代码助手",
                description = "专门用于代码相关问题的模板",
                template = "你是一个专业的程序员，擅长多种编程语言。请提供清晰、准确的代码示例和解释。\n\n用户问题：{{question}}\n\n请提供：\n1. 代码示例\n2. 详细解释\n3. 最佳实践建议",
                variables = listOf("question"),
                category = "编程"
            ),
            PromptTemplate(
                name = "翻译助手",
                description = "用于多语言翻译的模板",
                template = "你是一个专业的翻译助手，请将以下内容从{{source_language}}翻译成{{target_language}}：\n\n{{content}}\n\n请提供准确的翻译结果。",
                variables = listOf("source_language", "target_language", "content"),
                category = "翻译"
            ),
            PromptTemplate(
                name = "创意写作",
                description = "用于创意写作和故事创作的模板",
                template = "你是一个富有创造力的作家。请根据以下要求创作内容：\n\n主题：{{topic}}\n风格：{{style}}\n长度：{{length}}\n\n请创作出富有想象力和吸引力的内容。",
                variables = listOf("topic", "style", "length"),
                category = "写作"
            ),
            PromptTemplate(
                name = "学习辅导",
                description = "用于学习和教育辅导的模板",
                template = "你是一个耐心的老师，请帮助用户理解以下概念：\n\n概念：{{concept}}\n\n请提供：\n1. 简单易懂的解释\n2. 实际例子\n3. 相关知识点\n4. 练习题建议",
                variables = listOf("concept"),
                category = "教育"
            ),
            PromptTemplate(
                name = "情感支持",
                description = "用于情感支持和心理安慰的模板",
                template = "你是一个温暖、理解的情感支持者。请以同理心和关怀的态度回应用户的情感需求。\n\n请记住：\n- 保持耐心和理解\n- 提供情感支持\n- 鼓励积极思考\n- 必要时建议寻求专业帮助",
                category = "情感"
            )
        )

        fun getDefaultTemplate(): PromptTemplate {
            return DEFAULT_TEMPLATES.find { it.isDefault } ?: DEFAULT_TEMPLATES.first()
        }

        fun getTemplatesByCategory(category: String): List<PromptTemplate> {
            return DEFAULT_TEMPLATES.filter { it.category == category }
        }

        fun searchTemplates(query: String): List<PromptTemplate> {
            return DEFAULT_TEMPLATES.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }
    }
}

class PromptManager {
    private val customTemplates = mutableListOf<PromptTemplate>()

    fun addCustomTemplate(template: PromptTemplate) {
        customTemplates.add(template)
    }

    fun removeCustomTemplate(templateName: String) {
        customTemplates.removeAll { it.name == templateName }
    }
    
    /**
     * 获取所有模板（默认+自定义）
     */
    fun getAllTemplates(): List<PromptTemplate> {
        return PromptTemplate.DEFAULT_TEMPLATES + customTemplates
    }

    fun getTemplateByName(name: String): PromptTemplate? {
        return getAllTemplates().find { it.name == name }
    }

    fun updateTemplate(templateName: String, newTemplate: PromptTemplate): Boolean {
        val index = customTemplates.indexOfFirst { it.name == templateName }
        return if (index != -1) {
            customTemplates[index] = newTemplate
            true
        } else {
            false
        }
    }
}
