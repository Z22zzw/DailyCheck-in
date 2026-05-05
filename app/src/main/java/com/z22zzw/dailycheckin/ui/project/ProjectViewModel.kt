package com.z22zzw.dailycheckin.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z22zzw.dailycheckin.data.db.entity.ProjectEntity
import com.z22zzw.dailycheckin.data.db.entity.TaskEntity
import com.z22zzw.dailycheckin.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProjectWithProgress(
    val project: ProjectEntity,
    val totalTasks: Int,
    val doneTasks: Int,
    val tasks: List<TaskEntity>
)

data class ProjectUiState(
    val projects: List<ProjectWithProgress> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false
)

class ProjectViewModel(
    private val repository: ProjectRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    init { loadProjects() }

    private fun loadProjects() {
        viewModelScope.launch {
            repository.getActiveProjects().collect { projects ->
                val items = projects.map { project ->
                    val tasks = repository.getTasksByProject(project.id).first()
                    ProjectWithProgress(
                        project = project,
                        totalTasks = repository.getTaskCount(project.id),
                        doneTasks = repository.getDoneTaskCount(project.id),
                        tasks = tasks
                    )
                }
                _uiState.value = ProjectUiState(projects = items, isLoading = false)
            }
        }
    }

    fun createProject(name: String, deadline: Long?) {
        viewModelScope.launch {
            repository.createProject(name = name, deadline = deadline)
            _uiState.value = _uiState.value.copy(showCreateDialog = false)
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch { repository.toggleTask(task) }
    }

    fun addTask(projectId: Long, title: String) {
        viewModelScope.launch { repository.createTask(projectId, title) }
    }

    fun showCreateDialog() { _uiState.value = _uiState.value.copy(showCreateDialog = true) }
    fun dismissCreateDialog() { _uiState.value = _uiState.value.copy(showCreateDialog = false) }
}
