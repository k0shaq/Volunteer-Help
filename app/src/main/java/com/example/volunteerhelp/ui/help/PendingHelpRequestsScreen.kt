package com.example.volunteerhelp.ui.help

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.volunteerhelp.model.HelpRequest
import com.example.volunteerhelp.model.CampaignType
import com.example.volunteerhelp.model.HelpRequestStatus
import com.example.volunteerhelp.ui.components.ErrorView
import com.example.volunteerhelp.ui.components.LoadingView
import com.example.volunteerhelp.ui.components.UserAvatar
import com.example.volunteerhelp.util.DateFormatter

@Composable
fun PendingHelpRequestsScreen(
    requests: List<HelpRequest>,
    isLoading: Boolean,
    errorMessage: String?,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading && requests.isEmpty()) {
        LoadingView()
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        errorMessage?.let { ErrorView(message = it) }
        if (requests.isEmpty()) {
            Text(text = "Немає заявок на підтвердження")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(requests, key = { it.id }) { request ->
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = request.campaignTitle, style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenProfile(request.donorId) }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(request.donorAvatarUrl, request.donorName)
                                Column(Modifier.weight(1f)) {
                                    Text(request.donorName.ifBlank { "Благодійник" }, fontWeight = FontWeight.SemiBold)
                                    Text(request.donorUsername.takeIf { it.isNotBlank() }?.let { "@$it" }.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(text = if (request.type == CampaignType.FINANCIAL.name) "Сума: ${request.amount.toInt()} грн" else "Передано: ${request.itemDescription.ifBlank { request.comment }}")
                            Text(text = "Коментар: ${request.comment.ifBlank { "Не вказано" }}")
                            Text(text = "Дата: ${DateFormatter.format(request.createdAt)}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Статус: ${if (request.status == HelpRequestStatus.PENDING.name) "Очікує підтвердження" else request.status}", style = MaterialTheme.typography.bodySmall)
                            if (!request.screenshotUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = request.screenshotUrl,
                                    contentDescription = "Скріншот допомоги",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            }
                            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onApprove(request.id) }) {
                                    Text(text = "Підтвердити")
                                }
                                OutlinedButton(onClick = { onReject(request.id) }) {
                                    Text(text = "Відхилити")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
