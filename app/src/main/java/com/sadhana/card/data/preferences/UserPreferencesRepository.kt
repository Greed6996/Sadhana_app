package com.sadhana.card.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class UserPreferences(
    val sadhakaName: String = "",
    val counselorName: String = "",
    val isDedicationExpanded: Boolean = true
)

interface UserPreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>
    suspend fun updateSadhakaName(name: String)
    suspend fun updateCounselorName(name: String)
    suspend fun setDedicationExpanded(expanded: Boolean)
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
            UserPreferences(
                sadhakaName = preferences[PreferencesKeys.SADHAKA_NAME] ?: "",
                counselorName = preferences[PreferencesKeys.COUNSELOR_NAME] ?: "",
                isDedicationExpanded = preferences[PreferencesKeys.DEDICATION_EXPANDED] ?: true
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
}
