package com.sadhana.card.data.repository

import com.sadhana.card.data.local.dao.SadhanaDao
import com.sadhana.card.data.local.entity.SadhanaEntryEntity
import com.sadhana.card.domain.model.SadhanaEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SadhanaRepositoryImplTest {

    private lateinit var fakeDao: FakeSadhanaDao
    private lateinit var repository: SadhanaRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeSadhanaDao()
        repository = SadhanaRepositoryImpl(fakeDao)
    }

    @Test
    fun testGetEntry_returnsMappedDomain() = runBlocking {
        val date = LocalDate.of(2026, 10, 11)
        val entry = SadhanaEntry(
            date = date,
            bedtime = LocalTime.of(21, 15),
            wakeTime = LocalTime.of(3, 45),
            napMinutes = 20,
            japaTime = LocalTime.of(7, 0)
        )

        repository.upsertEntry(entry)
        val result = repository.getEntry(date).first()

        assertEquals(entry, result)
    }

    @Test
    fun testGetEntry_nonExistent_returnsNull() = runBlocking {
        val result = repository.getEntry(LocalDate.of(2026, 1, 1)).first()
        assertNull(result)
    }

    @Test
    fun testGetEntriesForWeek_returnsSortedWeekList() = runBlocking {
        val monday = LocalDate.of(2026, 10, 5)
        val tuesday = LocalDate.of(2026, 10, 6)
        val nextMonday = LocalDate.of(2026, 10, 12)

        repository.upsertEntry(SadhanaEntry(date = monday, napMinutes = 10))
        repository.upsertEntry(SadhanaEntry(date = tuesday, napMinutes = 20))
        repository.upsertEntry(SadhanaEntry(date = nextMonday, napMinutes = 30))

        val weekEntries = repository.getEntriesForWeek(monday).first()
        assertEquals(2, weekEntries.size)
        assertEquals(monday, weekEntries[0].date)
        assertEquals(tuesday, weekEntries[1].date)
    }

    @Test
    fun testDeleteEntry_removesFromStorage() = runBlocking {
        val date = LocalDate.of(2026, 10, 11)
        repository.upsertEntry(SadhanaEntry(date = date))
        assertEquals(1, fakeDao.storage.size)

        repository.deleteEntry(date)
        val result = repository.getEntry(date).first()
        assertNull(result)
    }

    private class FakeSadhanaDao : SadhanaDao {
        val storage = mutableMapOf<LocalDate, SadhanaEntryEntity>()
        private val state = MutableStateFlow<Map<LocalDate, SadhanaEntryEntity>>(emptyMap())

        private fun notifyChange() {
            state.value = storage.toMap()
        }

        override fun getEntry(date: LocalDate): Flow<SadhanaEntryEntity?> {
            return state.map { it[date] }
        }

        override fun getEntriesForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<SadhanaEntryEntity>> {
            return state.map { map ->
                map.values.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
                    .sortedBy { it.date }
            }
        }

        override suspend fun upsertEntry(entry: SadhanaEntryEntity) {
            storage[entry.date] = entry
            notifyChange()
        }

        override suspend fun deleteEntry(date: LocalDate) {
            storage.remove(date)
            notifyChange()
        }
    }
}
