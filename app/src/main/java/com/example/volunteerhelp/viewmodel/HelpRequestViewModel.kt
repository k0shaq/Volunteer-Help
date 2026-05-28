package com.example.volunteerhelp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerhelp.data.CloudinaryRepository
import com.example.volunteerhelp.data.FirestoreRepository
import com.example.volunteerhelp.data.ResultState
import com.example.volunteerhelp.model.Campaign
import com.example.volunteerhelp.model.CampaignType
import com.example.volunteerhelp.model.HelpRequest
import com.example.volunteerhelp.model.HelpRequestStatus
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

data class HelpRequestUiState(
    val pendingRequests: List<HelpRequest> = emptyList(),
    val historyRequests: List<HelpRequest> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class HelpRequestViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val cloudinaryRepository: CloudinaryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HelpRequestUiState())
    val uiState: StateFlow<HelpRequestUiState> = _uiState.asStateFlow()

    private var pendingRequestsJob: Job? = null
    private var historyRequestsJob: Job? = null

    fun observePendingRequests(volunteerId: String) {
        pendingRequestsJob?.cancel()
        pendingRequestsJob = viewModelScope.launch {
            firestoreRepository.observePendingHelpRequests(volunteerId)
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(
                            pendingRequests = emptyList(),
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не вдалося завантажити заявки"
                        )
                    }
                }
                .collect { requests ->
                    _uiState.update { it.copy(pendingRequests = requests) }
                }
        }
    }

    fun observeHistory(donorId: String) {
        historyRequestsJob?.cancel()
        historyRequestsJob = viewModelScope.launch {
            firestoreRepository.observeDonorHelpRequests(donorId)
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(
                            historyRequests = emptyList(),
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не вдалося завантажити історію допомоги"
                        )
                    }
                }
                .collect { requests ->
                    _uiState.update { it.copy(historyRequests = requests) }
                }
        }
    }

    fun createHelpRequest(
        campaign: Campaign,
        donor: User,
        amountText: String,
        comment: String,
        screenshotUri: Uri?
    ) {
        if (donor.role != UserRole.DONOR.name) {
            _uiState.update { it.copy(errorMessage = "Лише благодійник може надсилати заявку") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val parsedAmount = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
            runCatching {
                FormValidators.validateMax(comment.trim(), FormLimits.HELP_REQUEST_COMMENT_MAX, "Коментар")
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Перевірте поля форми") }
                return@launch
            }
            val uploadResult = cloudinaryRepository.uploadImage(screenshotUri)
            if (uploadResult is ResultState.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = uploadResult.message) }
                return@launch
            }
            val helpRequest = HelpRequest(
                id = firestoreRepository.generateId(Constants.HELP_REQUESTS_COLLECTION),
                campaignId = campaign.id,
                campaignTitle = campaign.title,
                donorId = donor.id,
                donorName = donor.name,
                donorUsername = donor.username,
                donorAvatarUrl = donor.avatarUrl,
                volunteerId = campaign.volunteerId,
                type = campaign.type,
                amount = if (campaign.type == CampaignType.FINANCIAL.name) parsedAmount else 0.0,
                comment = comment.trim(),
                itemDescription = if (campaign.type == CampaignType.MATERIAL.name) comment.trim() else "",
                screenshotUrl = (uploadResult as? ResultState.Success)?.data,
                status = HelpRequestStatus.PENDING.name,
                createdAt = System.currentTimeMillis()
            )
            runCatching {
                firestoreRepository.createHelpRequest(helpRequest)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, successMessage = "Заявку надіслано") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Не вдалося створити заявку") }
            }
        }
    }

    fun approveRequest(helpRequestId: String, volunteerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                firestoreRepository.approveHelpRequest(helpRequestId, volunteerId)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, successMessage = "Допомогу підтверджено") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Не вдалося підтвердити допомогу") }
            }
        }
    }

    fun rejectRequest(helpRequestId: String, volunteerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                firestoreRepository.rejectHelpRequest(helpRequestId, volunteerId)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, successMessage = "Заявку відхилено") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Не вдалося відхилити заявку") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun clearSessionData() {
        pendingRequestsJob?.cancel()
        historyRequestsJob?.cancel()
        _uiState.value = HelpRequestUiState()
    }
}
