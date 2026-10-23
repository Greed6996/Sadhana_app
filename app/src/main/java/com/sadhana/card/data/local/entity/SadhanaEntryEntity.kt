package com.sadhana.card.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sadhana.card.domain.model.BedtimeCategory
import com.sadhana.card.domain.model.DaySleepCategory
import com.sadhana.card.domain.model.JapaCategory
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.model.WakeUpCategory
import java.time.LocalDate
import java.time.LocalTime

/**
 * Room database entity for daily Sadhana tracking.
 * Indexed by date for rapid range and single-day queries.
 *
 * Supports both legacy time-based fields and new category-based fields.
 * Category fields are stored as nullable Strings (enum name) for Room compatibility.
 */
@Entity(
    tableName = "sadhana_entries",
    primaryKeys = ["userId", "date"],
    indices = [Index(value = ["userId", "date"], unique = true)]
)
data class SadhanaEntryEntity(
    val userId: String = "local",
    val date: LocalDate,
    // Legacy fields
    val bedtime: LocalTime? = null,
    val wakeTime: LocalTime? = null,
    val napMinutes: Int? = null,
    val trainJourney: Boolean = false,
    val japaTime: LocalTime? = null,
    // New category-based fields (stored as enum name strings)
    val bedtimeCategory: String? = null,
    val wakeUpCategory: String? = null,
    val daySleepCategory: String? = null,
    val japaCategory: String? = null,
    val japaCompleted: Boolean = false,
    // Unscored fields
    val pathanMinutes: Int = 0,
    val sravanMinutes: Int = 0,
    val sevaMinutes: Int = 0,
    val sevaTags: List<String> = emptyList(),
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Mapping between Entity and Domain model.
 */
fun SadhanaEntryEntity.toDomain(): SadhanaEntry = SadhanaEntry(
    date = date,
    userId = userId,
    bedtime = bedtime,
    wakeTime = wakeTime,
    napMinutes = napMinutes,
    trainJourney = trainJourney,
    japaTime = japaTime,
    bedtimeCategory = bedtimeCategory?.let { runCatching { BedtimeCategory.valueOf(it) }.getOrNull() },
    wakeUpCategory = wakeUpCategory?.let { runCatching { WakeUpCategory.valueOf(it) }.getOrNull() },
    daySleepCategory = daySleepCategory?.let { runCatching { DaySleepCategory.valueOf(it) }.getOrNull() },
    japaCategory = japaCategory?.let { runCatching { JapaCategory.valueOf(it) }.getOrNull() },
    japaCompleted = japaCompleted || (japaCategory != null),
    pathanMinutes = pathanMinutes,
    sravanMinutes = sravanMinutes,
    sevaMinutes = sevaMinutes,
    sevaTags = sevaTags,
    isSynced = isSynced,
    updatedAt = updatedAt
)

fun SadhanaEntry.toEntity(): SadhanaEntryEntity = SadhanaEntryEntity(
    userId = userId,
    date = date,
    bedtime = bedtime,
    wakeTime = wakeTime,
    napMinutes = napMinutes,
    trainJourney = trainJourney,
    japaTime = japaTime,
    bedtimeCategory = bedtimeCategory?.name,
    wakeUpCategory = wakeUpCategory?.name,
    daySleepCategory = daySleepCategory?.name,
    japaCategory = japaCategory?.name,
    japaCompleted = japaCompleted,
    pathanMinutes = pathanMinutes,
    sravanMinutes = sravanMinutes,
    sevaMinutes = sevaMinutes,
    sevaTags = sevaTags,
    isSynced = isSynced,
    updatedAt = updatedAt
)
