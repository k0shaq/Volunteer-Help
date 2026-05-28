package com.example.volunteerhelp.ui.campaign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.volunteerhelp.model.Campaign
import com.example.volunteerhelp.model.CampaignStatus
import com.example.volunteerhelp.ui.components.CampaignCard
import com.example.volunteerhelp.ui.components.ErrorView
import com.example.volunteerhelp.ui.components.LoadingView

@Composable
fun MyCampaignsScreen(
    campaigns: List<Campaign>,
    isLoading: Boolean,
    errorMessage: String?,
    onCampaignClick: (String) -> Unit,
    onCloseCampaign: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading && campaigns.isEmpty()) {
        LoadingView()
        return
    }
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        errorMessage?.let { ErrorView(message = it) }
        if (campaigns.isEmpty()) {
            Text(text = "У вас ще немає зборів")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(campaigns, key = { it.id }) { campaign ->
                    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampaignCard(campaign = campaign, onClick = { onCampaignClick(campaign.id) })
                        if (CampaignStatus.canBeClosed(campaign.status)) {
                            OutlinedButton(onClick = { onCloseCampaign(campaign.id) }) {
                                Text(text = "Закрити збір")
                            }
                        }
                    }
                }
            }
        }
    }
}
