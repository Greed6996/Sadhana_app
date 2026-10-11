package com.sadhana.card.data.local.entity

import com.sadhana.card.domain.model.SadhanaEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SadhanaEntityMappingTest {

    @Test
    fun testDomainToEntityAndBack() {
        val domain = SadhanaEntry(
            date = LocalDate.of(2026, 10, 11),
            bedtime = LocalTime.of(21, 45),
            wakeTime = LocalTime.of(3, 20),
            napMinutes = 40,
            trainJourney = true,
            japaTime = LocalTime.of(8, 10),
            pathanMinutes = 45,
            sravanMinutes = 30,
            sevaMinutes = 60,
            sevaTags = listOf("Cooking", "Mentoring"),
            updatedAt = 123456789L
        )

        val entity = domain.toEntity()
        assertEquals(domain.date, entity.date)
        assertEquals(domain.bedtime, entity.bedtime)
        assertEquals(domain.wakeTime, entity.wakeTime)
        assertEquals(domain.napMinutes, entity.napMinutes)
        assertEquals(domain.trainJourney, entity.trainJourney)
        assertEquals(domain.japaTime, entity.japaTime)
        assertEquals(domain.pathanMinutes, entity.pathanMinutes)
        assertEquals(domain.sravanMinutes, entity.sravanMinutes)
        assertEquals(domain.sevaMinutes, entity.sevaMinutes)
        assertEquals(domain.sevaTags, entity.sevaTags)
        assertEquals(domain.updatedAt, entity.updatedAt)

        val reconstructed = entity.toDomain()
        assertEquals(domain, reconstructed)
    }

    @Test
    fun testDefaultEntryMapping() {
        val domain = SadhanaEntry(date = LocalDate.of(2026, 10, 12))
        val entity = domain.toEntity()
        assertEquals(domain.date, entity.date)
        assertEquals(null, entity.bedtime)
        assertEquals(null, entity.wakeTime)
        assertEquals(null, entity.napMinutes)
        assertEquals(false, entity.trainJourney)
        assertEquals(null, entity.japaTime)
        assertEquals(emptyList<String>(), entity.sevaTags)

        val reconstructed = entity.toDomain()
        assertEquals(domain, reconstructed)
    }
}
