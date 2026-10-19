package com.sadhana.card.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadhana.card.data.preferences.CURRENT_TERMS_VERSION
import com.sadhana.card.data.preferences.UserPreferencesRepository
import com.sadhana.card.data.update.UpdateConfig
import com.sadhana.card.data.update.UpdateManager
import com.sadhana.card.data.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val updateManager: UpdateManager? = null
) : ViewModel() {

    // Secondary constructor for existing unit tests
    constructor(userPreferencesRepository: UserPreferencesRepository) : this(
        userPreferencesRepository = userPreferencesRepository,
        updateManager = null
    )

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

    val sadhakaName: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.sadhakaName }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    val counselorName: StateFlow<String> = userPreferencesRepository.userPreferencesFlow
        .map { it.counselorName }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    val updateState: StateFlow<UpdateState> = updateManager?.updateState
        ?: MutableStateFlow(UpdateState.Idle).asStateFlow()

    init {
        // Automatic non-intrusive update check on app launch
        viewModelScope.launch {
            updateManager?.checkForUpdates(isManualCheck = false)
        }
    }

    suspend fun acceptTerms() {
        userPreferencesRepository.setTermsAccepted(true, CURRENT_TERMS_VERSION)
    }

    suspend fun setThemeMode(themeMode: com.sadhana.card.data.preferences.ThemeMode) {
        userPreferencesRepository.setThemeMode(themeMode)
    }

    fun saveProfile(sadhaka: String, counselor: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateSadhakaName(sadhaka)
            userPreferencesRepository.updateCounselorName(counselor)
        }
    }

    fun checkForUpdates(isManualCheck: Boolean = true) {
        viewModelScope.launch {
            updateManager?.checkForUpdates(isManualCheck = isManualCheck)
        }
    }

    fun dismissUpdate(config: UpdateConfig) {
        viewModelScope.launch {
            updateManager?.dismissUpdate(config)
        }
    }

    fun downloadUpdate(config: UpdateConfig) {
        viewModelScope.launch {
            updateManager?.downloadUpdate(config)
        }
    }

    fun installUpdate(apkFile: File) {
        updateManager?.startInstall(apkFile)
    }

    fun resetUpdateState() {
        updateManager?.resetState()
    }
}
