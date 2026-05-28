package com.example.volunteerhelp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerhelp.data.AuthRepository
import com.example.volunteerhelp.data.FirestoreRepository
import com.example.volunteerhelp.model.User
import com.example.volunteerhelp.model.UserRole
import com.example.volunteerhelp.util.FormLimits
import com.example.volunteerhelp.util.FormValidators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class ProfileIncomplete(val email: String) : SessionState
    data class Authenticated(val user: User) : SessionState
}

data class AuthFormState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _loginState = MutableStateFlow(AuthFormState())
    val loginState: StateFlow<AuthFormState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(AuthFormState())
    val registerState: StateFlow<AuthFormState> = _registerState.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _sessionState.value = SessionState.Loading
            val firebaseUser = authRepository.getCurrentUser()
            if (firebaseUser == null) {
                _sessionState.value = SessionState.LoggedOut
                return@launch
            }
            runCatching {
                firestoreRepository.getUser(firebaseUser.uid)
            }.onSuccess { user ->
                _sessionState.value = if (user != null) {
                    SessionState.Authenticated(user)
                } else {
                    SessionState.ProfileIncomplete(firebaseUser.email.orEmpty())
                }
            }.onFailure {
                _loginState.value = AuthFormState(errorMessage = it.message ?: "Не вдалося завантажити профіль")
                _sessionState.value = SessionState.LoggedOut
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthFormState(isLoading = true)
            runCatching {
                val firebaseUser = authRepository.signIn(email.trim(), password)
                firestoreRepository.getUser(firebaseUser.uid)
            }.onSuccess { user ->
                _loginState.value = AuthFormState()
                _sessionState.value = if (user != null) {
                    SessionState.Authenticated(user)
                } else {
                    SessionState.ProfileIncomplete(email.trim())
                }
            }.onFailure {
                _loginState.value = AuthFormState(errorMessage = it.message ?: "Помилка входу")
            }
        }
    }

    fun register(name: String, username: String, email: String, password: String, role: UserRole) {
        viewModelScope.launch {
            _registerState.value = AuthFormState(isLoading = true)
            runCatching {
                val cleanUsername = username.trim().removePrefix("@")
                validateUsernameFormat(cleanUsername)
                FormValidators.validateName(name.trim())
                val currentUser = authRepository.getCurrentUser()
                val firebaseUser = if (currentUser != null && currentUser.email.equals(email.trim(), ignoreCase = true)) {
                    currentUser
                } else {
                    authRepository.register(email.trim(), password)
                }
                if (!firestoreRepository.isUsernameAvailable(cleanUsername, firebaseUser.uid)) {
                    throw IllegalStateException("Такий нікнейм уже використовується")
                }
                val user = User(
                    id = firebaseUser.uid,
                    name = name.trim(),
                    nameLowercase = name.trim().lowercase(),
                    email = firebaseUser.email ?: email.trim(),
                    username = cleanUsername,
                    usernameLowercase = cleanUsername.lowercase(),
                    role = role.name,
                    avatarUrl = null,
                    rating = 0,
                    isVerified = false,
                    createdAt = System.currentTimeMillis()
                )
                firestoreRepository.createUser(user)
                user
            }.onSuccess { user ->
                _registerState.value = AuthFormState()
                _sessionState.value = SessionState.Authenticated(user)
            }.onFailure {
                _registerState.value = AuthFormState(errorMessage = it.message ?: "Помилка реєстрації")
            }
        }
    }

    fun clearLoginError() {
        _loginState.value = _loginState.value.copy(errorMessage = null)
    }

    fun clearRegisterError() {
        _registerState.value = _registerState.value.copy(errorMessage = null)
    }

    fun signOut() {
        authRepository.signOut()
        _sessionState.value = SessionState.LoggedOut
    }

    private suspend fun validateUsername(username: String) {
        validateUsernameFormat(username)
        if (!firestoreRepository.isUsernameAvailable(username)) {
            throw IllegalStateException("Такий нікнейм уже використовується")
        }
    }

    private fun validateUsernameFormat(username: String) {
        require(username.length in FormLimits.USERNAME_MIN..FormLimits.USERNAME_MAX) {
            "Нікнейм має містити від ${FormLimits.USERNAME_MIN} до ${FormLimits.USERNAME_MAX} символів"
        }
        require(username.matches(Regex("^[A-Za-z0-9._]+$"))) { "Нікнейм може містити латинські літери, цифри, крапку та _" }
    }
}
