package com.sadhana.card.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadhana.card.data.preferences.CURRENT_TERMS_VERSION
import com.sadhana.card.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /**
     * null = loading preferences from disk (render splash)
     * false = terms not accepted or version is outdated (route to Terms screen)
     * true = terms accepted and valid (route to main app)
     */
    val isTermsAccepted: StateFlow<Boolean?> = userPreferencesRepository.userPreferencesFlow
        .map { it.isTermsAccepted }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val themeMode: StateFlow<com.sadhana.card.data.preferences.ThemeMode> = userPreferencesRepository.userPreferencesFlow
        .map { it.themeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = com.sadhana.card.data.preferences.ThemeMode.SYSTEM
        )

    suspend fun acceptTerms() {
        userPreferencesRepository.setTermsAccepted(true, CURRENT_TERMS_VERSION)
    }

    suspend fun setThemeMode(themeMode: com.sadhana.card.data.preferences.ThemeMode) {
        userPreferencesRepository.setThemeMode(themeMode)
    }
}
