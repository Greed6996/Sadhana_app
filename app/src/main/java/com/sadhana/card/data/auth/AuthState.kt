package com.sadhana.card.data.auth

sealed interface AuthState {
    object Unauthenticated : AuthState
    object Loading : AuthState
    data class Authenticated(
        val uid: String,
        val email: String,
        val displayName: String,
        val role: String, // "devotee" | "counselor"
        val isCounselorApproved: Boolean
    ) : AuthState
}
