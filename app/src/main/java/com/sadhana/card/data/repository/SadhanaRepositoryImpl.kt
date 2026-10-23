package com.sadhana.card.data.repository

import com.sadhana.card.data.local.dao.SadhanaDao
import com.sadhana.card.data.local.entity.toDomain
import com.sadhana.card.data.local.entity.toEntity
import com.sadhana.card.data.preferences.ThemeMode
import com.sadhana.card.data.preferences.UserPreferences
import com.sadhana.card.data.preferences.UserPreferencesRepository
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.repository.SadhanaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clean Architecture repository implementation backed by Room database.
 * Strictly isolates accounts by scoping all read and write queries to the active userId.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SadhanaRepositoryImpl @Inject constructor(
    private val dao: SadhanaDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val syncManager: com.sadhana.card.data.sync.SyncManager? = null
) : SadhanaRepository {

    // Secondary constructor for test doubles
    constructor(dao: SadhanaDao) : this(
        dao = dao,
        userPreferencesRepository = object : UserPreferencesRepository {
            override val userPreferencesFlow: Flow<UserPreferences> = flowOf(UserPreferences())
            override suspend fun updateSadhakaName(name: String) {}
            override suspend fun updateCounselorName(name: String) {}
            override suspend fun setDedicationExpanded(expanded: Boolean) {}
            override suspend fun setTermsAccepted(accepted: Boolean, version: Int) {}
            override suspend fun setThemeMode(themeMode: ThemeMode) {}
            override suspend fun setDismissedUpdateVersion(version: Int) {}
            override suspend fun recordDismissedUpdate(versionCode: Int, timestamp: Long) {}
        },
        syncManager = null
    )

    private val currentUserIdFlow: Flow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.authUid.ifBlank { "local" } }
        .distinctUntilChanged()

    override fun getEntry(date: LocalDate): Flow<SadhanaEntry?> {
        return currentUserIdFlow.flatMapLatest { userId ->
            dao.getEntry(userId, date).map { entity ->
                entity?.toDomain()
            }
        }
    }

    override fun getEntriesForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<SadhanaEntry>> {
        return currentUserIdFlow.flatMapLatest { userId ->
            dao.getEntriesForRange(userId, startDate, endDate).map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    override fun getEntriesForWeek(startOfWeek: LocalDate): Flow<List<SadhanaEntry>> {
        val endOfWeek = startOfWeek.plusDays(6)
        return getEntriesForRange(startOfWeek, endOfWeek)
    }

    override suspend fun upsertEntry(entry: SadhanaEntry) {
        val currentUserId = userPreferencesRepository.userPreferencesFlow.first().authUid.ifBlank { "local" }
        val scopedEntry = if (entry.userId.isBlank() || entry.userId != currentUserId) {
            entry.copy(userId = currentUserId)
        } else {
            entry
        }
        dao.upsertEntry(scopedEntry.toEntity())
        syncManager?.scheduleDebouncedSync(scopedEntry)
    }

    override suspend fun deleteEntry(date: LocalDate) {
        val currentUserId = userPreferencesRepository.userPreferencesFlow.first().authUid.ifBlank { "local" }
        dao.deleteEntry(currentUserId, date)
    }
}

