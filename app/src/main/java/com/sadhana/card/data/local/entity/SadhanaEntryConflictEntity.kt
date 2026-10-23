package com.sadhana.card.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

/**
 * Audit and conflict recovery entity for Sadhana entries.
 * Preserves complete versions of conflicting entries when claiming historical local records.
 */
@Entity(tableName = "sadhana_entry_conflicts")
data class SadhanaEntryConflictEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: LocalDate,
    val source: String, // e.g. "local_claim" or "account_superseded"
    val conflictTimestamp: Long = System.currentTimeMillis(),
    val bedtime: LocalTime? = null,
    val wakeTime: LocalTime? = null,
    val napMinutes: Int? = null,
    val trainJourney: Boolean = false,
    val japaTime: LocalTime? = null,
    val bedtimeCategory: String? = null,
    val wakeUpCategory: String? = null,
    val daySleepCategory: String? = null,
    val japaCategory: String? = null,
    val japaCompleted: Boolean = false,
    val pathanMinutes: Int = 0,
    val sravanMinutes: Int = 0,
    val sevaMinutes: Int = 0,
    val sevaTags: List<String> = emptyList(),
    val isSynced: Boolean = false,
    val updatedAt: Long
)
