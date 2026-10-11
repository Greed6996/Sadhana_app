package com.sadhana.card.data.local.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SadhanaTypeConvertersTest {

    private val converters = SadhanaTypeConverters()

    @Test
    fun testLocalDateConversion() {
        val date = LocalDate.of(2026, 10, 11)
        val str = converters.fromLocalDate(date)
        assertEquals("2026-10-11", str)
        val parsed = converters.toLocalDate(str)
        assertEquals(date, parsed)
    }

    @Test
    fun testLocalDateNullConversion() {
        assertNull(converters.fromLocalDate(null))
        assertNull(converters.toLocalDate(null))
    }

    @Test
    fun testLocalTimeConversion() {
        val time = LocalTime.of(21, 45, 0)
        val str = converters.fromLocalTime(time)
        val parsed = converters.toLocalTime(str)
        assertEquals(time, parsed)
    }

    @Test
    fun testLocalTimeNullConversion() {
        assertNull(converters.fromLocalTime(null))
        assertNull(converters.toLocalTime(null))
    }

    @Test
    fun testStringListConversion() {
        val tags = listOf("Cooking", "Mentoring", "Temple cleaning")
        val str = converters.fromStringList(tags)
        val parsed = converters.toStringList(str)
        assertEquals(tags, parsed)
    }

    @Test
    fun testStringListEmptyAndNullConversion() {
        val empty = emptyList<String>()
        val str = converters.fromStringList(empty)
        assertEquals("", str)
        assertEquals(emptyList<String>(), converters.toStringList(str))

        assertNull(converters.fromStringList(null))
        assertNull(converters.toStringList(null))
    }
}
