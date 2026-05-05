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
    val isLoading: Boolean = true,
    val editingNoteId: Long? = null,
    val showDeleteConfirm: NoteEntity? = null
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

    fun startNewNote() { _uiState.value = _uiState.value.copy(editingNoteId = null) }

    fun startEditNote(note: NoteEntity) { _uiState.value = _uiState.value.copy(editingNoteId = note.id) }

    fun dismissEdit() { _uiState.value = _uiState.value.copy(editingNoteId = null) }

    fun saveNote(title: String, content: String, type: String = "manual") {
        viewModelScope.launch {
            val editingId = _uiState.value.editingNoteId
            if (editingId != null && editingId > 0) {
                val note = _uiState.value.notes.find { it.id == editingId } ?: return@launch
                repository.updateNote(note.copy(title = title, content = content))
            } else {
                repository.createNote(title, content, type)
            }
            _uiState.value = _uiState.value.copy(editingNoteId = null)
        }
    }

    fun showDeleteConfirm(note: NoteEntity) { _uiState.value = _uiState.value.copy(showDeleteConfirm = note) }
    fun dismissDeleteConfirm() { _uiState.value = _uiState.value.copy(showDeleteConfirm = null) }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
            _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
        }
    }

    fun saveAiReplyToNote(title: String, content: String) {
        viewModelScope.launch { repository.createNote(title, content, "manual") }
    }
}
