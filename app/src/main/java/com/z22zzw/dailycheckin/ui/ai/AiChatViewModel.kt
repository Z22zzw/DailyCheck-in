package com.z22zzw.dailycheckin.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z22zzw.dailycheckin.data.db.entity.AiMessageEntity
import com.z22zzw.dailycheckin.data.repository.AiRepository
import com.z22zzw.dailycheckin.data.repository.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class AiChatUiState(
    val messages: List<AiMessageEntity> = emptyList(),
    val isLoading: Boolean = false,
    val streamingText: String = "",
    val error: String? = null,
    val onboardingStep: OnboardingStep? = null
)

class AiChatViewModel(
    private val aiRepository: AiRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var onboardingManager: AiOnboardingManager? = null
    private var streamJob: Job? = null

    init {
        loadMessages()
        checkOnboarding()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            aiRepository.getMessages().collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
    }

    private fun checkOnboarding() {
        viewModelScope.launch {
            val profileNotes = noteRepository.getProfileNote().firstOrNull() ?: emptyList()
            if (profileNotes.isEmpty()) {
                onboardingManager = AiOnboardingManager()
                _uiState.value = _uiState.value.copy(
                    onboardingStep = onboardingManager?.getCurrentStep()
                )
            }
        }
    }

    fun onboardingAnswer(value: String) {
        val manager = onboardingManager ?: return
        val nextStep = manager.recordAnswer(value)
        if (manager.isComplete()) {
            val profile = manager.buildProfile()
            viewModelScope.launch {
                noteRepository.createNote("我的个人画像", profile, "profile")
            }
            onboardingManager = null
            _uiState.value = _uiState.value.copy(onboardingStep = null)
        } else {
            _uiState.value = _uiState.value.copy(onboardingStep = nextStep)
        }
    }

    fun skipOnboarding() {
        onboardingManager = null
        _uiState.value = _uiState.value.copy(onboardingStep = null)
    }

    fun sendMessage(content: String) {
        // 取消上一个流
        streamJob?.cancel()

        // 乐观插入用户消息
        val userMsg = AiMessageEntity(role = "user", content = content, createdAt = System.currentTimeMillis())
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(userMsg)
        _uiState.value = _uiState.value.copy(
            messages = currentMessages,
            isLoading = true,
            streamingText = "",
            error = null
        )

        // 流式请求
        var fullReply = ""
        streamJob = viewModelScope.launch {
            aiRepository.sendMessageStream(content)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streamingText = "",
                        error = e.message ?: "请求失败"
                    )
                }
                .collect { delta ->
                    fullReply += delta
                    _uiState.value = _uiState.value.copy(streamingText = fullReply)
                }

            // 流结束：添加 AI 消息到列表
            if (fullReply.isNotBlank()) {
                val aiMsg = AiMessageEntity(role = "assistant", content = fullReply, createdAt = System.currentTimeMillis())
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + aiMsg,
                    isLoading = false,
                    streamingText = ""
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, streamingText = "")
            }
        }
    }

    fun generateReport(type: String = "本周") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = aiRepository.generateReport(type)
            result.fold(
                onSuccess = { report ->
                    noteRepository.createNote("${type}AI 分析报告", report, "ai_report")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "生成报告失败") }
            )
        }
    }

    fun saveMessageToNote(messageContent: String) {
        viewModelScope.launch {
            noteRepository.createNote("AI 回复 · ${System.currentTimeMillis()}", messageContent, "manual")
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun getOnboardingAnswer(key: String): String? = onboardingManager?.getAnswer(key)
}
