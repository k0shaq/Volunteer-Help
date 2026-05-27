package com.example.volunteerhelp.ui.report

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
import com.example.volunteerhelp.ui.components.ErrorView
import com.example.volunteerhelp.ui.components.PrimaryButton

@Composable
fun AddReportScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onSubmit: (String, Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    var description by remember { mutableStateOf("") }
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
        Text(text = "Додати звіт", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                localError = null
            },
            label = { Text("Опис звіту") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5
        )
        PrimaryButton(text = if (imageUri == null) "Додати фото" else "Змінити фото", onClick = { launcher.launch("image/*") })
        imageUri?.let {
            AsyncImage(model = it, contentDescription = "Фото звіту", modifier = Modifier.fillMaxWidth().height(200.dp))
        }
        localError?.let { ErrorView(message = it) }
        errorMessage?.let { ErrorView(message = it) }
        PrimaryButton(
            text = "Додати звіт",
            isLoading = isLoading,
            onClick = {
                if (description.isBlank()) {
                    localError = "Опис не може бути порожнім"
                } else {
                    onSubmit(description, imageUri)
                }
            }
        )
    }
}
