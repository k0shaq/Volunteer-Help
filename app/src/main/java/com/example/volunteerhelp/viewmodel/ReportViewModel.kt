package com.example.volunteerhelp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerhelp.data.CloudinaryRepository
import com.example.volunteerhelp.data.FirestoreRepository
import com.example.volunteerhelp.data.ResultState
import com.example.volunteerhelp.model.Campaign
import com.example.volunteerhelp.model.Report
import com.example.volunteerhelp.model.User
import com.example.volunteerhelp.util.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val reports: List<Report> = emptyList(),
    val feedReports: List<Report> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ReportViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val cloudinaryRepository: CloudinaryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private var reportsJob: Job? = null
    private var feedReportsJob: Job? = null

    fun observeReports(campaignId: String) {
        reportsJob?.cancel()
        reportsJob = viewModelScope.launch {
            firestoreRepository.observeReports(campaignId)
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(
                            reports = emptyList(),
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не вдалося завантажити звіти"
                        )
                    }
                }
                .collect { reports ->
                    _uiState.update { it.copy(reports = reports) }
                }
        }
    }

    fun observeFeedReports() {
        feedReportsJob?.cancel()
        feedReportsJob = viewModelScope.launch {
            firestoreRepository.observeAllReports()
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update { it.copy(feedReports = emptyList(), errorMessage = throwable.message ?: "Не вдалося завантажити звіти") }
                }
                .collect { reports ->
                    _uiState.update { it.copy(feedReports = reports) }
                }
        }
    }

    fun createReport(
        campaign: Campaign,
        volunteer: User,
        description: String,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val uploadResult = cloudinaryRepository.uploadImage(imageUri)
            if (uploadResult is ResultState.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = uploadResult.message) }
                return@launch
            }
            val report = Report(
                id = firestoreRepository.generateId(Constants.REPORTS_COLLECTION),
                campaignId = campaign.id,
                campaignTitle = campaign.title,
                volunteerId = volunteer.id,
                volunteerName = volunteer.name,
                volunteerUsername = volunteer.username,
                volunteerAvatarUrl = volunteer.avatarUrl,
                volunteerVerified = volunteer.isVerified,
                description = description.trim(),
                imageUrl = (uploadResult as? ResultState.Success)?.data,
                createdAt = System.currentTimeMillis()
            )
            runCatching {
                firestoreRepository.createReport(report)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, successMessage = "Звіт додано") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Не вдалося додати звіт") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun clearSessionData() {
        reportsJob?.cancel()
        feedReportsJob?.cancel()
        _uiState.value = ReportUiState()
    }
}
