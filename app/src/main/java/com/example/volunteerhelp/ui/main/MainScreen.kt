package com.example.volunteerhelp.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volunteerhelp.model.User
import com.example.volunteerhelp.model.UserRole
import com.example.volunteerhelp.ui.campaign.CampaignFeedScreen
import com.example.volunteerhelp.ui.help.PendingHelpRequestsScreen
import com.example.volunteerhelp.ui.profile.ProfileScreen
import com.example.volunteerhelp.ui.profile.SearchProfilesScreen
import com.example.volunteerhelp.viewmodel.CampaignViewModel
import com.example.volunteerhelp.viewmodel.HelpRequestViewModel
import com.example.volunteerhelp.viewmodel.ProfileViewModel
import com.example.volunteerhelp.viewmodel.ReportViewModel

private data class MainTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentUser: User,
    campaignViewModel: CampaignViewModel,
    helpRequestViewModel: HelpRequestViewModel,
    profileViewModel: ProfileViewModel,
    reportViewModel: ReportViewModel,
    onCampaignClick: (String) -> Unit,
    onCreateCampaignClick: () -> Unit,
    onEditProfile: () -> Unit,
    onVerifyClick: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onLogout: () -> Unit
) {
    val campaignState by campaignViewModel.uiState.collectAsStateWithLifecycle()
    val helpState by helpRequestViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val reportState by reportViewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showVerifyDialog by remember { mutableStateOf(false) }

    val tabs = remember(currentUser.role) {
        buildList {
            add(MainTab("Стрічка", Icons.Default.Home))
            add(MainTab("Пошук", Icons.Default.Search))
            if (currentUser.role == UserRole.VOLUNTEER.name) add(MainTab("Заявки", Icons.Default.Verified))
            add(MainTab("Профіль", Icons.Default.Person))
        }
    }
    var selectedTabIndex by rememberSaveable(currentUser.role) { mutableIntStateOf(0) }

    LaunchedEffect(currentUser.id, currentUser.role) {
        campaignViewModel.observeFeedCampaigns()
        reportViewModel.observeFeedReports()
        profileViewModel.observeProfile(currentUser.id)
        profileViewModel.observeFollowingIds(currentUser.id)
        if (currentUser.role == UserRole.VOLUNTEER.name) {
            campaignViewModel.observeMyCampaigns(currentUser.id)
            helpRequestViewModel.observePendingRequests(currentUser.id)
        } else {
            helpRequestViewModel.observeHistory(currentUser.id)
        }
    }

    LaunchedEffect(currentUser.region, profileState.followingIds) {
        campaignViewModel.setFeedContext(currentUser.region, profileState.followingIds)
    }

    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            title = { Text("Потрібна верифікація") },
            text = { Text("Спочатку потрібно пройти верифікацію волонтера.") },
            confirmButton = { TextButton(onClick = { showVerifyDialog = false; onVerifyClick() }) { Text("Перейти") } },
            dismissButton = { TextButton(onClick = { showVerifyDialog = false }) { Text("Пізніше") } }
        )
    }

    val currentTitle = tabs.getOrNull(selectedTabIndex)?.title ?: "Стрічка"
    val currentProfile = profileState.user ?: currentUser
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (currentUser.role == UserRole.VOLUNTEER.name && currentTitle == "Стрічка") {
                FloatingActionButton(
                    onClick = {
                        if (currentProfile.isVerified) onCreateCampaignClick() else showVerifyDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Створити збір")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTitle) {
            "Стрічка" -> CampaignFeedScreen(
                campaigns = campaignState.activeCampaigns,
                reports = reportState.feedReports,
                filter = campaignState.filter,
                searchQuery = campaignState.searchQuery,
                categoryFilter = campaignState.categoryFilter,
                followedUserIds = profileState.followingIds,
                errorMessage = campaignState.errorMessage,
                modifier = Modifier.padding(innerPadding),
                onCampaignClick = onCampaignClick,
                onFilterSelected = campaignViewModel::setFilter,
                onSearchChanged = campaignViewModel::setSearchQuery,
                onCategorySelected = campaignViewModel::setCategoryFilter
            )
            "Пошук" -> SearchProfilesScreen(
                query = searchQuery,
                users = profileState.searchResults,
                errorMessage = profileState.errorMessage,
                onQueryChange = {
                    searchQuery = it
                    if (it.isBlank()) profileViewModel.clearSearch() else profileViewModel.searchUsers(it, currentUser.id)
                },
                onOpenProfile = onOpenProfile,
                modifier = Modifier.padding(innerPadding)
            )
            "Заявки" -> PendingHelpRequestsScreen(
                requests = helpState.pendingRequests,
                isLoading = helpState.isLoading,
                errorMessage = helpState.errorMessage,
                modifier = Modifier.padding(innerPadding),
                onApprove = { helpRequestId -> helpRequestViewModel.approveRequest(helpRequestId, currentUser.id) },
                onReject = { helpRequestId -> helpRequestViewModel.rejectRequest(helpRequestId, currentUser.id) }
            )
            else -> ProfileScreen(
                user = currentProfile,
                stats = profileState.stats,
                isLoading = profileState.isLoading,
                errorMessage = profileState.errorMessage,
                modifier = Modifier.padding(innerPadding),
                onEdit = onEditProfile,
                onVerify = onVerifyClick,
                onFollowersClick = { profileViewModel.loadFollowers(currentProfile.id) },
                onFollowingClick = { profileViewModel.loadFollowing(currentProfile.id) },
                onLogout = onLogout
            )
        }
    }
}
