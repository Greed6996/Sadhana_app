package com.sadhana.card.data.local.converter

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Room TypeConverters for java.time types (LocalDate, LocalTime) and List<String>.
 */
class SadhanaTypeConverters {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, dateFormatter) }
    }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? {
        return time?.format(timeFormatter)
    }

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it, timeFormatter) }
    }

    @TypeConverter
    fun fromStringList(tags: List<String>?): String? {
        if (tags == null) return null
        if (tags.isEmpty()) return ""
        return tags.joinToString(DELIMITER)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        if (value.isEmpty()) return emptyList()
        return value.split(DELIMITER).filter { it.isNotBlank() }
    }

    companion object {
        private const val DELIMITER = "|||"
    }
}
