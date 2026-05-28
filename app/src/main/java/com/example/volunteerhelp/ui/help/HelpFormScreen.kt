package com.example.volunteerhelp.ui.help

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.volunteerhelp.model.Campaign
import com.example.volunteerhelp.model.CampaignType
import com.example.volunteerhelp.model.User
import com.example.volunteerhelp.ui.components.ErrorView
import com.example.volunteerhelp.ui.components.PrimaryButton
import com.example.volunteerhelp.util.FormLimits

@Composable
fun HelpFormScreen(
    campaign: Campaign,
    currentUser: User,
    isLoading: Boolean,
    errorMessage: String?,
    onSubmit: (String, String, Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    var amount by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Я допоміг", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Збір: ${campaign.title}")
        Text(text = "Благодійник: ${currentUser.name}")
        if (campaign.type == CampaignType.FINANCIAL.name) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; localError = null },
                label = { Text("Сума") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it; localError = null },
            label = { Text("Опис допомоги") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )
        PrimaryButton(text = if (imageUri == null) "Додати фото або скріншот" else "Змінити фото", onClick = { launcher.launch("image/*") })
        imageUri?.let {
            AsyncImage(model = it, contentDescription = "Скріншот", modifier = Modifier.fillMaxWidth().height(200.dp))
        }
        localError?.let { ErrorView(message = it) }
        errorMessage?.let { ErrorView(message = it) }
        PrimaryButton(
            text = "Надіслати заявку",
            isLoading = isLoading,
            onClick = {
                val parsedAmount = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                when {
                    campaign.type == CampaignType.FINANCIAL.name && parsedAmount <= 0.0 -> localError = "Для фінансової допомоги сума має бути більшою за 0"
                    campaign.type == CampaignType.MATERIAL.name && comment.isBlank() -> localError = "Для матеріальної допомоги опис є обов'язковим"
                    comment.length > FormLimits.HELP_REQUEST_COMMENT_MAX -> localError = "Коментар має містити не більше ${FormLimits.HELP_REQUEST_COMMENT_MAX} символів"
                    else -> onSubmit(amount, comment, imageUri)
                }
            }
        )
    }
}
