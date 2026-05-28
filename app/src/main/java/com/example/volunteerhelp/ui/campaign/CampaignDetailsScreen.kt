package com.example.volunteerhelp.ui.campaign

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.volunteerhelp.model.Campaign
import com.example.volunteerhelp.model.CampaignStatus
import com.example.volunteerhelp.model.CampaignType
import com.example.volunteerhelp.model.Report
import com.example.volunteerhelp.model.User
import com.example.volunteerhelp.model.UserRole
import com.example.volunteerhelp.ui.components.CampaignStatusChip
import com.example.volunteerhelp.ui.components.CampaignTypeChip
import com.example.volunteerhelp.ui.components.CategoryChip
import com.example.volunteerhelp.ui.components.EmptyStateView
import com.example.volunteerhelp.ui.components.ErrorView
import com.example.volunteerhelp.ui.components.InfoCard
import com.example.volunteerhelp.ui.components.LoadingView
import com.example.volunteerhelp.ui.components.PrimaryButton
import com.example.volunteerhelp.ui.components.ReportCard
import com.example.volunteerhelp.ui.components.SecondaryButton
import com.example.volunteerhelp.ui.components.SectionHeader
import com.example.volunteerhelp.ui.components.UserAvatar
import com.example.volunteerhelp.ui.components.VerifiedBadge
import com.example.volunteerhelp.util.DateFormatter

@Composable
fun CampaignDetailsScreen(
    campaign: Campaign?,
    currentUser: User,
    reports: List<Report>,
    isLoading: Boolean,
    errorMessage: String?,
    onHelpClick: (String) -> Unit,
    onCloseCampaign: (String) -> Unit,
    onAddReport: (String) -> Unit,
    onVolunteerClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading && campaign == null) {
        LoadingView()
        return
    }
    if (campaign == null) {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            ErrorView(message = errorMessage ?: "Збір не знайдено")
        }
        return
    }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val isOwner = currentUser.id == campaign.volunteerId && currentUser.role == UserRole.VOLUNTEER.name
    val canHelp = currentUser.role == UserRole.DONOR.name && campaign.status == CampaignStatus.ACTIVE.name
    val canClose = isOwner && campaign.status == CampaignStatus.ACTIVE.name
    val canAddReport = isOwner && campaign.status in listOf(CampaignStatus.CLOSED.name, CampaignStatus.COMPLETED.name)
    val progress = if (campaign.targetAmount > 0) (campaign.currentAmount / campaign.targetAmount).coerceIn(0.0, 1.0).toFloat() else 0f

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(270.dp), contentAlignment = Alignment.Center) {
            if (!campaign.imageUrl.isNullOrBlank()) {
                AsyncImage(model = campaign.imageUrl, contentDescription = campaign.title, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            } else {
                EmptyStateView("Фото не додано", "Збір доступний без зображення.")
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            errorMessage?.let { ErrorView(message = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampaignStatusChip(campaign.status)
                CampaignTypeChip(campaign.type)
                CategoryChip(campaign.category)
            }
            Text(text = campaign.title, style = MaterialTheme.typography.headlineSmall)
            Text(text = campaign.description, style = MaterialTheme.typography.bodyLarge)
            Text(text = "${campaign.city}, ${campaign.region} · ${DateFormatter.format(campaign.createdAt)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.material3.Card(onClick = { onVolunteerClick(campaign.volunteerId) }, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(campaign.volunteerAvatarUrl, campaign.volunteerName)
                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(campaign.volunteerName.ifBlank { "Волонтер" }, style = MaterialTheme.typography.titleMedium)
                            VerifiedBadge(campaign.volunteerVerified)
                        }
                        Text(campaign.volunteerUsername.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Відкрити профіль")
                    }
                }
            }
            SectionHeader("Прогрес")
            if (campaign.type == CampaignType.FINANCIAL.name) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text("${campaign.currentAmount.toInt()} / ${campaign.targetAmount.toInt()} грн")
            } else {
                InfoCard("Матеріальна допомога", campaign.materialGoal.ifBlank { "Потрібна матеріальна допомога" })
                Text("Підтверджено допомог: ${campaign.currentAmount.toInt()}")
            }
            SectionHeader("Реквізити")
            InfoCard("Дані для допомоги", campaign.requisites.ifBlank { "Реквізити не вказано" })
            if (campaign.requisites.isNotBlank()) {
                SecondaryButton(
                    text = "Скопіювати реквізити",
                    onClick = {
                        clipboard.setText(AnnotatedString(campaign.requisites))
                        context.copyToSystemClipboard("VolunteerHelp requisites", campaign.requisites)
                        Toast.makeText(context, "Реквізити скопійовано", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            if (canHelp) PrimaryButton(text = "Я допоміг", onClick = { onHelpClick(campaign.id) })
            if (canClose) SecondaryButton(text = "Закрити збір", onClick = { onCloseCampaign(campaign.id) })
            if (canAddReport) PrimaryButton(text = "Додати звіт", onClick = { onAddReport(campaign.id) })
            SectionHeader("Звіти")
            if (reports.isEmpty()) {
                EmptyStateView("Звітів ще немає", "Після завершення волонтер додасть пост-звіт.")
            } else {
                reports.forEach { report ->
                    ReportCard(report = report, onOpenCampaign = {}, showCampaignLink = false)
                }
            }
        }
    }
}

private fun Context.copyToSystemClipboard(label: String, text: String) {
    val manager = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
}
