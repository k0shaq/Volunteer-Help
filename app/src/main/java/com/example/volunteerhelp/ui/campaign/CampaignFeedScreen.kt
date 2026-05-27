package com.example.volunteerhelp.ui.campaign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.volunteerhelp.model.Campaign
import com.example.volunteerhelp.model.FeedItem
import com.example.volunteerhelp.model.Report
import com.example.volunteerhelp.ui.components.CampaignCard
import com.example.volunteerhelp.ui.components.CampaignCategories
import com.example.volunteerhelp.ui.components.EmptyStateView
import com.example.volunteerhelp.ui.components.ErrorView
import com.example.volunteerhelp.ui.components.FeedFilterBar
import com.example.volunteerhelp.ui.components.ModernSearchBar
import com.example.volunteerhelp.ui.components.ReportCard
import com.example.volunteerhelp.viewmodel.CampaignFilter

@Composable
fun CampaignFeedScreen(
    campaigns: List<Campaign>,
    reports: List<Report>,
    filter: CampaignFilter,
    searchQuery: String,
    categoryFilter: String,
    errorMessage: String?,
    onCampaignClick: (String) -> Unit,
    onFilterSelected: (CampaignFilter) -> Unit,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = buildList {
        if (filter != CampaignFilter.REPORTS) addAll(campaigns.map { FeedItem.CampaignItem(it) })
        if (filter in listOf(CampaignFilter.ALL, CampaignFilter.REPORTS)) addAll(reports.map { FeedItem.ReportItem(it) })
    }.sortedByDescending { it.sortTime }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModernSearchBar(query = searchQuery, onQueryChange = onSearchChanged, placeholder = "Пошук за назвою, містом або областю")
        FeedFilterBar(selected = filter, onSelected = onFilterSelected)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(selected = categoryFilter.isBlank(), onClick = { onCategorySelected("") }, label = { Text("Усі категорії") })
            }
            items(CampaignCategories) { category ->
                FilterChip(selected = categoryFilter == category, onClick = { onCategorySelected(category) }, label = { Text(category) })
            }
        }
        errorMessage?.let { ErrorView(message = it) }
        if (items.isEmpty()) {
            EmptyStateView(title = "Стрічка поки тиха", message = "Тут з'являться збори, звіти та оновлення волонтерів.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { item ->
                    when (item) {
                        is FeedItem.CampaignItem -> "campaign_${item.campaign.id}"
                        is FeedItem.ReportItem -> "report_${item.report.id}"
                    }
                }) { item ->
                    when (item) {
                        is FeedItem.CampaignItem -> CampaignCard(campaign = item.campaign, onClick = { onCampaignClick(item.campaign.id) })
                        is FeedItem.ReportItem -> ReportCard(report = item.report, onOpenCampaign = onCampaignClick)
                    }
                }
            }
        }
    }
}
