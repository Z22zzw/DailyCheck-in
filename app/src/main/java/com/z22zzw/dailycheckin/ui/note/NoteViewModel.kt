package com.z22zzw.dailycheckin.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z22zzw.dailycheckin.data.db.entity.NoteEntity
import com.z22zzw.dailycheckin.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NoteUiState(
    val notes: List<NoteEntity> = emptyList(),
    val filterType: String = "all",
    val isLoading: Boolean = true
)

class NoteViewModel(
    private val repository: NoteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    init { loadNotes() }

    private fun loadNotes() {
        viewModelScope.launch {
            repository.getAllNotes().collect { notes ->
                _uiState.value = _uiState.value.copy(notes = notes, isLoading = false)
            }
        }
    }

    fun setFilter(type: String) { _uiState.value = _uiState.value.copy(filterType = type) }

    fun createNote(title: String, content: String, type: String = "manual") {
        viewModelScope.launch { repository.createNote(title, content, type) }
    }

    fun saveAiReplyToNote(title: String, content: String) {
        viewModelScope.launch { repository.createNote(title, content, "manual") }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.deleteNote(id) }
    }
}
