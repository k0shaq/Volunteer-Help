package com.example.volunteerhelp

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cloudinary.android.MediaManager
import com.example.volunteerhelp.data.AuthRepository
import com.example.volunteerhelp.data.CloudinaryRepository
import com.example.volunteerhelp.data.FirestoreRepository
import com.example.volunteerhelp.navigation.AppNavigation
import com.example.volunteerhelp.ui.theme.VolunteerHelpTheme
import com.example.volunteerhelp.util.Constants
import com.example.volunteerhelp.viewmodel.AuthViewModel
import com.example.volunteerhelp.viewmodel.CampaignViewModel
import com.example.volunteerhelp.viewmodel.HelpRequestViewModel
import com.example.volunteerhelp.viewmodel.ProfileViewModel
import com.example.volunteerhelp.viewmodel.ReportViewModel

class VolunteerHelpApp : Application() {
    val authRepository by lazy { AuthRepository() }
    val firestoreRepository by lazy { FirestoreRepository() }
    val cloudinaryRepository by lazy { CloudinaryRepository(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        if (Constants.CLOUDINARY_CLOUD_NAME.isNotBlank()) {
            runCatching {
                MediaManager.init(this, hashMapOf("cloud_name" to Constants.CLOUDINARY_CLOUD_NAME))
            }
        }
    }
}

class VolunteerHelpViewModelFactory(
    private val app: VolunteerHelpApp
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(app.authRepository, app.firestoreRepository) as T
            }
            modelClass.isAssignableFrom(CampaignViewModel::class.java) -> {
                CampaignViewModel(app.firestoreRepository, app.cloudinaryRepository, app.authRepository) as T
            }
            modelClass.isAssignableFrom(HelpRequestViewModel::class.java) -> {
                HelpRequestViewModel(app.firestoreRepository, app.cloudinaryRepository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(app.firestoreRepository, app.authRepository, app.cloudinaryRepository) as T
            }
            modelClass.isAssignableFrom(ReportViewModel::class.java) -> {
                ReportViewModel(app.firestoreRepository, app.cloudinaryRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@Composable
fun VolunteerHelpRoot(app: VolunteerHelpApp = LocalContext.current.applicationContext as VolunteerHelpApp) {
    val factory = VolunteerHelpViewModelFactory(app)
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val campaignViewModel: CampaignViewModel = viewModel(factory = factory)
    val helpRequestViewModel: HelpRequestViewModel = viewModel(factory = factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = factory)
    val reportViewModel: ReportViewModel = viewModel(factory = factory)

    VolunteerHelpTheme {
        AppNavigation(
            authViewModel = authViewModel,
            campaignViewModel = campaignViewModel,
            helpRequestViewModel = helpRequestViewModel,
            profileViewModel = profileViewModel,
            reportViewModel = reportViewModel
        )
    }
}
