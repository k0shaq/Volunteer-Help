package com.example.volunteerhelp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerhelp.data.AuthRepository
import com.example.volunteerhelp.data.CloudinaryRepository
import com.example.volunteerhelp.data.FirestoreRepository
import com.example.volunteerhelp.data.ResultState
import com.example.volunteerhelp.model.ProfileStats
import com.example.volunteerhelp.model.User
import com.example.volunteerhelp.model.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val publicUser: User? = null,
    val searchResults: List<User> = emptyList(),
    val followingIds: Set<String> = emptySet(),
    val followListUsers: List<User> = emptyList(),
    val followListTitle: String? = null,
    val isFollowListLoading: Boolean = false,
    val stats: ProfileStats = ProfileStats(),
    val isFollowing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val authRepository: AuthRepository,
    private val cloudinaryRepository: CloudinaryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var profileJob: Job? = null
    private var publicProfileJob: Job? = null
    private var followingIdsJob: Job? = null

    fun observeProfile(userId: String) {
        profileJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        profileJob = viewModelScope.launch {
            combine(
                firestoreRepository.observeUser(userId),
                firestoreRepository.observeUserRating(userId),
                firestoreRepository.observeFollowersCount(userId),
                firestoreRepository.observeFollowingCount(userId)
            ) { user, rating, followers, following ->
                user?.copy(
                    rating = if (user.role == UserRole.DONOR.name) rating else user.rating,
                    followersCount = followers,
                    followingCount = following
                )
            }
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update { it.copy(isLoading = false, user = null, errorMessage = throwable.message ?: "Не вдалося завантажити профіль") }
                }
                .collect { observedUser ->
                    _uiState.update { state ->
                        val user = if (
                            observedUser != null &&
                            state.user?.id == observedUser.id &&
                            state.user.isVerified &&
                            !observedUser.isVerified
                        ) {
                            observedUser.copy(isVerified = true, verifiedAt = state.user.verifiedAt)
                        } else {
                            observedUser
                        }
                        state.copy(isLoading = false, user = user, errorMessage = if (user == null) "Профіль не знайдено" else null)
                    }
                    observedUser?.let { loadStats(it.id, it.role) }
                }
        }
    }

    fun observePublicProfile(currentUserId: String, targetUserId: String) {
        publicProfileJob?.cancel()
        _uiState.update { it.copy(isLoading = true, publicUser = null, errorMessage = null) }
        publicProfileJob = viewModelScope.launch {
            firestoreRepository.observeUser(targetUserId)
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Не вдалося завантажити профіль") }
                }
                .collect { user ->
                    val following = if (user != null) firestoreRepository.isFollowing(currentUserId, targetUserId) else false
                    _uiState.update { it.copy(isLoading = false, publicUser = user, isFollowing = following) }
                    user?.let { loadStats(it.id, it.role) }
                }
        }
    }

    fun observeFollowingIds(userId: String) {
        followingIdsJob?.cancel()
        followingIdsJob = viewModelScope.launch {
            firestoreRepository.observeFollowingIds(userId)
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update { it.copy(followingIds = emptySet()) }
                }
                .collect { followingIds ->
                    _uiState.update { it.copy(followingIds = followingIds) }
                }
        }
    }

    fun updateProfile(
        user: User,
        name: String,
        username: String,
        bio: String,
        city: String,
        region: String,
        avatarUri: Uri?,
        coverUri: Uri?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                require(name.isNotBlank()) { "Ім'я не може бути порожнім" }
                val cleanUsername = username.trim().removePrefix("@")
                require(cleanUsername.length in 3..20) { "Нікнейм має містити від 3 до 20 символів" }
                require(cleanUsername.matches(Regex("^[A-Za-z0-9._]+$"))) { "Нікнейм може містити латинські літери, цифри, крапку та _" }
                require(bio.length <= 160) { "Bio має бути до 160 символів" }
                val avatarUrl = uploadOrKeep(avatarUri, user.avatarUrl)
                val coverUrl = uploadOrKeep(coverUri, user.coverImageUrl)
                val updated = user.copy(
                    name = name.trim(),
                    username = cleanUsername,
                    usernameLowercase = cleanUsername.lowercase(),
                    bio = bio.trim(),
                    city = city.trim(),
                    region = region.trim(),
                    avatarUrl = avatarUrl,
                    coverImageUrl = coverUrl
                )
                firestoreRepository.updateProfile(updated, user.usernameLowercase)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, successMessage = "Профіль оновлено") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Не вдалося оновити профіль") }
            }
        }
    }

    fun verifyVolunteer(user: User) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val verifiedAt = System.currentTimeMillis()
            val verifiedUser = user.copy(isVerified = true, verifiedAt = verifiedAt)
            runCatching { firestoreRepository.verifyVolunteer(user.id) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            user = if (state.user?.id == user.id || state.user == null) verifiedUser else state.user,
                            publicUser = state.publicUser?.let { if (it.id == user.id) verifiedUser else it },
                            successMessage = "Волонтера верифіковано"
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не вдалося пройти верифікацію. Перевірте Firestore rules для оновлення users/{uid}."
                        )
                    }
                }
        }
    }

    fun followUser(currentUserId: String, targetUserId: String) {
        viewModelScope.launch {
            runCatching { firestoreRepository.followUser(currentUserId, targetUserId) }
                .onSuccess { _uiState.update { it.copy(isFollowing = true) } }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = throwable.message ?: "Не вдалося підписатися") } }
        }
    }

    fun unfollowUser(currentUserId: String, targetUserId: String) {
        viewModelScope.launch {
            runCatching { firestoreRepository.unfollowUser(currentUserId, targetUserId) }
                .onSuccess { _uiState.update { it.copy(isFollowing = false) } }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = throwable.message ?: "Не вдалося відписатися") } }
        }
    }

    fun searchUsers(query: String, currentUserId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { firestoreRepository.searchUsers(query).filterNot { it.id == currentUserId } }
                .onSuccess { users -> _uiState.update { it.copy(isLoading = false, searchResults = users) } }
                .onFailure { throwable -> _uiState.update { it.copy(isLoading = false, searchResults = emptyList(), errorMessage = throwable.message ?: "Пошук не вдався") } }
        }
    }

    fun loadFollowers(userId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFollowListLoading = true,
                    followListTitle = "Підписники",
                    followListUsers = emptyList(),
                    errorMessage = null
                )
            }
            runCatching { firestoreRepository.getFollowers(userId) }
                .onSuccess { users ->
                    _uiState.update { it.copy(isFollowListLoading = false, followListUsers = users) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isFollowListLoading = false,
                            errorMessage = throwable.message ?: "Не вдалося завантажити підписників"
                        )
                    }
                }
        }
    }

    fun loadFollowing(userId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFollowListLoading = true,
                    followListTitle = "Підписки",
                    followListUsers = emptyList(),
                    errorMessage = null
                )
            }
            runCatching { firestoreRepository.getFollowing(userId) }
                .onSuccess { users ->
                    _uiState.update { it.copy(isFollowListLoading = false, followListUsers = users) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isFollowListLoading = false,
                            errorMessage = throwable.message ?: "Не вдалося завантажити підписки"
                        )
                    }
                }
        }
    }

    fun clearFollowList() {
        _uiState.update { it.copy(followListTitle = null, followListUsers = emptyList(), isFollowListLoading = false) }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchResults = emptyList()) }
    }

    fun loadStats(userId: String, role: String) {
        viewModelScope.launch {
            runCatching { firestoreRepository.getProfileStats(userId, role) }
                .onSuccess { stats -> _uiState.update { it.copy(stats = stats) } }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun signOut() {
        profileJob?.cancel()
        publicProfileJob?.cancel()
        followingIdsJob?.cancel()
        authRepository.signOut()
        _uiState.value = ProfileUiState()
    }

    fun clearSessionData() {
        profileJob?.cancel()
        publicProfileJob?.cancel()
        followingIdsJob?.cancel()
        _uiState.value = ProfileUiState()
    }

    private suspend fun uploadOrKeep(uri: Uri?, currentUrl: String?): String? {
        return when (val result = cloudinaryRepository.uploadImage(uri)) {
            is ResultState.Error -> throw IllegalStateException(result.message)
            is ResultState.Success -> result.data ?: currentUrl
            else -> currentUrl
        }
    }
}
