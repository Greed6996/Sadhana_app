package com.sadhana.card.domain.repository

import com.sadhana.card.domain.model.SadhanaEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Domain repository contract for Sadhana entries.
 * Offline-first, reactive via Flows.
 */
interface SadhanaRepository {

    /**
     * Observes the entry for a specific date.
     * Emits null if no entry has been logged for this date.
     */
    fun getEntry(date: LocalDate): Flow<SadhanaEntry?>

    /**
     * Observes all entries within a date range [startDate, endDate] inclusive.
     */
    fun getEntriesForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<SadhanaEntry>>

    /**
     * Observes all 7 entries for the week starting on [startOfWeek] (Sunday).
     */
    fun getEntriesForWeek(startOfWeek: LocalDate): Flow<List<SadhanaEntry>>

    /**
     * Inserts or updates an entry for its specific date.
     */
    suspend fun upsertEntry(entry: SadhanaEntry)

    /**
     * Deletes the entry for the specified date.
     */
    suspend fun deleteEntry(date: LocalDate)
}
