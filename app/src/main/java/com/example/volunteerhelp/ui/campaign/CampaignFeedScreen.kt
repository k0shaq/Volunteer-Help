package com.example.volunteerhelp.ui.campaign

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import com.example.volunteerhelp.ui.components.RegionDropdownField
import com.example.volunteerhelp.ui.components.ReportCard
import com.example.volunteerhelp.viewmodel.CampaignFilter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CampaignFeedScreen(
    campaigns: List<Campaign>,
    reports: List<Report>,
    filter: CampaignFilter,
    searchQuery: String,
    categoryFilter: String,
    regionFilter: String,
    followedUserIds: Set<String>,
    errorMessage: String?,
    onCampaignClick: (String) -> Unit,
    onFilterSelected: (CampaignFilter) -> Unit,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onRegionSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val refreshing = false
    val pullRefreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = onRefresh)
    val normalizedQuery = searchQuery.trim().lowercase()
    val visibleReports = reports.filter { report ->
        val filterAllowsReports = when (filter) {
            CampaignFilter.ALL, CampaignFilter.REPORTS -> true
            CampaignFilter.FOLLOWING -> report.volunteerId in followedUserIds
            else -> false
        }
        filterAllowsReports &&
            categoryFilter.isBlank() &&
            regionFilter.isBlank() &&
            (
                normalizedQuery.isBlank() ||
                    report.campaignTitle.lowercase().contains(normalizedQuery) ||
                    report.description.lowercase().contains(normalizedQuery) ||
                    report.volunteerName.lowercase().contains(normalizedQuery)
            )
    }
    val items = buildList {
        if (filter != CampaignFilter.REPORTS) addAll(campaigns.map { FeedItem.CampaignItem(it) })
        if (filter in listOf(CampaignFilter.ALL, CampaignFilter.REPORTS, CampaignFilter.FOLLOWING)) {
            addAll(visibleReports.map { FeedItem.ReportItem(it) })
        }
    }.sortedByDescending { it.sortTime }

    Box(modifier = modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ModernSearchBar(query = searchQuery, onQueryChange = onSearchChanged, placeholder = "Пошук за назвою, містом або областю")
            }
            item {
                FeedFilterBar(selected = filter, onSelected = onFilterSelected)
            }
            item {
                RegionDropdownField(
                    selectedRegion = regionFilter,
                    onRegionSelected = onRegionSelected,
                    includeAllOption = true,
                    label = "Фільтр за областю"
                )
            }
            item {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = categoryFilter.isBlank(), onClick = { onCategorySelected("") }, label = { Text("Усі категорії") })
                    }
                    items(CampaignCategories) { category ->
                        FilterChip(selected = categoryFilter == category, onClick = { onCategorySelected(category) }, label = { Text(category) })
                    }
                }
            }
            errorMessage?.let { error ->
                item { ErrorView(message = error) }
            }
            if (items.isEmpty()) {
                item {
                    EmptyStateView(title = "Стрічка поки тиха", message = "Поки немає дописів за вибраними фільтрами.")
                }
            } else {
                items(items, key = { item ->
                    when (item) {
                        is FeedItem.CampaignItem -> "campaign_${item.campaign.id}"
                        is FeedItem.ReportItem -> "report_${item.report.id}"
                    }
                }) { item ->
                    when (item) {
                        is FeedItem.CampaignItem -> CampaignCard(
                            campaign = item.campaign.copy(category = item.campaign.category.ifBlank { "Інше" }),
                            onClick = { onCampaignClick(item.campaign.id) }
                        )
                        is FeedItem.ReportItem -> ReportCard(report = item.report, onOpenCampaign = onCampaignClick)
                    }
                }
            }
        }
        PullRefreshIndicator(refreshing = refreshing, state = pullRefreshState, modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter))
    }
}
