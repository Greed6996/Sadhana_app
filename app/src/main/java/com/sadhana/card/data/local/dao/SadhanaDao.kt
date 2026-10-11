package com.sadhana.card.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sadhana.card.data.local.entity.SadhanaEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for Sadhana entries.
 * Returns Flow off main thread and indices date column.
 */
@Dao
interface SadhanaDao {

    @Query("SELECT * FROM sadhana_entries WHERE date = :date LIMIT 1")
    fun getEntry(date: LocalDate): Flow<SadhanaEntryEntity?>

    @Query("SELECT * FROM sadhana_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getEntriesForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<SadhanaEntryEntity>>

    @Upsert
    suspend fun upsertEntry(entry: SadhanaEntryEntity)

    @Query("DELETE FROM sadhana_entries WHERE date = :date")
    suspend fun deleteEntry(date: LocalDate)
}
