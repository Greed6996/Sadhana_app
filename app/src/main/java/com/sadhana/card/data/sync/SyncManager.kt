package com.sadhana.card.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sadhana.card.data.local.dao.SadhanaDao
import com.sadhana.card.data.local.entity.toDomain
import com.sadhana.card.data.preferences.UserPreferencesRepository
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.scorer.SadhanaScorer
import com.sadhana.card.domain.util.DateUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun sadhanaDao(): SadhanaDao
    fun userPreferencesRepository(): UserPreferencesRepository
}

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java
        )
        val dao = entryPoint.sadhanaDao()
        val prefsRepo = entryPoint.userPreferencesRepository()
        val prefs = prefsRepo.userPreferencesFlow.first()
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null || prefs.authUid.isBlank()) {
            return@withContext Result.success()
        }

        try {
            val unsynced = dao.getUnsyncedEntries(currentUser.uid)
            if (unsynced.isEmpty()) {
                return@withContext Result.success()
            }

            val firestore = FirebaseFirestore.getInstance()
            val weekGroups = unsynced.groupBy { DateUtils.getStartOfWeek(it.date) }

            for ((startOfWeek, _) in weekGroups) {
                val endOfWeek = startOfWeek.plusDays(6)
                val allWeekEntities = dao.getEntriesForRange(currentUser.uid, startOfWeek, endOfWeek).first()
                val weekEntries = allWeekEntities.map { it.toDomain() }

                val summaryDoc = buildWeeklySummaryMap(startOfWeek, weekEntries, prefs.sadhakaName)
                val weekKey = startOfWeek.format(DateTimeFormatter.ISO_LOCAL_DATE)

                firestore.collection("users").document(currentUser.uid)
                    .collection("weekly_summaries").document(weekKey)
                    .set(summaryDoc, SetOptions.merge())
                    .await()

                // If current week in Asia/Kolkata, update root user directory summary
                val kolkataZone = java.time.ZoneId.of("Asia/Kolkata")
                val currentWeekSunday = DateUtils.getStartOfWeek(LocalDate.now(kolkataZone))
                if (startOfWeek == currentWeekSunday) {
                    val rootUpdate = mapOf(
                        "currentWeekStart" to weekKey,
                        "currentWeekScore" to (summaryDoc["weekTotalScore"] ?: 0),
                        "currentWeekPercentage" to (summaryDoc["percentage"] ?: 0),
                        "currentWeekUpdatedAt" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(currentUser.uid)
                        .set(rootUpdate, SetOptions.merge())
                        .await()
                }

                // Mark entities as synced locally
                for (entity in allWeekEntities) {
                    dao.markAsSynced(currentUser.uid, entity.date)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: SadhanaDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var debounceJob: Job? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val kolkataZone = java.time.ZoneId.of("Asia/Kolkata")

    /**
     * Checks for pending unsynced records on startup and enqueues durable background sync.
     */
    fun enqueueStartupSync() {
        val currentUser = auth.currentUser ?: return
        if (currentUser.uid.isBlank()) return

        scope.launch {
            try {
                val unsynced = dao.getUnsyncedEntries(currentUser.uid)
                if (unsynced.isNotEmpty()) {
                    enqueueBackgroundSync(currentUser.uid)
                }
            } catch (_: Exception) {
                // Ignore startup check failure; background retry remains available
            }
        }
    }

    /**
     * Schedules cloud sync with 10-second debounce after last user modification.
     */
    fun scheduleDebouncedSync(entry: SadhanaEntry) {
        val currentUser = auth.currentUser ?: return
        if (entry.userId != currentUser.uid) return

        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(10_000) // 10-second debounce
            executeSync(currentUser.uid, entry.date)
        }
    }

    suspend fun executeSync(userId: String, date: LocalDate) = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            enqueueBackgroundSync(userId)
            return@withContext
        }

        try {
            val startOfWeek = DateUtils.getStartOfWeek(date)
            val endOfWeek = startOfWeek.plusDays(6)
            val weekEntities = dao.getEntriesForRange(userId, startOfWeek, endOfWeek).first()
            val weekEntries = weekEntities.map { it.toDomain() }
            val prefs = userPreferencesRepository.userPreferencesFlow.first()

            val summaryDoc = buildWeeklySummaryMap(startOfWeek, weekEntries, prefs.sadhakaName)
            val weekKey = startOfWeek.format(DateTimeFormatter.ISO_LOCAL_DATE)

            firestore.collection("users").document(userId)
                .collection("weekly_summaries").document(weekKey)
                .set(summaryDoc, SetOptions.merge())
                .await()

            val currentWeekSunday = DateUtils.getStartOfWeek(LocalDate.now(kolkataZone))
            if (startOfWeek == currentWeekSunday) {
                val rootUpdate = mapOf(
                    "currentWeekStart" to weekKey,
                    "currentWeekScore" to (summaryDoc["weekTotalScore"] ?: 0),
                    "currentWeekPercentage" to (summaryDoc["percentage"] ?: 0),
                    "currentWeekUpdatedAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(userId)
                    .set(rootUpdate, SetOptions.merge())
                    .await()
            }

            for (entity in weekEntities) {
                dao.markAsSynced(userId, entity.date)
            }
        } catch (e: Exception) {
            enqueueBackgroundSync(userId)
        }
    }

    fun enqueueBackgroundSync(userId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag("sadhana_sync_$userId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sadhana_sync_worker",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/**
 * Builds the Firestore-compatible Map for a weekly summary document.
 */
fun buildWeeklySummaryMap(
    startOfWeek: LocalDate,
    entries: List<SadhanaEntry>,
    devoteeName: String
): Map<String, Any> {
    val entryMap = entries.associateBy { it.date }
    val weekDates = DateUtils.getWeekSundayStart(startOfWeek)

    var weekTotal = 0
    var pathanTotal = 0
    var sravanTotal = 0

    val daysMap = mutableMapOf<String, Any>()

    for (date in weekDates) {
        val entry = entryMap[date]
        val dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (entry != null) {
            val scores = SadhanaScorer.scoreEntry(entry)
            pathanTotal += entry.pathanMinutes
            sravanTotal += entry.sravanMinutes
            val dayTotal = scores.totalScore ?: 0
            weekTotal += dayTotal

            val japaScoreVal = scores.japaScore ?: 0
            daysMap[dateKey] = mapOf(
                "bedtimeCategory" to (entry.bedtimeCategory?.name ?: ""),
                "wakeUpCategory" to (entry.wakeUpCategory?.name ?: ""),
                "japaCategory" to (entry.japaCategory?.name ?: ""),
                "japaCompleted" to (entry.japaCompleted || entry.japaCategory != null),
                "japaScore" to japaScoreVal,
                "sixteenRoundsDoneByScore" to japaScoreVal,
                "roundsCompletedScore" to japaScoreVal,
                "bedScore" to (scores.bedScore ?: 0),
                "wakeScore" to (scores.wakeScore ?: 0),
                "napScore" to (scores.napScore ?: 0),
                "pathanScore" to (scores.pathanScore ?: 0),
                "sravanScore" to (scores.sravanScore ?: 0),
                "daySleepCategory" to (entry.daySleepCategory?.name ?: ""),
                "pathanMinutes" to entry.pathanMinutes,
                "sravanMinutes" to entry.sravanMinutes,
                "totalScore" to dayTotal
            )
        }
    }

    val maxScore = 1050
    val percentage = ((weekTotal.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f) * 100).toInt()

    return mapOf(
        "startOfWeek" to startOfWeek.format(DateTimeFormatter.ISO_LOCAL_DATE),
        "devoteeName" to devoteeName,
        "weekTotalScore" to weekTotal,
        "percentage" to percentage,
        "totalPathanMinutes" to pathanTotal,
        "totalSravanMinutes" to sravanTotal,
        "updatedAt" to System.currentTimeMillis(),
        "days" to daysMap
    )
}
