package com.z22zzw.dailycheckin.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z22zzw.dailycheckin.data.db.entity.HabitEntity
import com.z22zzw.dailycheckin.data.repository.CheckInRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HabitItem(
    val habit: HabitEntity,
    val totalCount: Int,
    val checkedInToday: Boolean
)

data class CheckInUiState(
    val habits: List<HabitItem> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showUncheckConfirm: Long? = null
)

class CheckInViewModel(
    private val repository: CheckInRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    init { refresh() }

    private fun refresh() {
        viewModelScope.launch {
            val habits = repository.getActiveHabits().first()
            val items = habits.map { habit ->
                HabitItem(
                    habit = habit,
                    totalCount = repository.getCheckInCount(habit.id),
                    checkedInToday = repository.isCheckedInToday(habit.id)
                )
            }
            _uiState.value = _uiState.value.copy(habits = items, isLoading = false)
        }
    }

    fun checkIn(habitId: Long) {
        viewModelScope.launch {
            repository.checkIn(habitId)
            refresh()
        }
    }

    fun uncheckIn(habitId: Long) {
        viewModelScope.launch {
            repository.uncheckIn(habitId)
            refresh()
        }
    }

    fun showUncheckConfirm(habitId: Long) {
        _uiState.value = _uiState.value.copy(showUncheckConfirm = habitId)
    }

    fun dismissUncheckConfirm() {
        _uiState.value = _uiState.value.copy(showUncheckConfirm = null)
    }

    fun addHabit(name: String) {
        viewModelScope.launch {
            repository.createHabit(name)
            _uiState.value = _uiState.value.copy(showAddDialog = false)
            refresh()
        }
    }

    fun showAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = true) }
    fun dismissAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = false) }
}
