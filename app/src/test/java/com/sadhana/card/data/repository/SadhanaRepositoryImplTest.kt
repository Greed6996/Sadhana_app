package com.sadhana.card.data.repository

import com.sadhana.card.data.local.dao.SadhanaDao
import com.sadhana.card.data.local.entity.SadhanaEntryConflictEntity
import com.sadhana.card.data.local.entity.SadhanaEntryEntity
import com.sadhana.card.data.local.entity.toDomain
import com.sadhana.card.data.preferences.ThemeMode
import com.sadhana.card.data.preferences.UserPreferences
import com.sadhana.card.data.preferences.UserPreferencesRepository
import com.sadhana.card.domain.model.SadhanaEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SadhanaRepositoryImplTest {

    private lateinit var fakeDao: FakeSadhanaDao
    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository
    private lateinit var repository: SadhanaRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeSadhanaDao()
        fakePrefsRepo = FakeUserPreferencesRepository()
        repository = SadhanaRepositoryImpl(fakeDao, fakePrefsRepo, null)
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

    @Test
    fun testAccountIsolation_userCannotSeeOtherUserData() = runBlocking {
        val date = LocalDate.of(2026, 10, 20)

        // User 1 saves an entry
        fakePrefsRepo.setAuthUid("user_alice")
        repository.upsertEntry(SadhanaEntry(date = date, napMinutes = 45))

        val aliceEntry = repository.getEntry(date).first()
        assertNotNull(aliceEntry)
        assertEquals(45, aliceEntry?.napMinutes)

        // Switch to User 2 (Bob)
        fakePrefsRepo.setAuthUid("user_bob")
        val bobEntry = repository.getEntry(date).first()
        assertNull("Bob should not see Alice's entry", bobEntry)

        // Bob saves their own entry on the same date
        repository.upsertEntry(SadhanaEntry(date = date, napMinutes = 15))
        val bobUpdatedEntry = repository.getEntry(date).first()
        assertNotNull(bobUpdatedEntry)
        assertEquals(15, bobUpdatedEntry?.napMinutes)

        // Switch back to Alice: verify her original entry is untouched
        fakePrefsRepo.setAuthUid("user_alice")
        val aliceRestoredEntry = repository.getEntry(date).first()
        assertNotNull(aliceRestoredEntry)
        assertEquals(45, aliceRestoredEntry?.napMinutes)
    }

    @Test
    fun testHistoricalClaim_preservesBothVersionsOnConflict() = runBlocking {
        val date = LocalDate.of(2026, 10, 15)

        // 1. Create a local entry
        fakePrefsRepo.setAuthUid("local")
        repository.upsertEntry(SadhanaEntry(date = date, napMinutes = 30))

        // 2. Create an existing entry for user_charlie on the same date with earlier timestamp
        val charlieEntity = SadhanaEntryEntity(
            userId = "user_charlie",
            date = date,
            napMinutes = 10,
            updatedAt = 1000L
        )
        fakeDao.storage[Pair("user_charlie", date)] = charlieEntity

        // 3. User claims local records safely
        fakeDao.claimLocalRecordsSafely("user_charlie")

        // 4. Verify both versions are preserved in the conflicts table
        assertEquals(2, fakeDao.conflicts.size)
        val localConflict = fakeDao.conflicts.find { it.source == "local_claim" }
        val supersededConflict = fakeDao.conflicts.find { it.source == "account_superseded" }

        assertEquals("user_charlie", localConflict?.userId)
        assertEquals(date, localConflict?.date)
        assertEquals(30, localConflict?.napMinutes)

        assertEquals("user_charlie", supersededConflict?.userId)
        assertEquals(date, supersededConflict?.date)
        assertEquals(10, supersededConflict?.napMinutes)

        // Active entry for user_charlie is the newer local entry
        val activeEntry = fakeDao.storage[Pair("user_charlie", date)]
        assertEquals(30, activeEntry?.napMinutes)
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val prefsFlow = MutableStateFlow(UserPreferences(authUid = "local"))

        override val userPreferencesFlow: Flow<UserPreferences> = prefsFlow

        fun setAuthUid(uid: String) {
            prefsFlow.value = prefsFlow.value.copy(authUid = uid)
        }

        override suspend fun updateSadhakaName(name: String) {}
        override suspend fun updateCounselorName(name: String) {}
        override suspend fun setDedicationExpanded(expanded: Boolean) {}
        override suspend fun setTermsAccepted(accepted: Boolean, version: Int) {}
        override suspend fun setThemeMode(themeMode: ThemeMode) {}
        override suspend fun setDismissedUpdateVersion(version: Int) {}
        override suspend fun recordDismissedUpdate(versionCode: Int, timestamp: Long) {}
    }

    private class FakeSadhanaDao : SadhanaDao {
        // Keyed by Pair(userId, date)
        val storage = mutableMapOf<Pair<String, LocalDate>, SadhanaEntryEntity>()
        val conflicts = mutableListOf<SadhanaEntryConflictEntity>()
        private val state = MutableStateFlow<Map<Pair<String, LocalDate>, SadhanaEntryEntity>>(emptyMap())

        private fun notifyChange() {
            state.value = storage.toMap()
        }

        override fun getEntry(userId: String, date: LocalDate): Flow<SadhanaEntryEntity?> {
            return state.map { it[Pair(userId, date)] }
        }

        override suspend fun getEntrySync(userId: String, date: LocalDate): SadhanaEntryEntity? {
            return storage[Pair(userId, date)]
        }

        override fun getEntriesForRange(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<SadhanaEntryEntity>> {
            return state.map { map ->
                map.values
                    .filter { it.userId == userId && !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
                    .sortedBy { it.date }
            }
        }

        override suspend fun getUnsyncedEntries(userId: String): List<SadhanaEntryEntity> {
            return storage.values.filter { it.userId == userId && !it.isSynced }
        }

        override suspend fun getLocalEntries(): List<SadhanaEntryEntity> {
            return storage.values.filter { it.userId == "local" }
        }

        override suspend fun markAsSynced(userId: String, date: LocalDate) {
            val key = Pair(userId, date)
            storage[key]?.let { storage[key] = it.copy(isSynced = true) }
            notifyChange()
        }

        override suspend fun insertConflict(conflict: SadhanaEntryConflictEntity) {
            conflicts.add(conflict)
        }

        override fun getConflicts(userId: String): Flow<List<SadhanaEntryConflictEntity>> {
            return flowOf(conflicts.filter { it.userId == userId })
        }

        override suspend fun getLocalRecordCount(): Int {
            return storage.values.count { it.userId == "local" }
        }

        override suspend fun upsertEntry(entry: SadhanaEntryEntity) {
            storage[Pair(entry.userId, entry.date)] = entry
            notifyChange()
        }

        override suspend fun deleteEntry(userId: String, date: LocalDate) {
            storage.remove(Pair(userId, date))
            notifyChange()
        }
    }
}
