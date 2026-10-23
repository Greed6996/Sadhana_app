package com.sadhana.card.data.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    suspend fun refreshAuthStatus(): AuthState
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

    private var userDocListener: ListenerRegistration? = null

    init {
        scope.launch {
            // Restore session immediately from local DataStore preferences
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            if (prefs.authUid.isNotBlank()) {
                val isCounselorRole = prefs.authRole.equals("counselor", ignoreCase = true)
                _authState.value = if (isCounselorRole && !prefs.isCounselorApproved) {
                    AuthState.PendingApproval(
                        uid = prefs.authUid,
                        email = prefs.authEmail,
                        displayName = prefs.sadhakaName,
                        role = "COUNSELOR"
                    )
                } else {
                    AuthState.Authenticated(
                        uid = prefs.authUid,
                        email = prefs.authEmail,
                        displayName = prefs.sadhakaName,
                        role = if (isCounselorRole) "COUNSELOR" else "DEVOTEE",
                        isCounselorApproved = if (isCounselorRole) prefs.isCounselorApproved else true
                    )
                }
            } else if (auth.currentUser == null) {
                _authState.value = AuthState.Unauthenticated
            }
        }

        // Attach Firebase Auth state listener for robust persistent session tracking across launches
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                detachSnapshotListener()
                scope.launch {
                    val prefs = userPreferencesRepository.userPreferencesFlow.first()
                    if (prefs.authUid.isNotBlank()) {
                        userPreferencesRepository.clearAuthSession()
                    }
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                attachUserSnapshotListener(user.uid)
            }
        }
    }

    private fun detachSnapshotListener() {
        userDocListener?.remove()
        userDocListener = null
    }

    private fun attachUserSnapshotListener(uid: String) {
        detachSnapshotListener()
        userDocListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                scope.launch {
                    if (snapshot != null && snapshot.exists()) {
                        val roleRaw = snapshot.getString("role") ?: "DEVOTEE"
                        val isCounselorRole = roleRaw.equals("counselor", ignoreCase = true)
                        val status = snapshot.getString("status")?.uppercase() ?: ""
                        val isApprovedExplicit = snapshot.getBoolean("isApproved") == true

                        val isApproved = if (isCounselorRole) {
                            isApprovedExplicit || status == "APPROVED" || checkCounselorDoc(uid)
                        } else {
                            true
                        }

                        val currentUser = auth.currentUser
                        val email = snapshot.getString("email") ?: currentUser?.email ?: ""
                        val displayName = snapshot.getString("displayName") ?: currentUser?.displayName ?: "Devotee"

                        userPreferencesRepository.updateAuthSession(
                            uid = uid,
                            email = email,
                            role = if (isCounselorRole) "COUNSELOR" else "DEVOTEE",
                            isApproved = isApproved
                        )

                        _authState.value = if (isCounselorRole && !isApproved) {
                            AuthState.PendingApproval(
                                uid = uid,
                                email = email,
                                displayName = displayName,
                                role = "COUNSELOR"
                            )
                        } else {
                            AuthState.Authenticated(
                                uid = uid,
                                email = email,
                                displayName = displayName,
                                role = if (isCounselorRole) "COUNSELOR" else "DEVOTEE",
                                isCounselorApproved = isApproved
                            )
                        }
                    } else if (auth.currentUser != null) {
                        // User exists in Auth but document hasn't synced yet; preserve cached state
                        val prefs = userPreferencesRepository.userPreferencesFlow.first()
                        if (prefs.authUid == uid) {
                            val isCounselorRole = prefs.authRole.equals("counselor", ignoreCase = true)
                            _authState.value = if (isCounselorRole && !prefs.isCounselorApproved) {
                                AuthState.PendingApproval(uid = uid, email = prefs.authEmail, displayName = prefs.sadhakaName)
                            } else {
                                AuthState.Authenticated(
                                    uid = uid,
                                    email = prefs.authEmail,
                                    displayName = prefs.sadhakaName,
                                    role = if (isCounselorRole) "COUNSELOR" else "DEVOTEE",
                                    isCounselorApproved = if (isCounselorRole) prefs.isCounselorApproved else true
                                )
                            }
                        }
                    }
                }
            }
    }

    private suspend fun checkCounselorDoc(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("counselors").document(uid).get().await()
            doc.exists() && doc.getBoolean("approved") == true
        } catch (_: Exception) {
            false
        }
    }

    private val cachedWebClientId: String? by lazy {
        try {
            context.getString(com.sadhana.card.R.string.default_web_client_id)
        } catch (_: Exception) {
            val stringId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (stringId != 0) context.getString(stringId) else null
        }
    }

    override fun getWebClientId(): String? = cachedWebClientId

    override suspend fun checkCounselorApproval(uid: String): Boolean = withContext(Dispatchers.IO) {
        val cachedApproval = userPreferencesRepository.userPreferencesFlow.first().isCounselorApproved
        try {
            // 1. Check user profile document in /users/{uid}
            val userDoc = firestore.collection("users").document(uid).get().await()
            val approvedInUserDoc = userDoc.exists() && (
                userDoc.getBoolean("isApproved") == true ||
                userDoc.getString("status")?.equals("APPROVED", ignoreCase = true) == true
            )

            // 2. Fallback check /counselors/{uid}
            val approvedInCounselorDoc = checkCounselorDoc(uid)

            val finalApproval = approvedInUserDoc || approvedInCounselorDoc
            userPreferencesRepository.setCounselorApproved(finalApproval)
            finalApproval
        } catch (e: Exception) {
            // Preserve cached approval during transient network failures; do not revoke offline access
            cachedApproval
        }
    }

    override suspend fun refreshAuthStatus(): AuthState = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: run {
            _authState.value = AuthState.Unauthenticated
            return@withContext AuthState.Unauthenticated
        }
        val prefs = userPreferencesRepository.userPreferencesFlow.first()
        val isCounselorRole = prefs.authRole.equals("counselor", ignoreCase = true)

        val isApproved = if (isCounselorRole) {
            checkCounselorApproval(user.uid)
        } else {
            true
        }

        val state = if (isCounselorRole && !isApproved) {
            AuthState.PendingApproval(
                uid = user.uid,
                email = user.email ?: "",
                displayName = prefs.sadhakaName.ifBlank { user.displayName ?: "Counselor" },
                role = "COUNSELOR"
            )
        } else {
            AuthState.Authenticated(
                uid = user.uid,
                email = user.email ?: "",
                displayName = prefs.sadhakaName.ifBlank { user.displayName ?: "Devotee" },
                role = if (isCounselorRole) "COUNSELOR" else "DEVOTEE",
                isCounselorApproved = isApproved
            )
        }
        _authState.value = state
        state
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
            val isCounselorRequest = requestedRole.equals("counselor", ignoreCase = true)

            // Inspect if user document already exists in Firestore
            val userDocRef = firestore.collection("users").document(user.uid)
            val existingDoc = userDocRef.get().await()

            val isApproved: Boolean
            val normalizedRole: String

            if (existingDoc.exists()) {
                // Existing document: PRESERVE existing approval status (do not overwrite approved counselor to pending)
                val existingRole = existingDoc.getString("role")?.uppercase() ?: if (isCounselorRequest) "COUNSELOR" else "DEVOTEE"
                normalizedRole = existingRole

                val wasApproved = existingDoc.getBoolean("isApproved") == true ||
                    existingDoc.getString("status")?.equals("APPROVED", ignoreCase = true) == true ||
                    checkCounselorDoc(user.uid)

                isApproved = if (normalizedRole == "COUNSELOR") wasApproved else true

                val updates = mutableMapOf<String, Any>(
                    "displayName" to finalDisplayName,
                    "email" to (user.email ?: ""),
                    "updatedAt" to System.currentTimeMillis()
                )
                if (prefs.counselorName.isNotBlank() && !existingDoc.contains("counselorName")) {
                    updates["counselorName"] = prefs.counselorName
                }

                userDocRef.set(updates, SetOptions.merge()).await()
            } else {
                // First-time sign in: initialize document according to schema specification
                normalizedRole = if (isCounselorRequest) "COUNSELOR" else "DEVOTEE"
                isApproved = if (isCounselorRequest) false else true

                val profileData = mutableMapOf<String, Any>(
                    "uid" to user.uid,
                    "displayName" to finalDisplayName,
                    "email" to (user.email ?: ""),
                    "role" to normalizedRole,
                    "requestedRole" to normalizedRole,
                    "status" to (if (isCounselorRequest) "PENDING" else "APPROVED"),
                    "isApproved" to isApproved,
                    "updatedAt" to System.currentTimeMillis()
                )
                if (isCounselorRequest) {
                    profileData["requestedAt"] = System.currentTimeMillis()
                }
                if (prefs.counselorName.isNotBlank()) {
                    profileData["counselorName"] = prefs.counselorName
                }

                userDocRef.set(profileData, SetOptions.merge()).await()
            }

            userPreferencesRepository.updateAuthSession(
                uid = user.uid,
                email = user.email ?: "",
                role = normalizedRole,
                isApproved = isApproved
            )

            val newState = if (normalizedRole == "COUNSELOR" && !isApproved) {
                AuthState.PendingApproval(
                    uid = user.uid,
                    email = user.email ?: "",
                    displayName = finalDisplayName,
                    role = "COUNSELOR"
                )
            } else {
                AuthState.Authenticated(
                    uid = user.uid,
                    email = user.email ?: "",
                    displayName = finalDisplayName,
                    role = normalizedRole,
                    isCounselorApproved = isApproved
                )
            }

            _authState.value = newState
            attachUserSnapshotListener(user.uid)
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
        detachSnapshotListener()
        auth.signOut()
        userPreferencesRepository.clearAuthSession()
        _authState.value = AuthState.Unauthenticated
    }
}
