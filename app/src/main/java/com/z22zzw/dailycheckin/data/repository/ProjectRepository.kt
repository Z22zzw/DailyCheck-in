package com.z22zzw.dailycheckin.data.repository

import com.z22zzw.dailycheckin.data.db.dao.ProjectDao
import com.z22zzw.dailycheckin.data.db.dao.TaskDao
import com.z22zzw.dailycheckin.data.db.entity.ProjectEntity
import com.z22zzw.dailycheckin.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao
) {
    fun getActiveProjects(): Flow<List<ProjectEntity>> = projectDao.getActiveProjects()

    fun getDoneProjects(): Flow<List<ProjectEntity>> = projectDao.getDoneProjects()

    suspend fun getProject(id: Long): ProjectEntity? = projectDao.getById(id)

    suspend fun createProject(name: String, description: String = "", deadline: Long? = null): Long {
        return projectDao.insert(ProjectEntity(name = name, description = description, deadline = deadline))
    }

    suspend fun updateStatus(id: Long, status: String) = projectDao.updateStatus(id, status)

    suspend fun getTasksByProject(projectId: Long): Flow<List<TaskEntity>> = taskDao.getByProject(projectId)

    suspend fun createTask(projectId: Long, title: String, sortOrder: Int = 0): Long {
        return taskDao.insert(TaskEntity(projectId = projectId, title = title, sortOrder = sortOrder))
    }

    suspend fun toggleTask(task: TaskEntity) {
        val newStatus = if (task.status == "done") "todo" else "done"
        val doneAt = if (newStatus == "done") System.currentTimeMillis() else null
        taskDao.update(task.copy(status = newStatus, doneAt = doneAt))
    }

    suspend fun getTaskCount(projectId: Long): Int = taskDao.countByProject(projectId)

    suspend fun getDoneTaskCount(projectId: Long): Int = taskDao.countDoneByProject(projectId)

    suspend fun deleteTask(id: Long) = taskDao.delete(id)

    suspend fun deleteProject(id: Long) = projectDao.delete(id)

    suspend fun updateProject(project: ProjectEntity) = projectDao.update(project)
}
