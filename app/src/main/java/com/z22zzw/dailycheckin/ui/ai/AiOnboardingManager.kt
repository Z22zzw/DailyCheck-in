package com.z22zzw.dailycheckin.ui.ai

sealed class OnboardingStep(open val question: String, open val key: String) {
    data class TextInput(
        override val question: String,
        override val key: String,
        val placeholder: String = ""
    ) : OnboardingStep(question, key)

    data class Options(
        override val question: String,
        override val key: String,
        val options: List<String>
    ) : OnboardingStep(question, key)

    data class DatePicker(
        override val question: String,
        override val key: String
    ) : OnboardingStep(question, key)
}

class AiOnboardingManager {
    private val steps = listOf(
        OnboardingStep.TextInput("请先输入您的 DeepSeek API Key", "api_key", "sk-..."),
        OnboardingStep.TextInput("您的姓名是？", "name", "请输入姓名"),
        OnboardingStep.Options("您的性别是？", "gender", listOf("男", "女")),
        OnboardingStep.DatePicker("您的出生年月是？", "birth_date"),
        OnboardingStep.TextInput("您的职业或身份是？", "occupation", "如：学生、程序员..."),
        OnboardingStep.Options("您更偏好什么类型的建议？", "advice_style", listOf("鼓励型", "直接型", "分析型"))
    )

    private var currentIndex = 0
    private val answers = mutableMapOf<String, String>()

    fun isComplete(): Boolean = currentIndex >= steps.size

    fun getCurrentStep(): OnboardingStep? {
        return if (isComplete()) null else steps[currentIndex]
    }

    fun recordAnswer(value: String): OnboardingStep? {
        val step = steps[currentIndex]
        answers[step.key] = value
        currentIndex++
        return if (isComplete()) null else steps[currentIndex]
    }

    fun buildProfile(): String {
        return buildString {
            appendLine("### 个人画像")
            appendLine()
            answers["name"]?.let { appendLine("**姓名：** $it") }
            answers["gender"]?.let { appendLine("**性别：** $it") }
            answers["birth_date"]?.let { appendLine("**出生年月：** $it") }
            answers["occupation"]?.let { appendLine("**职业/身份：** $it") }
            answers["advice_style"]?.let { appendLine("**偏好建议风格：** $it") }
        }
    }

    fun getAnswer(key: String): String? = answers[key]
}
