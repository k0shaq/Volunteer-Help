package com.example.volunteerhelp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.volunteerhelp.ui.components.ErrorView
import com.example.volunteerhelp.ui.components.PrimaryButton
import com.example.volunteerhelp.viewmodel.AuthFormState

@Composable
fun LoginScreen(
    state: AuthFormState,
    onLoginClick: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Aidly", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Увійдіть, щоб переглядати збори та координувати допомогу")
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                localError = null
            },
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
        localError?.let { ErrorView(message = it) }
        state.errorMessage?.let { ErrorView(message = it) }
        PrimaryButton(
            text = "Увійти",
            isLoading = state.isLoading,
            onClick = {
                when {
                    email.isBlank() -> localError = "Email не може бути порожнім"
                    password.length < 6 -> localError = "Пароль має містити щонайменше 6 символів"
                    else -> onLoginClick(email, password)
                }
            }
        )
        TextButton(onClick = onNavigateToRegister, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Створити акаунт")
        }
    }
}
