package com.sadhana.card.data.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sadhana.card.data.local.dao.SadhanaDao
import com.sadhana.card.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun signInWithGoogleCredential(idToken: String, requestedRole: String): Result<AuthState>
    suspend fun checkCounselorApproval(uid: String): Boolean
    suspend fun claimLocalRecords(googleUid: String)
    suspend fun getLocalRecordCount(): Int
    suspend fun setHistoricalSyncPrompted(prompted: Boolean)
    suspend fun signOut()
    suspend fun syncProfileToCloud(sadhakaName: String, counselorName: String)
    fun getWebClientId(): String?
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dao: SadhanaDao
) : AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        scope.launch {
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            val currentUser = auth.currentUser

            if (currentUser != null && prefs.authUid == currentUser.uid) {
                val isApproved = if (prefs.authRole == "counselor") {
                    checkCounselorApproval(currentUser.uid)
                } else {
                    false
                }
                _authState.value = AuthState.Authenticated(
                    uid = currentUser.uid,
                    email = currentUser.email ?: "",
                    displayName = prefs.sadhakaName.ifBlank { currentUser.displayName ?: "" },
                    role = prefs.authRole.ifBlank { "devotee" },
                    isCounselorApproved = isApproved
                )
            } else if (currentUser != null) {
                // Recover session from Firebase Auth directly
                val role = prefs.authRole.ifBlank { "devotee" }
                val isApproved = if (role == "counselor") checkCounselorApproval(currentUser.uid) else false
                userPreferencesRepository.updateAuthSession(
                    uid = currentUser.uid,
                    email = currentUser.email ?: "",
                    role = role,
                    isApproved = isApproved
                )
                _authState.value = AuthState.Authenticated(
                    uid = currentUser.uid,
                    email = currentUser.email ?: "",
                    displayName = prefs.sadhakaName.ifBlank { currentUser.displayName ?: "" },
                    role = role,
                    isCounselorApproved = isApproved
                )
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    override fun getWebClientId(): String? {
        val stringId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (stringId != 0) {
            context.getString(stringId)
        } else {
            null
        }
    }

    override suspend fun checkCounselorApproval(uid: String): Boolean = withContext(Dispatchers.IO) {
        val cachedApproval = userPreferencesRepository.userPreferencesFlow.first().isCounselorApproved
        try {
            val doc = firestore.collection("counselors").document(uid).get().await()
            val approved = doc.exists() && doc.getBoolean("approved") == true
            userPreferencesRepository.setCounselorApproved(approved)
            approved
        } catch (e: Exception) {
            // Preserve cached approval during transient network failures; do not revoke offline access
            cachedApproval
        }
    }

    override suspend fun signInWithGoogleCredential(
        idToken: String,
        requestedRole: String
    ): Result<AuthState> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in.")

            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            val finalDisplayName = prefs.sadhakaName.ifBlank { user.displayName ?: "Devotee" }

            val isApproved = if (requestedRole == "counselor") {
                checkCounselorApproval(user.uid)
            } else {
                false
            }

            // Sync user profile document in Firestore
            val userData = mutableMapOf<String, Any>(
                "uid" to user.uid,
                "displayName" to finalDisplayName,
                "email" to (user.email ?: ""),
                "role" to "devotee", // Client write is constrained to role = devotee by security rules
                "updatedAt" to System.currentTimeMillis()
            )
            if (requestedRole == "counselor") {
                userData["requestedRole"] = "counselor"
            }
            if (prefs.counselorName.isNotBlank()) {
                userData["counselorName"] = prefs.counselorName
            }

            firestore.collection("users").document(user.uid)
                .set(userData, SetOptions.merge())
                .await()

            userPreferencesRepository.updateAuthSession(
                uid = user.uid,
                email = user.email ?: "",
                role = requestedRole,
                isApproved = isApproved
            )

            val newState = AuthState.Authenticated(
                uid = user.uid,
                email = user.email ?: "",
                displayName = finalDisplayName,
                role = requestedRole,
                isCounselorApproved = isApproved
            )
            _authState.value = newState
            Result.success(newState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun claimLocalRecords(googleUid: String) = withContext(Dispatchers.IO) {
        dao.claimLocalRecords(googleUid)
        userPreferencesRepository.setHistoricalSyncPrompted(true)
    }

    override suspend fun getLocalRecordCount(): Int = withContext(Dispatchers.IO) {
        dao.getLocalRecordCount()
    }

    override suspend fun setHistoricalSyncPrompted(prompted: Boolean) = withContext(Dispatchers.IO) {
        userPreferencesRepository.setHistoricalSyncPrompted(prompted)
    }

    override suspend fun syncProfileToCloud(sadhakaName: String, counselorName: String) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        try {
            val updates = mapOf(
                "displayName" to sadhakaName,
                "counselorName" to counselorName,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(currentUser.uid)
                .set(updates, SetOptions.merge())
                .await()
        } catch (_: Exception) {
            // Silently ignore if offline
        }
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) {
        auth.signOut()
        userPreferencesRepository.clearAuthSession()
        _authState.value = AuthState.Unauthenticated
    }
}
