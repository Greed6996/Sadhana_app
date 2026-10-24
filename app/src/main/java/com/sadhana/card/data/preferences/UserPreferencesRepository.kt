package com.sadhana.card.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

const val CURRENT_TERMS_VERSION = 2

data class UserPreferences(
    val sadhakaName: String = "",
    val counselorName: String = "",
    val isDedicationExpanded: Boolean = true,
    val acceptedTerms: Boolean = false,
    val acceptedTermsVersion: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dismissedUpdateVersion: Int = 0,
    val lastDismissedTimestamp: Long = 0L,
    val lastDismissedVersionCode: Int = 0,
    val authUid: String = "",
    val authEmail: String = "",
    val authRole: String = "",
    val isCounselorApproved: Boolean = false,
    val hasPromptedHistoricalSync: Boolean = false
) {
    val isTermsAccepted: Boolean
        get() = acceptedTerms && acceptedTermsVersion >= CURRENT_TERMS_VERSION

    // Compatibility alias
    val hasAcceptedTerms: Boolean
        get() = isTermsAccepted

    val isAuthenticated: Boolean
        get() = authUid.isNotBlank()

    val isCounselor: Boolean
        get() = authRole.equals("counselor", ignoreCase = true) && isCounselorApproved
}

interface UserPreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>
    suspend fun updateSadhakaName(name: String)
    suspend fun updateCounselorName(name: String)
    suspend fun setDedicationExpanded(expanded: Boolean)
    suspend fun setTermsAccepted(accepted: Boolean, version: Int = CURRENT_TERMS_VERSION)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setDismissedUpdateVersion(version: Int)
    suspend fun recordDismissedUpdate(versionCode: Int, timestamp: Long = System.currentTimeMillis())
    suspend fun updateAuthSession(uid: String, email: String, role: String, isApproved: Boolean) {}
    suspend fun clearAuthSession() {}
    suspend fun setHistoricalSyncPrompted(prompted: Boolean) {}
    suspend fun setCounselorApproved(approved: Boolean) {}
}

/**
 * Repository for user preferences backed by DataStore Preferences.
 */
@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val SADHAKA_NAME = stringPreferencesKey("sadhaka_name")
        val COUNSELOR_NAME = stringPreferencesKey("counselor_name")
        val DEDICATION_EXPANDED = booleanPreferencesKey("dedication_expanded")
        val ACCEPTED_TERMS = booleanPreferencesKey("acceptedTerms")
        val ACCEPTED_TERMS_VERSION = intPreferencesKey("acceptedTermsVersion")
        val THEME_MODE = stringPreferencesKey("themeMode")
        val DISMISSED_UPDATE_VERSION = intPreferencesKey("dismissed_update_version")
        val LAST_DISMISSED_TIMESTAMP = longPreferencesKey("last_dismissed_timestamp")
        val LAST_DISMISSED_VERSION_CODE = intPreferencesKey("last_dismissed_version_code")
        // Auth keys
        val AUTH_UID = stringPreferencesKey("auth_uid")
        val AUTH_EMAIL = stringPreferencesKey("auth_email")
        val AUTH_ROLE = stringPreferencesKey("auth_role")
        val IS_COUNSELOR_APPROVED = booleanPreferencesKey("is_counselor_approved")
        val HAS_PROMPTED_HISTORICAL_SYNC = booleanPreferencesKey("has_prompted_historical_sync")
        // Legacy fallback
        val LEGACY_HAS_ACCEPTED_TERMS = booleanPreferencesKey("has_accepted_terms")
    }

    override val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val accepted = preferences[PreferencesKeys.ACCEPTED_TERMS]
                ?: preferences[PreferencesKeys.LEGACY_HAS_ACCEPTED_TERMS]
                ?: false
            val version = preferences[PreferencesKeys.ACCEPTED_TERMS_VERSION] ?: if (accepted) 1 else 0
            val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM)
            val dismissedVersion = preferences[PreferencesKeys.LAST_DISMISSED_VERSION_CODE]
                ?: preferences[PreferencesKeys.DISMISSED_UPDATE_VERSION]
                ?: 0
            val dismissedTimestamp = preferences[PreferencesKeys.LAST_DISMISSED_TIMESTAMP] ?: 0L

            UserPreferences(
                sadhakaName = preferences[PreferencesKeys.SADHAKA_NAME] ?: "",
                counselorName = preferences[PreferencesKeys.COUNSELOR_NAME] ?: "",
                isDedicationExpanded = preferences[PreferencesKeys.DEDICATION_EXPANDED] ?: true,
                acceptedTerms = accepted,
                acceptedTermsVersion = version,
                themeMode = themeMode,
                dismissedUpdateVersion = dismissedVersion,
                lastDismissedTimestamp = dismissedTimestamp,
                lastDismissedVersionCode = dismissedVersion,
                authUid = preferences[PreferencesKeys.AUTH_UID] ?: "",
                authEmail = preferences[PreferencesKeys.AUTH_EMAIL] ?: "",
                authRole = preferences[PreferencesKeys.AUTH_ROLE] ?: "",
                isCounselorApproved = preferences[PreferencesKeys.IS_COUNSELOR_APPROVED] ?: false,
                hasPromptedHistoricalSync = preferences[PreferencesKeys.HAS_PROMPTED_HISTORICAL_SYNC] ?: false
            )
        }

    override suspend fun updateSadhakaName(name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SADHAKA_NAME] = name
        }
    }

    override suspend fun updateCounselorName(name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.COUNSELOR_NAME] = name
        }
    }

    override suspend fun setDedicationExpanded(expanded: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEDICATION_EXPANDED] = expanded
        }
    }

    override suspend fun setTermsAccepted(accepted: Boolean, version: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCEPTED_TERMS] = accepted
            preferences[PreferencesKeys.ACCEPTED_TERMS_VERSION] = version
        }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    override suspend fun setDismissedUpdateVersion(version: Int) {
        recordDismissedUpdate(version, System.currentTimeMillis())
    }

    override suspend fun recordDismissedUpdate(versionCode: Int, timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISMISSED_UPDATE_VERSION] = versionCode
            preferences[PreferencesKeys.LAST_DISMISSED_VERSION_CODE] = versionCode
            preferences[PreferencesKeys.LAST_DISMISSED_TIMESTAMP] = timestamp
        }
    }

    override suspend fun updateAuthSession(uid: String, email: String, role: String, isApproved: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_UID] = uid
            preferences[PreferencesKeys.AUTH_EMAIL] = email
            preferences[PreferencesKeys.AUTH_ROLE] = role
            preferences[PreferencesKeys.IS_COUNSELOR_APPROVED] = isApproved
        }
    }

    override suspend fun clearAuthSession() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_UID] = ""
            preferences[PreferencesKeys.AUTH_EMAIL] = ""
            preferences[PreferencesKeys.AUTH_ROLE] = ""
            preferences[PreferencesKeys.IS_COUNSELOR_APPROVED] = false
            preferences[PreferencesKeys.HAS_PROMPTED_HISTORICAL_SYNC] = false
        }
    }

    override suspend fun setHistoricalSyncPrompted(prompted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_PROMPTED_HISTORICAL_SYNC] = prompted
        }
    }

    override suspend fun setCounselorApproved(approved: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_COUNSELOR_APPROVED] = approved
        }
    }
}
