package com.example.volunteerhelp.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.volunteerhelp.model.ProfileStats
import com.example.volunteerhelp.model.User
import com.example.volunteerhelp.model.UserRole
import com.example.volunteerhelp.ui.components.EmptyStateView
import com.example.volunteerhelp.ui.components.ErrorView
import com.example.volunteerhelp.ui.components.InfoCard
import com.example.volunteerhelp.ui.components.ModernSearchBar
import com.example.volunteerhelp.ui.components.PrimaryButton
import com.example.volunteerhelp.ui.components.ProfileHeader
import com.example.volunteerhelp.ui.components.ProfileStatsGrid
import com.example.volunteerhelp.ui.components.SecondaryButton
import com.example.volunteerhelp.ui.components.UserAvatar
import com.example.volunteerhelp.ui.components.VerifiedBadge
import com.example.volunteerhelp.util.DateFormatter

@Composable
fun ProfileScreen(
    user: User,
    stats: ProfileStats,
    isLoading: Boolean,
    errorMessage: String?,
    onEdit: () -> Unit,
    onVerify: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Активність", "Збори", "Звіти", "Статистика")
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            ProfileHeader(user = user, isOwnProfile = true, isFollowing = false, onEdit = onEdit, onFollowToggle = {})
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                errorMessage?.let { ErrorView(message = it) }
                if (user.role == UserRole.VOLUNTEER.name && !user.isVerified) {
                    InfoCard(
                        title = "Верифікація волонтера",
                        body = "У демо-версії фото не зберігається і не передається. Після вибору фото статус підтверджується імітаційно."
                    )
                    PrimaryButton(text = "Пройти demo-верифікацію", onClick = onVerify, isLoading = isLoading)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEachIndexed { index, tab ->
                        FilterChip(selected = selectedTab == index, onClick = { selectedTab = index }, label = { Text(tab) })
                    }
                }
                when (selectedTab) {
                    0 -> InfoCard("Остання активність", "Створено профіль ${DateFormatter.format(user.createdAt)}. Нові дії з'являтимуться тут.")
                    1 -> ProfileStatsGrid(stats = stats, role = user.role)
                    2 -> EmptyStateView("Звіти профілю", "Звіти показуються у стрічці та деталях зборів.")
                    else -> ProfileStatsGrid(stats = stats, role = user.role)
                }
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
    isFollowing: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onFollowToggle: () -> Unit,
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
            ProfileHeader(user = user, isOwnProfile = user.id == currentUser.id, isFollowing = isFollowing, onEdit = {}, onFollowToggle = onFollowToggle)
        }
        item {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                errorMessage?.let { ErrorView(message = it) }
                ProfileStatsGrid(stats = stats, role = user.role)
                InfoCard("Публічний профіль", "Тут видно соціальну інформацію, статистику та статус волонтера.")
            }
        }
    }
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

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Редагувати профіль", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = name, onValueChange = { name = it; localError = null }, label = { Text("Ім'я") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = username, onValueChange = { username = it.removePrefix("@"); localError = null }, label = { Text("Нікнейм") }, prefix = { Text("@") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = bio, onValueChange = { bio = it.take(160); localError = null }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Місто") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Область") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(text = "Avatar", onClick = { avatarLauncher.launch("image/*") }, modifier = Modifier.weight(1f))
            SecondaryButton(text = "Cover", onClick = { coverLauncher.launch("image/*") }, modifier = Modifier.weight(1f))
        }
        localError?.let { ErrorView(message = it) }
        errorMessage?.let { ErrorView(message = it) }
        PrimaryButton(
            text = "Зберегти",
            isLoading = isLoading,
            onClick = {
                when {
                    name.isBlank() -> localError = "Ім'я не може бути порожнім"
                    username.length !in 3..20 -> localError = "Нікнейм має містити від 3 до 20 символів"
                    !username.matches(Regex("^[A-Za-z0-9._]+$")) -> localError = "Нікнейм може містити латинські літери, цифри, крапку та _"
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
        ModernSearchBar(query = query, onQueryChange = onQueryChange, placeholder = "Пошук профілю, @username або email")
        errorMessage?.let { ErrorView(message = it) }
        if (users.isEmpty()) {
            EmptyStateView("Знайдіть людей", "Введіть ім'я, username, email, місто або область.")
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
                                Text(user.username.takeIf { it.isNotBlank() }?.let { "@$it" } ?: user.email)
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
        Text("Demo-верифікація волонтера", style = MaterialTheme.typography.headlineSmall)
        InfoCard("Без передачі документів", "У демо-версії фото не зберігається і не передається. Після вибору фото статус верифікації підтверджується імітаційно.")
        if (user.isVerified) {
            InfoCard("Верифіковано", "Ваш профіль уже має синю галочку.")
        } else {
            SecondaryButton(text = if (pickedUri == null) "Обрати фото паспорта" else "Фото обрано локально", onClick = { launcher.launch("image/*") })
            errorMessage?.let { ErrorView(message = it) }
            PrimaryButton(text = "Підтвердити верифікацію", enabled = pickedUri != null, isLoading = isLoading, onClick = onVerify)
        }
    }
}
