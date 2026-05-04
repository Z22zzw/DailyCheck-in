package com.z22zzw.dailycheckin.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.z22zzw.dailycheckin.data.db.dao.AiMessageDao
import com.z22zzw.dailycheckin.data.db.dao.CheckInRecordDao
import com.z22zzw.dailycheckin.data.db.dao.HabitDao
import com.z22zzw.dailycheckin.data.db.dao.NoteDao
import com.z22zzw.dailycheckin.data.db.dao.ProjectDao
import com.z22zzw.dailycheckin.data.db.dao.TaskDao
import com.z22zzw.dailycheckin.data.db.entity.AiMessageEntity
import com.z22zzw.dailycheckin.data.db.entity.CheckInRecordEntity
import com.z22zzw.dailycheckin.data.db.entity.HabitEntity
import com.z22zzw.dailycheckin.data.db.entity.NoteEntity
import com.z22zzw.dailycheckin.data.db.entity.ProjectEntity
import com.z22zzw.dailycheckin.data.db.entity.TaskEntity

@Database(
    entities = [
        HabitEntity::class,
        CheckInRecordEntity::class,
        ProjectEntity::class,
        TaskEntity::class,
        NoteEntity::class,
        AiMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun checkInRecordDao(): CheckInRecordDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun aiMessageDao(): AiMessageDao
}
