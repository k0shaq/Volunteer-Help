package com.example.volunteerhelp.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.volunteerhelp.model.Campaign
import com.example.volunteerhelp.model.ProfileStats
import com.example.volunteerhelp.model.Report
import com.example.volunteerhelp.model.User
import com.example.volunteerhelp.model.UserRole
import com.example.volunteerhelp.ui.components.CampaignCard
import com.example.volunteerhelp.ui.components.EmptyStateView
import com.example.volunteerhelp.ui.components.ErrorView
import com.example.volunteerhelp.ui.components.InfoCard
import com.example.volunteerhelp.ui.components.ModernSearchBar
import com.example.volunteerhelp.ui.components.PrimaryButton
import com.example.volunteerhelp.ui.components.ProfileHeader
import com.example.volunteerhelp.ui.components.RegionDropdownField
import com.example.volunteerhelp.ui.components.ReportCard
import com.example.volunteerhelp.ui.components.SecondaryButton
import com.example.volunteerhelp.ui.components.UserAvatar
import com.example.volunteerhelp.ui.components.VerifiedBadge
import com.example.volunteerhelp.util.FormLimits

@Composable
fun ProfileScreen(
    user: User,
    stats: ProfileStats,
    campaigns: List<Campaign>,
    reports: List<Report>,
    isLoading: Boolean,
    errorMessage: String?,
    onEdit: () -> Unit,
    onVerify: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onCampaignClick: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            ProfileHeader(
                user = user,
                isOwnProfile = true,
                isFollowing = false,
                onEdit = onEdit,
                onFollowToggle = {},
                onFollowersClick = onFollowersClick,
                onFollowingClick = onFollowingClick
            )
        }
        item {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                errorMessage?.let { ErrorView(message = it) }
                if (!isLoading && user.role == UserRole.VOLUNTEER.name && !user.isVerified) {
                    VerificationPrompt(isLoading = isLoading, onVerify = onVerify)
                }
                ProfileImpactSummary(stats = stats, role = user.role)
                ProfileSocialTabs(stats = stats, role = user.role, campaigns = campaigns, reports = reports, onCampaignClick = onCampaignClick)
                SecondaryButton(text = "Вийти", onClick = onLogout)
            }
        }
    }
}

@Composable
fun PublicProfileScreen(
    user: User?,
    currentUser: User,
    stats: ProfileStats,
    campaigns: List<Campaign>,
    reports: List<Report>,
    isFollowing: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onFollowToggle: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onCampaignClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (user == null) {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            if (isLoading) Text("Завантаження профілю...") else ErrorView(errorMessage ?: "Профіль не знайдено")
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            ProfileHeader(
                user = user,
                isOwnProfile = user.id == currentUser.id,
                isFollowing = isFollowing,
                onEdit = {},
                onFollowToggle = onFollowToggle,
                onFollowersClick = onFollowersClick,
                onFollowingClick = onFollowingClick
            )
        }
        item {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                errorMessage?.let { ErrorView(message = it) }
                ProfileImpactSummary(stats = stats, role = user.role)
                ProfileSocialTabs(stats = stats, role = user.role, campaigns = campaigns, reports = reports, onCampaignClick = onCampaignClick)
            }
        }
    }
}

@Composable
private fun ProfileSocialTabs(
    stats: ProfileStats,
    role: String,
    campaigns: List<Campaign>,
    reports: List<Report>,
    onCampaignClick: (String) -> Unit
) {
    val isVolunteer = role == UserRole.VOLUNTEER.name
    val tabs = if (isVolunteer) {
        listOf("Збори", "Звіти", "Допомога", "Про профіль")
    } else {
        listOf("Допомога", "Про профіль")
    }
    var selectedTab by rememberSaveable(role) { mutableStateOf(tabs.first()) }
    if (selectedTab !in tabs) selectedTab = tabs.first()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tabs.size) { index ->
                val tab = tabs[index]
                FilterChip(selected = selectedTab == tab, onClick = { selectedTab = tab }, label = { Text(tab) })
            }
        }
        when (selectedTab) {
            "Збори" -> {
                if (!isVolunteer) {
                    DonorHelpInfo(stats)
                } else if (campaigns.isEmpty()) {
                    EmptyStateView("Зборів ще немає", "Коли волонтер створить збір, він з'явиться тут.")
                } else {
                    campaigns.forEach { campaign ->
                        CampaignCard(
                            campaign = campaign.copy(category = campaign.category.ifBlank { "Інше" }),
                            onClick = { onCampaignClick(campaign.id) }
                        )
                    }
                }
            }
            "Звіти" -> {
                if (!isVolunteer) {
                    DonorHelpInfo(stats)
                } else if (reports.isEmpty()) {
                    EmptyStateView("Звітів ще немає", "Після додавання звіту він буде видно у профілі.")
                } else {
                    reports.forEach { report ->
                        ReportCard(report = report, onOpenCampaign = onCampaignClick)
                    }
                }
            }
            "Допомога" -> if (isVolunteer) VolunteerHelpInfo(stats) else DonorHelpInfo(stats)
            else -> InfoCard(
                title = "Про профіль",
                body = "${stats.title.ifBlank { "Профіль Aidly" }}. ${stats.level.ifBlank { "Соціальний профіль" }}."
            )
        }
    }
}

