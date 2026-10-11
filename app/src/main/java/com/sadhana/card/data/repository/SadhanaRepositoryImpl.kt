package com.sadhana.card.data.repository

import com.sadhana.card.data.local.dao.SadhanaDao
import com.sadhana.card.data.local.entity.toDomain
import com.sadhana.card.data.local.entity.toEntity
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.repository.SadhanaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clean Architecture repository implementation backed by Room database.
 */
@Singleton
class SadhanaRepositoryImpl @Inject constructor(
    private val dao: SadhanaDao
) : SadhanaRepository {

    override fun getEntry(date: LocalDate): Flow<SadhanaEntry?> {
        return dao.getEntry(date).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getEntriesForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<SadhanaEntry>> {
        return dao.getEntriesForRange(startDate, endDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getEntriesForWeek(startOfWeek: LocalDate): Flow<List<SadhanaEntry>> {
        val endOfWeek = startOfWeek.plusDays(6)
        return dao.getEntriesForRange(startOfWeek, endOfWeek).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun upsertEntry(entry: SadhanaEntry) {
        dao.upsertEntry(entry.toEntity())
    }

    override suspend fun deleteEntry(date: LocalDate) {
        dao.deleteEntry(date)
    }
}
