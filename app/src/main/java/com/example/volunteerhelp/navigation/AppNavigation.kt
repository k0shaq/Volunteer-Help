package com.example.volunteerhelp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.volunteerhelp.model.UserRole
import com.example.volunteerhelp.ui.auth.LoginScreen
import com.example.volunteerhelp.ui.auth.RegisterScreen
import com.example.volunteerhelp.ui.campaign.CampaignDetailsScreen
import com.example.volunteerhelp.ui.campaign.CreateCampaignScreen
import com.example.volunteerhelp.ui.help.HelpFormScreen
import com.example.volunteerhelp.ui.main.MainScreen
import com.example.volunteerhelp.ui.profile.EditProfileScreen
import com.example.volunteerhelp.ui.profile.FollowListDialog
import com.example.volunteerhelp.ui.profile.PublicProfileScreen
import com.example.volunteerhelp.ui.profile.VolunteerVerificationScreen
import com.example.volunteerhelp.ui.report.AddReportScreen
import com.example.volunteerhelp.viewmodel.AuthViewModel
import com.example.volunteerhelp.viewmodel.CampaignViewModel
import com.example.volunteerhelp.viewmodel.HelpRequestViewModel
import com.example.volunteerhelp.viewmodel.ProfileViewModel
import com.example.volunteerhelp.viewmodel.ReportViewModel
import com.example.volunteerhelp.viewmodel.SessionState

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    campaignViewModel: CampaignViewModel,
    helpRequestViewModel: HelpRequestViewModel,
    profileViewModel: ProfileViewModel,
    reportViewModel: ReportViewModel
) {
    val navController = rememberNavController()
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val loginState by authViewModel.loginState.collectAsStateWithLifecycle()
    val registerState by authViewModel.registerState.collectAsStateWithLifecycle()
    val campaignState by campaignViewModel.uiState.collectAsStateWithLifecycle()
    val helpState by helpRequestViewModel.uiState.collectAsStateWithLifecycle()
    val reportState by reportViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sessionState) {
        when (val state = sessionState) {
            SessionState.Loading -> Unit
            SessionState.LoggedOut -> {
                campaignViewModel.clearSessionData()
                helpRequestViewModel.clearSessionData()
                profileViewModel.clearSessionData()
                reportViewModel.clearSessionData()
                navController.navigate(Screen.Login.route) {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: Screen.Splash.route
                    popUpTo(currentRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
            is SessionState.ProfileIncomplete -> {
                campaignViewModel.clearSessionData()
                helpRequestViewModel.clearSessionData()
                profileViewModel.clearSessionData()
                reportViewModel.clearSessionData()
                navController.navigate(Screen.Register.route) {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: Screen.Splash.route
                    popUpTo(currentRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
            is SessionState.Authenticated -> {
                campaignViewModel.observeFeedCampaigns()
                reportViewModel.observeFeedReports()
                profileViewModel.observeProfile(state.user.id)
                navController.navigate(Screen.Main.route) {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: Screen.Splash.route
                    popUpTo(currentRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(campaignState.successMessage, helpState.successMessage, reportState.successMessage) {
        listOf(
            campaignState.successMessage,
            helpState.successMessage,
            reportState.successMessage
        ).filterNotNull().firstOrNull()?.let { snackbarHostState.showSnackbar(it) }
    }

    profileState.followListTitle?.let { title ->
        FollowListDialog(
            title = title,
            users = profileState.followListUsers,
            isLoading = profileState.isFollowListLoading,
            onDismiss = profileViewModel::clearFollowList,
            onOpenProfile = { userId ->
                profileViewModel.clearFollowList()
                navController.navigate(Screen.PublicProfile.createRoute(userId))
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen()
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    state = loginState,
                    onLoginClick = { email, password -> authViewModel.login(email, password) },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Register.route) {
                val prefilledEmail = (sessionState as? SessionState.ProfileIncomplete)?.email.orEmpty()
                val isEmailReadOnly = sessionState is SessionState.ProfileIncomplete
                RegisterScreen(
                    state = registerState,
                    prefilledEmail = prefilledEmail,
                    isEmailReadOnly = isEmailReadOnly,
                    onRegisterClick = { name, username, email, password, role -> authViewModel.register(name, username, email, password, role) },
                    onNavigateToLogin = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Main.route) {
                val currentUser = profileState.user ?: (sessionState as? SessionState.Authenticated)?.user
                if (currentUser == null) {
                    SplashScreen()
                } else {
                    MainScreen(
                        currentUser = currentUser,
                        campaignViewModel = campaignViewModel,
                        helpRequestViewModel = helpRequestViewModel,
                        profileViewModel = profileViewModel,
                        reportViewModel = reportViewModel,
                        onCampaignClick = { campaignId -> navController.navigate(Screen.CampaignDetails.createRoute(campaignId)) },
                        onCreateCampaignClick = { navController.navigate(Screen.CreateCampaign.route) },
                        onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                        onVerifyClick = { navController.navigate(Screen.VolunteerVerification.route) },
                        onOpenProfile = { userId -> navController.navigate(Screen.PublicProfile.createRoute(userId)) },
                        onLogout = {
                            profileViewModel.signOut()
                            authViewModel.signOut()
                        }
                    )
                }
            }
            composable(
                route = Screen.CampaignDetails.route,
                arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
            ) { backStackEntry ->
                val campaignId = backStackEntry.arguments?.getString("campaignId").orEmpty()
                val currentUser = profileState.user ?: (sessionState as? SessionState.Authenticated)?.user
                LaunchedEffect(campaignId) {
                    campaignViewModel.observeCampaign(campaignId)
                    reportViewModel.observeReports(campaignId)
                }
                if (currentUser != null) {
                    CampaignDetailsScreen(
                        campaign = campaignState.selectedCampaign,
                        currentUser = currentUser,
                        reports = reportState.reports,
                        isLoading = campaignState.isLoading,
                        errorMessage = campaignState.errorMessage,
                        onHelpClick = { navController.navigate(Screen.HelpForm.createRoute(it)) },
                        onCloseCampaign = { campaignViewModel.closeCampaign(it, currentUser.id) },
                        onAddReport = { navController.navigate(Screen.AddReport.createRoute(it)) },
                        onVolunteerClick = { userId -> navController.navigate(Screen.PublicProfile.createRoute(userId)) }
                    )
                } else {
                    SplashScreen()
                }
            }
            composable(Screen.CreateCampaign.route) {
                val currentUser = profileState.user ?: (sessionState as? SessionState.Authenticated)?.user
                if (currentUser != null && currentUser.role == UserRole.VOLUNTEER.name) {
                    CreateCampaignScreen(
                        currentUser = currentUser,
                        isLoading = campaignState.isLoading,
                        errorMessage = campaignState.errorMessage,
                        onSubmit = { title, description, type, category, targetAmount, materialGoal, city, region, requisites, imageUri ->
                            campaignViewModel.createCampaign(currentUser, title, description, type, category, targetAmount, materialGoal, city, region, requisites, imageUri)
                        },
                        onVerifyClick = { navController.navigate(Screen.VolunteerVerification.route) }
                    )
                    LaunchedEffect(campaignState.successMessage) {
                        if (campaignState.successMessage != null) {
                            campaignViewModel.clearMessages()
                            navController.popBackStack()
                        }
                    }
                } else {
                    Text(text = "Доступ заборонено")
                }
            }
            composable(
                route = Screen.HelpForm.route,
                arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
            ) { _ ->
                val campaign = campaignState.selectedCampaign
                val currentUser = profileState.user ?: (sessionState as? SessionState.Authenticated)?.user
                if (campaign != null && currentUser != null && currentUser.role == UserRole.DONOR.name) {
                    HelpFormScreen(
                        campaign = campaign,
                        currentUser = currentUser,
                        isLoading = helpState.isLoading,
                        errorMessage = helpState.errorMessage,
                        onSubmit = { amount, comment, imageUri ->
                            helpRequestViewModel.createHelpRequest(campaign, currentUser, amount, comment, imageUri)
                        }
                    )
                    LaunchedEffect(helpState.successMessage) {
                        if (helpState.successMessage != null) {
                            helpRequestViewModel.clearMessages()
                            navController.popBackStack()
                        }
                    }
                } else {
                    Text(text = "Доступ заборонено")
                }
            }
            composable(
                route = Screen.AddReport.route,
                arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
            ) { backStackEntry ->
                val campaignId = backStackEntry.arguments?.getString("campaignId").orEmpty()
                val currentUser = profileState.user ?: (sessionState as? SessionState.Authenticated)?.user
                LaunchedEffect(campaignId) {
                    campaignViewModel.observeCampaign(campaignId)
                }
                if (currentUser != null && currentUser.role == UserRole.VOLUNTEER.name) {
                    AddReportScreen(
                        isLoading = reportState.isLoading,
                        errorMessage = reportState.errorMessage,
                        onSubmit = { description, imageUri ->
                            val campaign = campaignState.selectedCampaign ?: campaignViewModel.uiState.value.selectedCampaign
                            if (campaign != null) {
                                reportViewModel.createReport(campaign, currentUser, description, imageUri)
                            }
                        }
                    )
                    LaunchedEffect(reportState.successMessage) {
                        if (reportState.successMessage != null) {
                            reportViewModel.clearMessages()
                            navController.popBackStack()
                        }
                    }
                } else {
                    Text(text = "Доступ заборонено")
                }
            }
            composable(Screen.EditProfile.route) {
                val currentUser = profileState.user ?: (sessionState as? SessionState.Authenticated)?.user
                if (currentUser != null) {
                    EditProfileScreen(
                        user = currentUser,
                        isLoading = profileState.isLoading,
                        errorMessage = profileState.errorMessage,
                        onSave = { name, username, bio, city, region, avatarUri, coverUri ->
                            profileViewModel.updateProfile(currentUser, name, username, bio, city, region, avatarUri, coverUri)
                        }
                    )
                    LaunchedEffect(profileState.successMessage) {
                        if (profileState.successMessage != null) {
                            profileViewModel.clearMessages()
                            navController.popBackStack()
                        }
                    }
                } else {
                    SplashScreen()
                }
            }
            composable(Screen.VolunteerVerification.route) {
                val currentUser = profileState.user ?: (sessionState as? SessionState.Authenticated)?.user
                if (currentUser != null) {
                    VolunteerVerificationScreen(
                        user = currentUser,
                        isLoading = profileState.isLoading,
                        errorMessage = profileState.errorMessage,
                        onVerify = { profileViewModel.verifyVolunteer(currentUser) }
                    )
                    LaunchedEffect(profileState.successMessage) {
                        if (profileState.successMessage != null) {
                            profileViewModel.clearMessages()
                            navController.popBackStack()
                        }
                    }
                } else {
                    SplashScreen()
                }
            }
            composable(
                route = Screen.PublicProfile.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val targetUserId = backStackEntry.arguments?.getString("userId").orEmpty()
                val currentUser = profileState.user ?: (sessionState as? SessionState.Authenticated)?.user
                LaunchedEffect(targetUserId, currentUser?.id) {
                    if (currentUser != null && targetUserId.isNotBlank()) {
                        profileViewModel.observePublicProfile(currentUser.id, targetUserId)
                    }
                }
                if (currentUser != null) {
                    PublicProfileScreen(
                        user = profileState.publicUser,
                        currentUser = currentUser,
                        stats = profileState.stats,
                        campaigns = profileState.profileCampaigns,
                        reports = profileState.profileReports,
                        isFollowing = profileState.isFollowing,
                        isLoading = profileState.isLoading,
                        errorMessage = profileState.errorMessage,
                        onFollowToggle = {
                            val publicUser = profileState.publicUser
                            if (publicUser != null) {
                                if (profileState.isFollowing) {
                                    profileViewModel.unfollowUser(currentUser.id, publicUser.id)
                                } else {
                                    profileViewModel.followUser(currentUser.id, publicUser.id)
                                }
                            }
                        },
                        onFollowersClick = { profileViewModel.loadFollowers(targetUserId) },
                        onFollowingClick = { profileViewModel.loadFollowing(targetUserId) },
                        onCampaignClick = { campaignId -> navController.navigate(Screen.CampaignDetails.createRoute(campaignId)) }
                    )
                } else {
                    SplashScreen()
                }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
