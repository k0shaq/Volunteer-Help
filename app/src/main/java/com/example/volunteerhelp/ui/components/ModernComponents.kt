package com.example.volunteerhelp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.volunteerhelp.model.Campaign
import com.example.volunteerhelp.model.CampaignStatus
import com.example.volunteerhelp.model.CampaignType
import com.example.volunteerhelp.model.ProfileStats
import com.example.volunteerhelp.model.Report
import com.example.volunteerhelp.model.User
import com.example.volunteerhelp.model.UserRole
import com.example.volunteerhelp.util.DateFormatter
import com.example.volunteerhelp.viewmodel.CampaignFilter

val CampaignCategories = listOf("Медицина", "Військове", "Тварини", "Їжа", "Одяг", "Транспорт", "Діти", "ВПО", "Інше")

@Composable
fun UserAvatar(url: String?, name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = name, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        } else {
            Text(text = name.trim().take(1).ifBlank { "V" }, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun VerifiedBadge(isVerified: Boolean, modifier: Modifier = Modifier) {
    if (isVerified) {
        Icon(Icons.Default.CheckCircle, contentDescription = "Верифіковано", tint = MaterialTheme.colorScheme.primary, modifier = modifier.size(18.dp))
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val cardModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Card(modifier = cardModifier, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyStateView(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (action != null && onAction != null) {
            Text(text = action, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

@Composable
fun InfoCard(title: String, body: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CampaignStatusChip(status: String) {
    val label = when (status) {
        CampaignStatus.ACTIVE.name -> "Активний"
        CampaignStatus.COMPLETED.name -> "Майже завершено"
        CampaignStatus.CLOSED.name -> "Завершено"
        else -> status
    }
    AssistChip(onClick = {}, label = { Text(label) })
}

@Composable
fun CampaignTypeChip(type: String) {
    AssistChip(onClick = {}, label = { Text(if (type == CampaignType.FINANCIAL.name) "Фінансова" else "Матеріальна") })
}

@Composable
fun CategoryChip(category: String) {
    if (category.isNotBlank()) AssistChip(onClick = {}, label = { Text(category) })
}

@Composable
fun FeedFilterBar(selected: CampaignFilter, onSelected: (CampaignFilter) -> Unit, modifier: Modifier = Modifier) {
    val filters = listOf(
        CampaignFilter.ALL to "Усе",
        CampaignFilter.CAMPAIGNS to "Збори",
        CampaignFilter.REPORTS to "Звіти",
        CampaignFilter.FINANCIAL to "Фінансова",
        CampaignFilter.MATERIAL to "Матеріальна",
        CampaignFilter.MY_REGION to "Мій регіон",
        CampaignFilter.FOLLOWING to "Стежу",
        CampaignFilter.ALMOST_FUNDED to "Майже зібрано",
        CampaignFilter.COMPLETED to "Завершені"
    )
    androidx.compose.foundation.lazy.LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters.size) { index ->
            val item = filters[index]
            FilterChip(selected = selected == item.first, onClick = { onSelected(item.first) }, label = { Text(item.second) })
        }
    }
}

@Composable
fun ModernSearchBar(query: String, onQueryChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        label = { Text(placeholder) },
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun CampaignCard(campaign: Campaign, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val progress = if (campaign.targetAmount > 0) (campaign.currentAmount / campaign.targetAmount).coerceIn(0.0, 1.0).toFloat() else 0f
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(190.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                if (!campaign.imageUrl.isNullOrBlank()) {
                    AsyncImage(model = campaign.imageUrl, contentDescription = campaign.title, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampaignTypeChip(campaign.type)
                    CategoryChip(campaign.category)
                    CampaignStatusChip(campaign.status)
                }
                Text(text = campaign.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(text = campaign.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserAvatar(campaign.volunteerAvatarUrl, campaign.volunteerName, Modifier.size(34.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = campaign.volunteerName.ifBlank { "Волонтер" }, style = MaterialTheme.typography.labelLarge)
                            VerifiedBadge(campaign.volunteerVerified)
                        }
                        Text(text = campaign.volunteerUsername.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "${campaign.city}, ${campaign.region}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (campaign.type == CampaignType.FINANCIAL.name) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Text(text = "${campaign.currentAmount.toInt()} / ${campaign.targetAmount.toInt()} грн", style = MaterialTheme.typography.labelMedium)
                } else {
                    Text(text = "Потрібно: ${campaign.materialGoal.ifBlank { "Матеріальна допомога" }}", style = MaterialTheme.typography.labelMedium)
                    Text(text = "Підтверджено допомог: ${campaign.currentAmount.toInt()}", style = MaterialTheme.typography.bodySmall)
                }
                Text(text = "${campaign.city}, ${campaign.region} · ${DateFormatter.format(campaign.createdAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ReportCard(report: Report, onOpenCampaign: (String) -> Unit, modifier: Modifier = Modifier, showCampaignLink: Boolean = true) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UserAvatar(report.volunteerAvatarUrl, report.volunteerName, Modifier.size(42.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = report.volunteerName.ifBlank { "Волонтер" }, fontWeight = FontWeight.SemiBold)
                        VerifiedBadge(report.volunteerVerified)
                    }
                    Text(text = report.volunteerUsername.takeIf { it.isNotBlank() }?.let { "@$it" }.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = DateFormatter.format(report.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = if (showCampaignLink) {
                    Modifier.fillMaxWidth().clickable { onOpenCampaign(report.campaignId) }
                } else {
                    Modifier.fillMaxWidth()
                }
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Звіт про збір", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = report.campaignTitle.ifBlank { "Відкрити збір" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            if (!report.imageUrl.isNullOrBlank()) {
                AsyncImage(model = report.imageUrl, contentDescription = "Звіт", modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            }
            Text(text = report.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ProfileHeader(
    user: User,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onEdit: () -> Unit,
    onFollowToggle: () -> Unit,
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Box(modifier = Modifier.fillMaxWidth().height(168.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.BottomStart) {
            if (!user.coverImageUrl.isNullOrBlank()) {
                AsyncImage(model = user.coverImageUrl, contentDescription = "Cover", modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            }
            UserAvatar(user.avatarUrl, user.name, Modifier.padding(16.dp).size(88.dp))
        }
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                VerifiedBadge(user.role == UserRole.VOLUNTEER.name && user.isVerified)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = user.username.takeIf { it.isNotBlank() }?.let { "@$it" } ?: user.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "·")
                Text(text = if (user.role == UserRole.VOLUNTEER.name) "Волонтер" else "Благодійник", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            if (user.bio.isNotBlank()) Text(text = user.bio)
            val location = listOf(user.city, user.region).filter { it.isNotBlank() }.joinToString(", ")
            if (location.isNotBlank()) {
                Text(text = location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Підписники", user.followersCount.toString(), Modifier.weight(1f), onFollowersClick)
                StatCard("Підписки", user.followingCount.toString(), Modifier.weight(1f), onFollowingClick)
                StatCard("Рейтинг", user.rating.toString(), Modifier.weight(1f))
            }
            if (isOwnProfile) {
                SecondaryButton(text = "Редагувати профіль", onClick = onEdit)
            } else {
                PrimaryButton(text = if (isFollowing) "Відписатися" else "Стежити", onClick = onFollowToggle)
            }
        }
    }
}

@Composable
fun ProfileStatsGrid(stats: ProfileStats, role: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Збори", stats.totalCampaigns.toString(), Modifier.weight(1f))
            StatCard("Звіти", stats.reportsCount.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(if (role == UserRole.DONOR.name) "Допомог" else "Підтверджень", (stats.approvedHelpCount + stats.approvedHelpRequestsCount).toString(), Modifier.weight(1f))
            StatCard("Сума", "${if (role == UserRole.DONOR.name) stats.totalDonatedAmount.toInt() else stats.totalRaisedAmount.toInt()} грн", Modifier.weight(1f))
        }
        if (role == UserRole.DONOR.name) InfoCard(stats.title.ifBlank { "Новачок допомоги" }, stats.level.ifBlank { "Рейтинг: ${stats.rating}" })
    }
}
