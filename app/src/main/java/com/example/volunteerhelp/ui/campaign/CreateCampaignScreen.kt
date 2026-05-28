package com.example.volunteerhelp.ui.campaign

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.volunteerhelp.model.CampaignType
import com.example.volunteerhelp.model.User
import com.example.volunteerhelp.ui.components.CampaignCategories
import com.example.volunteerhelp.ui.components.EmptyStateView
import com.example.volunteerhelp.ui.components.ErrorView
import com.example.volunteerhelp.ui.components.InfoCard
import com.example.volunteerhelp.ui.components.PrimaryButton
import com.example.volunteerhelp.ui.components.RegionDropdownField
import com.example.volunteerhelp.ui.components.SectionHeader
import com.example.volunteerhelp.ui.components.SecondaryButton
import com.example.volunteerhelp.util.FormLimits

@Composable
fun CreateCampaignScreen(
    currentUser: User,
    isLoading: Boolean,
    errorMessage: String?,
    onSubmit: (String, String, CampaignType, String, Double, String, String, String, String, Uri?) -> Unit,
    onVerifyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!currentUser.isVerified) {
        Column(modifier = modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EmptyStateView("Потрібна верифікація", "Спочатку підтвердьте профіль волонтера, після цього створення зборів стане доступним.")
            PrimaryButton(text = "Перейти до верифікації", onClick = onVerifyClick)
        }
        return
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CampaignType.FINANCIAL) }
    var category by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var materialGoal by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var requisites by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imageUri = uri }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "Новий збір", style = MaterialTheme.typography.headlineSmall)
        InfoCard("Публікація у стрічці", "Додайте зрозумілу ціль, місто, область і категорію. Благодійники побачать збір у соціальній стрічці.")
        SectionHeader("Тип допомоги")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = type == CampaignType.FINANCIAL, onClick = { type = CampaignType.FINANCIAL }, label = { Text("Фінансова") })
            FilterChip(selected = type == CampaignType.MATERIAL, onClick = { type = CampaignType.MATERIAL }, label = { Text("Матеріальна") })
        }
        SectionHeader("Категорія")
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CampaignCategories.size) { index ->
                val item = CampaignCategories[index]
                FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) })
            }
        }
        OutlinedTextField(value = title, onValueChange = { title = it; localError = null }, label = { Text("Назва") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = description, onValueChange = { description = it; localError = null }, label = { Text("Опис") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
        OutlinedTextField(value = city, onValueChange = { city = it; localError = null }, label = { Text("Місто") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        RegionDropdownField(
            selectedRegion = region,
            onRegionSelected = { region = it; localError = null },
            modifier = Modifier.fillMaxWidth(),
            isError = localError?.contains("область", ignoreCase = true) == true
        )
        if (type == CampaignType.FINANCIAL) {
            OutlinedTextField(value = targetAmount, onValueChange = { targetAmount = it; localError = null }, label = { Text("Цільова сума, грн") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = requisites, onValueChange = { requisites = it }, label = { Text("Реквізити") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        } else {
            OutlinedTextField(value = materialGoal, onValueChange = { materialGoal = it; localError = null }, label = { Text("Що потрібно") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        }
        SecondaryButton(text = if (imageUri == null) "Обрати фото" else "Змінити фото", onClick = { launcher.launch("image/*") })
        imageUri?.let { AsyncImage(model = it, contentDescription = "Зображення збору", modifier = Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop) }
        localError?.let { ErrorView(message = it) }
        errorMessage?.let { ErrorView(message = it) }
        PrimaryButton(
            text = "Опублікувати збір",
            isLoading = isLoading,
            onClick = {
                val parsedTarget = targetAmount.replace(',', '.').toDoubleOrNull() ?: 0.0
                when {
                    title.isBlank() -> localError = "Назва не може бути порожньою"
                    title.length > FormLimits.CAMPAIGN_TITLE_MAX -> localError = "Назва збору має містити не більше ${FormLimits.CAMPAIGN_TITLE_MAX} символів"
                    description.isBlank() -> localError = "Опис не може бути порожнім"
                    description.length > FormLimits.CAMPAIGN_DESCRIPTION_MAX -> localError = "Опис збору має містити не більше ${FormLimits.CAMPAIGN_DESCRIPTION_MAX} символів"
                    city.isBlank() -> localError = "Вкажіть місто"
                    city.length > FormLimits.CITY_MAX -> localError = "Місто має містити не більше ${FormLimits.CITY_MAX} символів"
                    region.isBlank() -> localError = "Вкажіть область"
                    category.isBlank() -> localError = "Оберіть категорію"
                    type == CampaignType.FINANCIAL && parsedTarget <= 0.0 -> localError = "Для фінансового збору сума має бути більшою за 0"
                    requisites.length > FormLimits.REQUISITES_MAX -> localError = "Реквізити мають містити не більше ${FormLimits.REQUISITES_MAX} символів"
                    type == CampaignType.MATERIAL && materialGoal.isBlank() -> localError = "Опишіть, що саме потрібно"
                    materialGoal.length > FormLimits.MATERIAL_GOAL_MAX -> localError = "Матеріальна ціль має містити не більше ${FormLimits.MATERIAL_GOAL_MAX} символів"
                    else -> onSubmit(title, description, type, category, parsedTarget, materialGoal, city, region, requisites, imageUri)
                }
            }
        )
    }
}
