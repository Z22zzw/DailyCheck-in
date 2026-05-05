package com.z22zzw.dailycheckin.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.z22zzw.dailycheckin.data.db.entity.AiMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMessageDao {
    @Query("SELECT * FROM ai_messages ORDER BY created_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<AiMessageEntity>>

    @Query("SELECT * FROM ai_messages ORDER BY created_at ASC")
    fun getAllAsc(): Flow<List<AiMessageEntity>>

    @Insert
    suspend fun insert(message: AiMessageEntity): Long

    @Query("DELETE FROM ai_messages")
    suspend fun clearAll()
}
