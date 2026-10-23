package com.sadhana.card.ui.auth

/**
 * Clean UI state representation for authentication and session status.
 */
sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Authenticated(
        val uid: String,
        val email: String,
        val displayName: String,
        val role: String,
        val isApproved: Boolean
    ) : AuthUiState
    data class PendingApproval(
        val uid: String,
        val email: String,
        val displayName: String,
        val role: String = "COUNSELOR"
    ) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
