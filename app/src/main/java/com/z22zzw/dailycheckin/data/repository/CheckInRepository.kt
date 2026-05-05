package com.z22zzw.dailycheckin.data.repository

import com.z22zzw.dailycheckin.data.db.dao.CheckInRecordDao
import com.z22zzw.dailycheckin.data.db.dao.HabitDao
import com.z22zzw.dailycheckin.data.db.entity.CheckInRecordEntity
import com.z22zzw.dailycheckin.data.db.entity.HabitEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CheckInRepository(
    private val habitDao: HabitDao,
    private val checkInRecordDao: CheckInRecordDao
) {
    fun getActiveHabits(): Flow<List<HabitEntity>> = habitDao.getActiveHabits()

    fun getArchivedHabits(): Flow<List<HabitEntity>> = habitDao.getArchivedHabits()

    suspend fun createHabit(name: String, icon: String = ""): Long {
        return habitDao.insert(HabitEntity(name = name, icon = icon))
    }

    suspend fun archiveHabit(id: Long) = habitDao.archive(id)

    suspend fun getCheckInCount(habitId: Long): Int = checkInRecordDao.getCountByHabit(habitId)

    suspend fun isCheckedInToday(habitId: Long): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return checkInRecordDao.getByHabitAndDate(habitId, today) != null
    }

    suspend fun checkIn(habitId: Long) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (checkInRecordDao.getByHabitAndDate(habitId, today) == null) {
            checkInRecordDao.insert(CheckInRecordEntity(habitId = habitId, date = today))
        }
    }

    suspend fun getRecentCheckInDates(habitId: Long, limit: Int = 30): List<String> {
        return checkInRecordDao.getRecentDates(habitId, limit)
    }

    suspend fun getRecordsInRange(from: String, to: String): List<CheckInRecordEntity> {
        return checkInRecordDao.getRecordsInRange(from, to)
    }

    suspend fun uncheckIn(habitId: Long) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        checkInRecordDao.deleteByHabitAndDate(habitId, today)
    }
}
