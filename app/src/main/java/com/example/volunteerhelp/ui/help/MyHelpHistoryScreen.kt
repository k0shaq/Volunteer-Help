package com.example.volunteerhelp.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.volunteerhelp.model.HelpRequest
import com.example.volunteerhelp.ui.components.ErrorView
import com.example.volunteerhelp.ui.components.LoadingView
import com.example.volunteerhelp.util.DateFormatter

@Composable
fun MyHelpHistoryScreen(
    requests: List<HelpRequest>,
    isLoading: Boolean,
    errorMessage: String?,
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
            Text(text = "Історія допомоги поки порожня")
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
                            Text(text = "Статус: ${request.status}")
                            Text(text = if (request.amount > 0) "Сума: ${request.amount}" else "Матеріальна допомога")
                            if (request.comment.isNotBlank()) {
                                Text(text = request.comment)
                            }
                            Text(text = DateFormatter.format(request.createdAt), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
