package com.sadhana.card.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.sadhana.card.data.local.entity.SadhanaEntryConflictEntity
import com.sadhana.card.data.local.entity.SadhanaEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for Sadhana entries.
 * Returns Flow off main thread and indices date column.
 */
@Dao
interface SadhanaDao {

    @Query("SELECT * FROM sadhana_entries WHERE userId = :userId AND date = :date LIMIT 1")
    fun getEntry(userId: String, date: LocalDate): Flow<SadhanaEntryEntity?>

    @Query("SELECT * FROM sadhana_entries WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getEntrySync(userId: String, date: LocalDate): SadhanaEntryEntity?

    @Query("SELECT * FROM sadhana_entries WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getEntriesForRange(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<SadhanaEntryEntity>>

    @Query("SELECT * FROM sadhana_entries WHERE userId = :userId AND isSynced = 0 ORDER BY date ASC")
    suspend fun getUnsyncedEntries(userId: String): List<SadhanaEntryEntity>

    @Query("SELECT * FROM sadhana_entries WHERE userId = 'local' ORDER BY date ASC")
    suspend fun getLocalEntries(): List<SadhanaEntryEntity>

    @Query("UPDATE sadhana_entries SET isSynced = 1 WHERE userId = :userId AND date = :date")
    suspend fun markAsSynced(userId: String, date: LocalDate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConflict(conflict: SadhanaEntryConflictEntity)

    @Query("SELECT * FROM sadhana_entry_conflicts WHERE userId = :userId ORDER BY conflictTimestamp DESC")
    fun getConflicts(userId: String): Flow<List<SadhanaEntryConflictEntity>>

    @Transaction
    suspend fun claimLocalRecordsSafely(newUserId: String) {
        val localEntries = getLocalEntries()
        for (local in localEntries) {
            val existing = getEntrySync(newUserId, local.date)
            if (existing == null) {
                // No collision: promote local record directly to new account
                upsertEntry(local.copy(userId = newUserId, isSynced = false))
                deleteEntry("local", local.date)
            } else {
                // Collision detected: preserve both versions in audit conflicts table
                val localConflict = SadhanaEntryConflictEntity(
                    userId = newUserId,
                    date = local.date,
                    source = "local_claim",
                    conflictTimestamp = System.currentTimeMillis(),
                    bedtime = local.bedtime,
                    wakeTime = local.wakeTime,
                    napMinutes = local.napMinutes,
                    trainJourney = local.trainJourney,
                    japaTime = local.japaTime,
                    bedtimeCategory = local.bedtimeCategory,
                    wakeUpCategory = local.wakeUpCategory,
                    daySleepCategory = local.daySleepCategory,
                    japaCategory = local.japaCategory,
                    japaCompleted = local.japaCompleted,
                    pathanMinutes = local.pathanMinutes,
                    sravanMinutes = local.sravanMinutes,
                    isSynced = local.isSynced,
                    updatedAt = local.updatedAt
                )
                insertConflict(localConflict)

                if (local.updatedAt > existing.updatedAt) {
                    val accountSuperseded = SadhanaEntryConflictEntity(
                        userId = newUserId,
                        date = existing.date,
                        source = "account_superseded",
                        conflictTimestamp = System.currentTimeMillis(),
                        bedtime = existing.bedtime,
                        wakeTime = existing.wakeTime,
                        napMinutes = existing.napMinutes,
                        trainJourney = existing.trainJourney,
                        japaTime = existing.japaTime,
                        bedtimeCategory = existing.bedtimeCategory,
                        wakeUpCategory = existing.wakeUpCategory,
                        daySleepCategory = existing.daySleepCategory,
                        japaCategory = existing.japaCategory,
                        japaCompleted = existing.japaCompleted,
                        pathanMinutes = existing.pathanMinutes,
                        sravanMinutes = existing.sravanMinutes,
                        isSynced = existing.isSynced,
                        updatedAt = existing.updatedAt
                    )
                    insertConflict(accountSuperseded)
                    upsertEntry(local.copy(userId = newUserId, isSynced = false))
                }
                deleteEntry("local", local.date)
            }
        }
    }

    suspend fun claimLocalRecords(newUserId: String) {
        claimLocalRecordsSafely(newUserId)
    }

    @Query("SELECT COUNT(*) FROM sadhana_entries WHERE userId = 'local'")
    suspend fun getLocalRecordCount(): Int

    @Upsert
    suspend fun upsertEntry(entry: SadhanaEntryEntity)

    @Query("DELETE FROM sadhana_entries WHERE userId = :userId AND date = :date")
    suspend fun deleteEntry(userId: String, date: LocalDate)
}

