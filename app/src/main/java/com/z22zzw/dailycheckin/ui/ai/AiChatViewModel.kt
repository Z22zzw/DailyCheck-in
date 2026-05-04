package com.z22zzw.dailycheckin.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z22zzw.dailycheckin.data.db.entity.AiMessageEntity
import com.z22zzw.dailycheckin.data.repository.AiRepository
import com.z22zzw.dailycheckin.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class AiChatUiState(
    val messages: List<AiMessageEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val onboardingQuestion: String? = null
)

class AiChatViewModel(
    private val aiRepository: AiRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var onboardingManager: AiOnboardingManager? = null

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
                    onboardingQuestion = onboardingManager?.getCurrentQuestion()
                )
            }
        }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            if (onboardingManager != null && !onboardingManager!!.isComplete()) {
                val profileContent = onboardingManager!!.recordAnswer(content)
                if (profileContent != null) {
                    noteRepository.createNote("我的个人画像", profileContent, "profile")
                    onboardingManager = null
                    _uiState.value = _uiState.value.copy(onboardingQuestion = null, isLoading = false)
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    onboardingQuestion = onboardingManager?.getCurrentQuestion(),
                    isLoading = false
                )
                return@launch
            }

            val result = aiRepository.sendMessage(content)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "请求失败") }
            )
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
}
