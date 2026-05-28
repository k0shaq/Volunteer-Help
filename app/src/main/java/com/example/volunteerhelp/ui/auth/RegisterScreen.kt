package com.example.volunteerhelp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.volunteerhelp.model.UserRole
import com.example.volunteerhelp.ui.components.ErrorView
import com.example.volunteerhelp.ui.components.PrimaryButton
import com.example.volunteerhelp.util.FormLimits
import com.example.volunteerhelp.viewmodel.AuthFormState

@Composable
fun RegisterScreen(
    state: AuthFormState,
    prefilledEmail: String,
    isEmailReadOnly: Boolean,
    onRegisterClick: (String, String, String, String, UserRole) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable(prefilledEmail) { mutableStateOf(prefilledEmail) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(UserRole.DONOR) }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Реєстрація", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                localError = null
            },
            label = { Text("Ім'я") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it.removePrefix("@")
                localError = null
            },
            label = { Text("Нікнейм") },
            prefix = { Text("@") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = email,
            onValueChange = {
                if (!isEmailReadOnly) {
                    email = it
                    localError = null
                }
            },
            readOnly = isEmailReadOnly,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                localError = null
            },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                localError = null
            },
            label = { Text("Підтвердіть пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Text(text = "Оберіть роль")
        UserRole.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = role == option, onClick = { role = option }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = role == option, onClick = { role = option })
                Text(text = if (option == UserRole.VOLUNTEER) "Волонтер" else "Благодійник")
            }
        }
        localError?.let { ErrorView(message = it) }
        state.errorMessage?.let { ErrorView(message = it) }
        PrimaryButton(
            text = "Зареєструватися",
            isLoading = state.isLoading,
            onClick = {
                when {
                    name.isBlank() -> localError = "Ім'я не може бути порожнім"
                    name.length > FormLimits.NAME_MAX -> localError = "Ім'я має містити не більше ${FormLimits.NAME_MAX} символів"
                    username.length !in FormLimits.USERNAME_MIN..FormLimits.USERNAME_MAX -> localError = "Нікнейм має містити від ${FormLimits.USERNAME_MIN} до ${FormLimits.USERNAME_MAX} символів"
                    !username.matches(Regex("^[A-Za-z0-9._]+$")) -> localError = "Нікнейм може містити латинські літери, цифри, крапку та _"
                    email.isBlank() -> localError = "Email не може бути порожнім"
                    password.length < 6 -> localError = "Пароль має містити щонайменше 6 символів"
                    password != confirmPassword -> localError = "Паролі не збігаються"
                    else -> onRegisterClick(name, username, email, password, role)
                }
            }
        )
        if (!isEmailReadOnly) {
            TextButton(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Уже є акаунт? Увійти")
            }
        }
    }
}
