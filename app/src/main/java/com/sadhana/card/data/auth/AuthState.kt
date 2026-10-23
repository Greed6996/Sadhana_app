package com.sadhana.card.data.auth

sealed interface AuthState {
    object Unauthenticated : AuthState
    object Loading : AuthState
    data class Authenticated(
        val uid: String,
        val email: String,
        val displayName: String,
        val role: String, // "DEVOTEE" | "COUNSELOR"
        val isCounselorApproved: Boolean
    ) : AuthState
    data class PendingApproval(
        val uid: String,
        val email: String,
        val displayName: String,
        val role: String = "COUNSELOR"
    ) : AuthState
}