@Composable
private fun DonorHelpInfo(stats: ProfileStats) {
    InfoCard(
        title = "Благодійна активність",
        body = "Підтверджено допомог: ${stats.approvedHelpCount}. Очікує підтвердження: ${stats.pendingHelpCount}. Рейтинг: ${stats.rating}. ${stats.title.ifBlank { "Новачок допомоги" }}."
    )
}

@Composable
private fun VolunteerHelpInfo(stats: ProfileStats) {
    InfoCard(
        title = "Підтвердження допомоги",
        body = "Підтверджено заявок: ${stats.approvedHelpRequestsCount}. Очікують: ${stats.pendingHelpRequestsCount}."
    )
}

@Composable
private fun VerificationPrompt(isLoading: Boolean, onVerify: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Підтвердьте профіль", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Після верифікації біля профілю з'явиться позначка, а створення зборів стане доступним.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PrimaryButton(text = "Пройти верифікацію", onClick = onVerify, isLoading = isLoading)
        }
    }
}

@Composable
private fun ProfileImpactSummary(stats: ProfileStats, role: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = if (role == UserRole.VOLUNTEER.name) "Волонтерська активність" else "Добрі справи",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                if (role == UserRole.VOLUNTEER.name) {
                    CompactMetric("Збори", stats.totalCampaigns.toString(), Modifier.weight(1f))
                    CompactMetric("Звіти", stats.reportsCount.toString(), Modifier.weight(1f))
                    CompactMetric("Підтверджень", stats.approvedHelpRequestsCount.toString(), Modifier.weight(1f))
                } else {
                    CompactMetric("Допомог", stats.approvedHelpCount.toString(), Modifier.weight(1f))
                    CompactMetric("Очікує", stats.pendingHelpCount.toString(), Modifier.weight(1f))
                    CompactMetric("Рейтинг", stats.rating.toString(), Modifier.weight(1f))
                }
            }
            Text(
                text = if (role == UserRole.DONOR.name) {
                    "${stats.title.ifBlank { "Новачок допомоги" }} · ${stats.level.ifBlank { "Рейтинг ${stats.rating}" }}"
                } else {
                    "Зібрано ${stats.totalRaisedAmount.toInt()} грн · активних зборів ${stats.activeCampaigns}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FollowListDialog(
    title: String,
    users: List<User>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            when {
                isLoading -> Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
                users.isEmpty() -> EmptyStateView(title, "Список порожній.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users, key = { it.id }) { user ->
                        Card(onClick = { onOpenProfile(user.id) }, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                UserAvatar(user.avatarUrl, user.name)
                                Column(Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text(user.name, style = MaterialTheme.typography.titleMedium)
                                        VerifiedBadge(user.role == UserRole.VOLUNTEER.name && user.isVerified)
                                    }
                                    Text(user.username.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Профіль", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрити") } }
    )
}

@Composable
fun EditProfileScreen(
    user: User,
    isLoading: Boolean,
    errorMessage: String?,
    onSave: (String, String, String, String, String, Uri?, Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable(user.id) { mutableStateOf(user.name) }
    var username by rememberSaveable(user.id) { mutableStateOf(user.username) }
    var bio by rememberSaveable(user.id) { mutableStateOf(user.bio) }
    var city by rememberSaveable(user.id) { mutableStateOf(user.city) }
    var region by rememberSaveable(user.id) { mutableStateOf(user.region) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { avatarUri = it }
    val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { coverUri = it }
    val coverPreview: Any? = coverUri ?: user.coverImageUrl

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Редагувати профіль", style = MaterialTheme.typography.headlineSmall)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverPreview != null) {
                        AsyncImage(
                            model = coverPreview,
                            contentDescription = "Шапка профілю",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("Шапка профілю", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(avatarUri?.toString() ?: user.avatarUrl, user.name, Modifier.size(72.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Фото профілю", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                avatarUri != null && coverUri != null -> "Нове фото і шапку обрано"
                                avatarUri != null -> "Нове фото обрано"
                                coverUri != null -> "Нову шапку обрано"
                                else -> "Поточний вигляд профілю"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "Обрати фото", onClick = { avatarLauncher.launch("image/*") }, modifier = Modifier.weight(1f))
                    SecondaryButton(text = "Обрати шапку", onClick = { coverLauncher.launch("image/*") }, modifier = Modifier.weight(1f))
                }
            }
        }
        OutlinedTextField(value = name, onValueChange = { name = it; localError = null }, label = { Text("Ім'я") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = username, onValueChange = { username = it.removePrefix("@"); localError = null }, label = { Text("Нікнейм") }, prefix = { Text("@") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = bio, onValueChange = { bio = it; localError = null }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Місто") }, modifier = Modifier.fillMaxWidth())
        RegionDropdownField(
            selectedRegion = region,
            onRegionSelected = { region = it; localError = null },
            modifier = Modifier.fillMaxWidth(),
            isError = localError?.contains("область", ignoreCase = true) == true
        )
        localError?.let { ErrorView(message = it) }
        errorMessage?.let { ErrorView(message = it) }
        PrimaryButton(
            text = "Зберегти",
            isLoading = isLoading,
            onClick = {
                when {
                    name.isBlank() -> localError = "Ім'я не може бути порожнім"
                    name.length > FormLimits.NAME_MAX -> localError = "Ім'я має містити не більше ${FormLimits.NAME_MAX} символів"
                    username.length !in FormLimits.USERNAME_MIN..FormLimits.USERNAME_MAX -> localError = "Нікнейм має містити від ${FormLimits.USERNAME_MIN} до ${FormLimits.USERNAME_MAX} символів"
                    !username.matches(Regex("^[A-Za-z0-9._]+$")) -> localError = "Нікнейм може містити латинські літери, цифри, крапку та _"
                    bio.length > FormLimits.BIO_MAX -> localError = "Bio має містити не більше ${FormLimits.BIO_MAX} символів"
                    city.isBlank() -> localError = "Вкажіть місто"
                    city.length > FormLimits.CITY_MAX -> localError = "Місто має містити не більше ${FormLimits.CITY_MAX} символів"
                    region.isBlank() -> localError = "Вкажіть область"
                    else -> onSave(name, username, bio, city, region, avatarUri, coverUri)
                }
            }
        )
    }
}

@Composable
fun SearchProfilesScreen(
    query: String,
    users: List<User>,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernSearchBar(query = query, onQueryChange = onQueryChange, placeholder = "Пошук профілю, @username, місто або область")
        errorMessage?.let { ErrorView(message = it) }
        if (users.isEmpty()) {
            EmptyStateView("Знайдіть людей", "Введіть ім'я, username, місто або область.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(users, key = { it.id }) { user ->
                    androidx.compose.material3.Card(onClick = { onOpenProfile(user.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            UserAvatar(user.avatarUrl, user.name)
                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(user.name, style = MaterialTheme.typography.titleMedium)
                                    VerifiedBadge(user.role == UserRole.VOLUNTEER.name && user.isVerified)
                                }
                                Text(user.username.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Профіль")
                                Text(user.bio.ifBlank { listOf(user.city, user.region).filter { it.isNotBlank() }.joinToString(", ") }, maxLines = 1)
                            }
                            TextButton(onClick = { onOpenProfile(user.id) }) { Text("Відкрити") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VolunteerVerificationScreen(
    user: User,
    isLoading: Boolean,
    errorMessage: String?,
    onVerify: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { pickedUri = it }
    Column(modifier = modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Верифікація волонтера", style = MaterialTheme.typography.headlineSmall)
        InfoCard("Фото для підтвердження", "Оберіть фото документа або посвідчення. Після підтвердження акаунт отримає статус верифікованого.")
        if (user.isVerified) {
            InfoCard("Верифіковано", "Ваш профіль уже підтверджено.")
        } else {
            SecondaryButton(text = if (pickedUri == null) "Обрати фото" else "Змінити фото", onClick = { launcher.launch("image/*") })
            pickedUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Фото для верифікації",
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentScale = ContentScale.Crop
                )
            }
            errorMessage?.let { ErrorView(message = it) }
            PrimaryButton(text = "Підтвердити верифікацію", enabled = pickedUri != null, isLoading = isLoading, onClick = onVerify)
        }
    }
}
