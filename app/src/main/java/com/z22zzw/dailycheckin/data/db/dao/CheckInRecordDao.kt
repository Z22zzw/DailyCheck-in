package com.z22zzw.dailycheckin.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.z22zzw.dailycheckin.data.db.entity.CheckInRecordEntity

@Dao
interface CheckInRecordDao {
    @Query("SELECT * FROM check_in_records WHERE habit_id = :habitId AND date = :date LIMIT 1")
    suspend fun getByHabitAndDate(habitId: Long, date: String): CheckInRecordEntity?

    @Query("SELECT COUNT(*) FROM check_in_records WHERE habit_id = :habitId")
    suspend fun getCountByHabit(habitId: Long): Int

    @Query("SELECT date FROM check_in_records WHERE habit_id = :habitId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentDates(habitId: Long, limit: Int = 30): List<String>

    @Query("SELECT * FROM check_in_records WHERE date >= :from AND date <= :to")
    suspend fun getRecordsInRange(from: String, to: String): List<CheckInRecordEntity>

    @Insert
    suspend fun insert(record: CheckInRecordEntity): Long
}
