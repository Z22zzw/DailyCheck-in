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
    val showCreateDialog: Boolean = false,
    val showEditDialog: ProjectEntity? = null,
    val showDeleteConfirm: ProjectEntity? = null,
    val expandedProjectId: Long? = null,
    val newTaskTitle: String = ""
)

class ProjectViewModel(private val repository: ProjectRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    init { refresh() }

    private fun refresh() {
        viewModelScope.launch {
            val projects = repository.getActiveProjects().first()
            val items = projects.map { project ->
                val tasks = repository.getTasksByProject(project.id).first()
                ProjectWithProgress(
                    project = project,
                    totalTasks = repository.getTaskCount(project.id),
                    doneTasks = repository.getDoneTaskCount(project.id),
                    tasks = tasks
                )
            }
            _uiState.value = _uiState.value.copy(projects = items, isLoading = false)
        }
    }

    fun createProject(name: String, deadline: Long?) {
        viewModelScope.launch {
            repository.createProject(name = name, deadline = deadline)
            _uiState.value = _uiState.value.copy(showCreateDialog = false)
            refresh()
        }
    }

    fun updateProject(name: String, deadline: Long?) {
        val editing = _uiState.value.showEditDialog ?: return
        viewModelScope.launch {
            repository.updateProject(editing.copy(name = name, deadline = deadline))
            _uiState.value = _uiState.value.copy(showEditDialog = null)
            refresh()
        }
    }

    fun deleteProject() {
        val project = _uiState.value.showDeleteConfirm ?: return
        viewModelScope.launch {
            repository.deleteProject(project.id)
            _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
            refresh()
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTask(task)
            refresh()
        }
    }

    fun addTask(projectId: Long, title: String) {
        viewModelScope.launch {
            repository.createTask(projectId, title)
            _uiState.value = _uiState.value.copy(newTaskTitle = "")
            refresh()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
            refresh()
        }
    }

    fun showCreateDialog() { _uiState.value = _uiState.value.copy(showCreateDialog = true) }
    fun dismissCreateDialog() { _uiState.value = _uiState.value.copy(showCreateDialog = false) }
    fun showEditDialog(project: ProjectEntity) { _uiState.value = _uiState.value.copy(showEditDialog = project) }
    fun dismissEditDialog() { _uiState.value = _uiState.value.copy(showEditDialog = null) }
    fun showDeleteConfirm(project: ProjectEntity) { _uiState.value = _uiState.value.copy(showDeleteConfirm = project) }
    fun dismissDeleteConfirm() { _uiState.value = _uiState.value.copy(showDeleteConfirm = null) }
    fun toggleExpand(projectId: Long) {
        val cur = _uiState.value.expandedProjectId
        _uiState.value = _uiState.value.copy(expandedProjectId = if (cur == projectId) null else projectId)
    }
    fun setNewTaskTitle(title: String) { _uiState.value = _uiState.value.copy(newTaskTitle = title) }
}
