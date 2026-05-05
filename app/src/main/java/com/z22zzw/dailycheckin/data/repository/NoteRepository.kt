package com.z22zzw.dailycheckin.data.repository

import com.z22zzw.dailycheckin.data.db.dao.NoteDao
import com.z22zzw.dailycheckin.data.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAll()

    fun getNotesByType(type: String): Flow<List<NoteEntity>> = noteDao.getByType(type)

    suspend fun getNote(id: Long): NoteEntity? = noteDao.getById(id)

    suspend fun createNote(title: String, content: String, type: String = "manual"): Long {
        return noteDao.insert(NoteEntity(title = title, content = content, type = type))
    }

    suspend fun updateNote(note: NoteEntity) = noteDao.update(note)

    suspend fun deleteNote(id: Long) = noteDao.delete(id)

    fun getProfileNote(): Flow<List<NoteEntity>> = noteDao.getByType("profile")
}
