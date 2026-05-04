package com.z22zzw.dailycheckin.ui.ai

class AiOnboardingManager {
    private val questions = listOf(
        "嗨！让我们快速设置一下。你现在最重要的目标是什么？",
        "你一般在哪些时间段精力最好？",
        "你觉得自己最容易在什么事情上拖延？",
        "有没有什么习惯你一直想养成但没坚持下来？",
        "还有什么特别想让 AI 了解的吗？（可以直接跳过）"
    )

    private var currentQuestionIndex = 0
    private val answers = mutableListOf<String>()

    fun isComplete(): Boolean = currentQuestionIndex >= questions.size

    fun getCurrentQuestion(): String? {
        return if (isComplete()) null else questions[currentQuestionIndex]
    }

    fun recordAnswer(answer: String): String? {
        answers.add(answer)
        currentQuestionIndex++
        return if (isComplete()) buildProfile() else null
    }

    private fun buildProfile(): String {
        return buildString {
            appendLine("### 个人画像")
            appendLine()
            appendLine("**目标：** ${answers.getOrElse(0) { "" }}")
            appendLine("**精力时段：** ${answers.getOrElse(1) { "" }}")
            appendLine("**容易拖延的事：** ${answers.getOrElse(2) { "" }}")
            appendLine("**想养成的习惯：** ${answers.getOrElse(3) { "" }}")
            if (answers.size > 4 && answers[4].isNotBlank()) {
                appendLine("**补充：** ${answers[4]}")
            }
        }
    }
}
