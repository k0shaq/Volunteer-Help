package com.example.volunteerhelp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerhelp.data.AuthRepository
import com.example.volunteerhelp.data.CloudinaryRepository
import com.example.volunteerhelp.data.FirestoreRepository
import com.example.volunteerhelp.data.ResultState
import com.example.volunteerhelp.model.Campaign
import com.example.volunteerhelp.model.CampaignStatus
import com.example.volunteerhelp.model.CampaignType
import com.example.volunteerhelp.model.User
import com.example.volunteerhelp.model.UserRole
import com.example.volunteerhelp.util.Constants
import com.example.volunteerhelp.util.FormLimits
import com.example.volunteerhelp.util.FormValidators
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CampaignFilter {
    ALL,
    CAMPAIGNS,
    REPORTS,
    FINANCIAL,
    MATERIAL,
    MY_REGION,
    FOLLOWING,
    ALMOST_FUNDED,
    COMPLETED
}

data class CampaignUiState(
    val activeCampaigns: List<Campaign> = emptyList(),
    val myCampaigns: List<Campaign> = emptyList(),
    val selectedCampaign: Campaign? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val filter: CampaignFilter = CampaignFilter.ALL,
    val searchQuery: String = "",
    val categoryFilter: String = "",
    val regionFilter: String = ""
)

class CampaignViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val cloudinaryRepository: CloudinaryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CampaignUiState())
    val uiState: StateFlow<CampaignUiState> = _uiState.asStateFlow()

    private var activeCampaignsJob: Job? = null
    private var myCampaignsJob: Job? = null
    private var campaignDetailsJob: Job? = null
    private var allActiveCampaigns: List<Campaign> = emptyList()
    private var currentUserRegion: String = ""
    private var followedUserIds: Set<String> = emptySet()

    fun observeActiveCampaigns() {
        observeFeedCampaigns()
    }

    fun observeFeedCampaigns() {
        activeCampaignsJob?.cancel()
        _uiState.update { it.copy(errorMessage = null) }
        activeCampaignsJob = viewModelScope.launch {
            firestoreRepository.observeFeedCampaigns()
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    allActiveCampaigns = emptyList()
                    _uiState.update {
                        it.copy(
                            activeCampaigns = emptyList(),
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не вдалося завантажити збори"
                        )
                    }
                }
                .collect { campaigns ->
                    allActiveCampaigns = campaigns
                    applyFilter(_uiState.value.filter)
                }
        }
    }

    fun refreshFeedCampaigns() {
        observeFeedCampaigns()
    }

    fun setFilter(filter: CampaignFilter) {
        applyFilter(filter, _uiState.value.searchQuery, _uiState.value.categoryFilter, _uiState.value.regionFilter)
    }

    fun setSearchQuery(query: String) {
        applyFilter(_uiState.value.filter, query, _uiState.value.categoryFilter, _uiState.value.regionFilter)
    }

    fun setCategoryFilter(category: String) {
        applyFilter(_uiState.value.filter, _uiState.value.searchQuery, category, _uiState.value.regionFilter)
    }

    fun setRegionFilter(region: String) {
        applyFilter(_uiState.value.filter, _uiState.value.searchQuery, _uiState.value.categoryFilter, region)
    }

    fun setFeedContext(region: String, followingIds: Set<String>) {
        currentUserRegion = region.trim()
        followedUserIds = followingIds
        applyFilter(_uiState.value.filter)
    }

    private fun applyFilter(filter: CampaignFilter) {
        applyFilter(filter, _uiState.value.searchQuery, _uiState.value.categoryFilter, _uiState.value.regionFilter)
    }

    private fun applyFilter(filter: CampaignFilter, query: String, category: String, region: String) {
        val normalized = query.trim().lowercase()
        val filtered = allActiveCampaigns
            .filter { campaign ->
                normalized.isBlank() ||
                    campaign.title.lowercase().contains(normalized) ||
                    campaign.city.lowercase().contains(normalized) ||
                    campaign.region.lowercase().contains(normalized) ||
                    campaign.description.lowercase().contains(normalized)
            }
            .filter { category.isBlank() || campaignCategory(it.category) == category }
            .filter { region.isBlank() || it.region.equals(region, ignoreCase = true) }
            .filter {
                when (filter) {
                    CampaignFilter.ALL, CampaignFilter.CAMPAIGNS, CampaignFilter.REPORTS -> true
                    CampaignFilter.FINANCIAL -> it.type == CampaignType.FINANCIAL.name
                    CampaignFilter.MATERIAL -> it.type == CampaignType.MATERIAL.name
                    CampaignFilter.MY_REGION -> currentUserRegion.isBlank() || it.region.equals(currentUserRegion, ignoreCase = true)
                    CampaignFilter.FOLLOWING -> it.volunteerId in followedUserIds
                    CampaignFilter.ALMOST_FUNDED -> it.targetAmount > 0 && it.currentAmount / it.targetAmount >= 0.8
                    CampaignFilter.COMPLETED -> CampaignStatus.fromStorage(it.status) in listOf(
                        CampaignStatus.GOAL_REACHED,
                        CampaignStatus.CLOSED,
                        CampaignStatus.REPORTED
                    )
                }
            }
        _uiState.update {
            it.copy(activeCampaigns = filtered, filter = filter, searchQuery = query, categoryFilter = category, regionFilter = region)
        }
    }

    private fun campaignCategory(category: String): String = category.ifBlank { "Інше" }

    fun observeMyCampaigns(volunteerId: String) {
        myCampaignsJob?.cancel()
        myCampaignsJob = viewModelScope.launch {
            firestoreRepository.observeVolunteerCampaigns(volunteerId)
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(
                            myCampaigns = emptyList(),
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не вдалося завантажити ваші збори"
                        )
                    }
                }
                .collect { campaigns ->
                    _uiState.update { state -> state.copy(myCampaigns = campaigns) }
                }
        }
    }

    fun observeCampaign(campaignId: String) {
        campaignDetailsJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        campaignDetailsJob = viewModelScope.launch {
            firestoreRepository.observeCampaign(campaignId)
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(
                            selectedCampaign = null,
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не вдалося завантажити збір"
                        )
                    }
                }
                .collect { campaign ->
                    _uiState.update {
                        it.copy(
                            selectedCampaign = campaign,
                            isLoading = false,
                            errorMessage = if (campaign == null) "Збір не знайдено" else null
                        )
                    }
                }
        }
    }

    fun createCampaign(
        currentUser: User,
        title: String,
        description: String,
        type: CampaignType,
        category: String,
        targetAmount: Double,
        materialGoal: String,
        city: String,
        region: String,
        requisites: String,
        imageUri: Uri?
    ) {
        if (currentUser.role != UserRole.VOLUNTEER.name || authRepository.getCurrentUserId() != currentUser.id) {
            _uiState.update { it.copy(errorMessage = "Лише волонтер може створювати збір") }
            return
        }
        if (!currentUser.isVerified) {
            _uiState.update { it.copy(errorMessage = "Спочатку потрібно пройти верифікацію волонтера") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                FormValidators.validateMax(title.trim(), FormLimits.CAMPAIGN_TITLE_MAX, "Назва збору")
                FormValidators.validateMax(description.trim(), FormLimits.CAMPAIGN_DESCRIPTION_MAX, "Опис збору")
                FormValidators.validateCity(city.trim())
                require(region.isNotBlank()) { "Вкажіть область" }
                FormValidators.validateMax(requisites.trim(), FormLimits.REQUISITES_MAX, "Реквізити")
                FormValidators.validateMax(materialGoal.trim(), FormLimits.MATERIAL_GOAL_MAX, "Матеріальна ціль")
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Перевірте поля форми") }
                return@launch
            }
            val uploadResult = cloudinaryRepository.uploadImage(imageUri)
            if (uploadResult is ResultState.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = uploadResult.message) }
                return@launch
            }
            val imageUrl = (uploadResult as? ResultState.Success)?.data
            val campaign = Campaign(
                id = firestoreRepository.generateId(Constants.CAMPAIGNS_COLLECTION),
                title = title.trim(),
                description = description.trim(),
                type = type.name,
                category = category.ifBlank { "Інше" },
                targetAmount = if (type == CampaignType.FINANCIAL) targetAmount else 0.0,
                currentAmount = 0.0,
                materialGoal = if (type == CampaignType.MATERIAL) materialGoal.trim() else "",
                city = city.trim(),
                region = region.trim(),
                imageUrl = imageUrl,
                requisites = requisites.trim(),
                volunteerId = currentUser.id,
                volunteerName = currentUser.name,
                volunteerUsername = currentUser.username,
                volunteerAvatarUrl = currentUser.avatarUrl,
                volunteerVerified = currentUser.isVerified,
                status = CampaignStatus.ACTIVE.name,
                createdAt = System.currentTimeMillis()
            )
            runCatching {
                firestoreRepository.createCampaign(campaign)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Збір створено успішно"
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Не вдалося створити збір"
                    )
                }
            }
        }
    }

    fun closeCampaign(campaignId: String, volunteerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                firestoreRepository.closeCampaign(campaignId, volunteerId)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, successMessage = "Збір закрито") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Не вдалося закрити збір") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun clearSessionData() {
        activeCampaignsJob?.cancel()
        myCampaignsJob?.cancel()
        campaignDetailsJob?.cancel()
        allActiveCampaigns = emptyList()
        currentUserRegion = ""
        followedUserIds = emptySet()
        _uiState.value = CampaignUiState()
    }
}
