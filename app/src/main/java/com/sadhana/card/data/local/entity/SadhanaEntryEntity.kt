package com.sadhana.card.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sadhana.card.domain.model.SadhanaEntry
import java.time.LocalDate
import java.time.LocalTime

/**
 * Room database entity for daily Sadhana tracking.
 * Indexed by date for rapid range and single-day queries.
 */
@Entity(
    tableName = "sadhana_entries",
    indices = [Index(value = ["date"], unique = true)]
)
data class SadhanaEntryEntity(
    @PrimaryKey
    val date: LocalDate,
    val bedtime: LocalTime? = null,
    val wakeTime: LocalTime? = null,
    val napMinutes: Int? = null,
    val trainJourney: Boolean = false,
    val japaTime: LocalTime? = null,
    val pathanMinutes: Int = 0,
    val sravanMinutes: Int = 0,
    val sevaMinutes: Int = 0,
    val sevaTags: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Mapping between Entity and Domain model.
 */
fun SadhanaEntryEntity.toDomain(): SadhanaEntry = SadhanaEntry(
    date = date,
    bedtime = bedtime,
    wakeTime = wakeTime,
    napMinutes = napMinutes,
    trainJourney = trainJourney,
    japaTime = japaTime,
    pathanMinutes = pathanMinutes,
    sravanMinutes = sravanMinutes,
    sevaMinutes = sevaMinutes,
    sevaTags = sevaTags,
    updatedAt = updatedAt
)

fun SadhanaEntry.toEntity(): SadhanaEntryEntity = SadhanaEntryEntity(
    date = date,
    bedtime = bedtime,
    wakeTime = wakeTime,
    napMinutes = napMinutes,
    trainJourney = trainJourney,
    japaTime = japaTime,
    pathanMinutes = pathanMinutes,
    sravanMinutes = sravanMinutes,
    sevaMinutes = sevaMinutes,
    sevaTags = sevaTags,
    updatedAt = updatedAt
)
