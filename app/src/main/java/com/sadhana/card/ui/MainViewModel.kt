package com.sadhana.card.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.sadhana.card.data.auth.AuthRepository
import com.sadhana.card.data.auth.AuthState
import com.sadhana.card.data.preferences.CURRENT_TERMS_VERSION
import com.sadhana.card.data.preferences.UserPreferencesRepository
import com.sadhana.card.data.update.UpdateConfig
import com.sadhana.card.data.update.UpdateManager
import com.sadhana.card.data.update.UpdateState
import com.sadhana.card.ui.auth.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val updateManager: UpdateManager? = null,
    private val authRepository: AuthRepository? = null,
    private val syncManager: com.sadhana.card.data.sync.SyncManager? = null
) : ViewModel() {

    // Secondary constructor for existing unit tests
    constructor(userPreferencesRepository: UserPreferencesRepository) : this(
        userPreferencesRepository = userPreferencesRepository,
        updateManager = null,
        authRepository = null,
        syncManager = null
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

    val authState: StateFlow<AuthState> = authRepository?.authState
        ?: MutableStateFlow(AuthState.Unauthenticated).asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _isCheckingStatus = MutableStateFlow(false)
    val isCheckingStatus: StateFlow<Boolean> = _isCheckingStatus.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    val authUiState: StateFlow<AuthUiState> = combine(
        authState,
        _isAuthLoading,
        _authErrorMessage
    ) { state, loading, error ->
        when {
            loading -> AuthUiState.Loading
            error != null -> AuthUiState.Error(error)
            state is AuthState.Authenticated -> {
                if (state.role.equals("counselor", ignoreCase = true) && !state.isCounselorApproved) {
                    AuthUiState.PendingApproval(
                        uid = state.uid,
                        email = state.email,
                        displayName = state.displayName,
                        role = "COUNSELOR"
                    )
                } else {
                    AuthUiState.Authenticated(
                        uid = state.uid,
                        email = state.email,
                        displayName = state.displayName,
                        role = state.role,
                        isApproved = state.isCounselorApproved
                    )
                }
            }
            state is AuthState.PendingApproval -> {
                AuthUiState.PendingApproval(
                    uid = state.uid,
                    email = state.email,
                    displayName = state.displayName,
                    role = state.role
                )
            }
            state is AuthState.Loading -> AuthUiState.Loading
            else -> AuthUiState.Idle
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AuthUiState.Idle
    )

    private val _showHistoricalSyncPrompt = MutableStateFlow(false)
    val showHistoricalSyncPrompt: StateFlow<Boolean> = _showHistoricalSyncPrompt.asStateFlow()

    private val _localRecordCount = MutableStateFlow(0)
    val localRecordCount: StateFlow<Int> = _localRecordCount.asStateFlow()

    private val _showCounselorPendingPrompt = MutableStateFlow(false)
    val showCounselorPendingPrompt: StateFlow<Boolean> = _showCounselorPendingPrompt.asStateFlow()

    init {
        // Automatic non-intrusive update check on app launch
        viewModelScope.launch {
            updateManager?.checkForUpdates(isManualCheck = false)
        }
        // Durable sync recovery on startup
        syncManager?.enqueueStartupSync()
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
            authRepository?.syncProfileToCloud(sadhaka, counselor)
        }
    }

    fun launchGoogleSignIn(context: Context, role: String) {
        viewModelScope.launch {
            val repo = authRepository ?: return@launch
            val webClientId = repo.getWebClientId()
            if (webClientId.isNullOrBlank()) {
                _authErrorMessage.value = "Google Web Client ID is not configured. Please ensure google-services.json has OAuth configured."
                return@launch
            }
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(request = request, context = context)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val authResult = repo.signInWithGoogleCredential(idToken, role)
                    authResult.fold(
                        onSuccess = { state ->
                            if (state is AuthState.Authenticated) {
                                if (state.role == "counselor" && !state.isCounselorApproved) {
                                    _showCounselorPendingPrompt.value = true
                                } else if (state.role == "devotee") {
                                    val prefs = userPreferencesRepository.userPreferencesFlow.first()
                                    val count = repo.getLocalRecordCount()
                                    if (!prefs.hasPromptedHistoricalSync && count > 0) {
                                        _localRecordCount.value = count
                                        _showHistoricalSyncPrompt.value = true
                                    }
                                }
                            }
                        },
                        onFailure = { error ->
                            _authErrorMessage.value = error.localizedMessage ?: "Failed to sign in with Google."
                        }
                    )
                } else {
                    _authErrorMessage.value = "Unexpected credential type received."
                }
            } catch (e: Exception) {
                if (e !is GetCredentialCancellationException) {
                    _authErrorMessage.value = e.localizedMessage ?: "Google sign-in failed."
                }
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun claimHistoricalRecords() {
        viewModelScope.launch {
            val currentUid = (authState.value as? AuthState.Authenticated)?.uid ?: return@launch
            authRepository?.claimLocalRecords(currentUid)
            _showHistoricalSyncPrompt.value = false
        }
    }

    fun skipHistoricalRecords() {
        viewModelScope.launch {
            authRepository?.setHistoricalSyncPrompted(true)
            _showHistoricalSyncPrompt.value = false
        }
    }

    fun dismissCounselorPendingPrompt() {
        _showCounselorPendingPrompt.value = false
    }

    fun clearAuthError() {
        _authErrorMessage.value = null
    }

    fun refreshAuthStatus() {
        viewModelScope.launch {
            _isCheckingStatus.value = true
            try {
                authRepository?.refreshAuthStatus()
            } finally {
                _isCheckingStatus.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository?.signOut()
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

