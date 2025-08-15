package com.example.photogallery.features.galleryScreen.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.photogallery.utils.Strings

@Composable
fun PasswordDialog(
    title: String,
    needConfirm: Boolean = false,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var passwordConfirmed by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
                },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(Strings.PASSWORD) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                if (needConfirm) {
                    OutlinedTextField(
                        value = passwordConfirmed,
                        onValueChange = { passwordConfirmed = it },
                        label = { Text(Strings.CONFIRM_PASSWORD) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
                if (error != null) {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (password.isBlank()) {
                    error = "Password cannot be empty"
                    return@TextButton
                }
                if (needConfirm && password != passwordConfirmed) {
                    error = "Passwords do not match"
                    return@TextButton
                }
                val chars = password.toCharArray()
                onConfirm(chars)
                password = Strings.EMPTY_STRING
                passwordConfirmed = Strings.EMPTY_STRING
            }) { Text(Strings.OKAY) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.CANCEL) }
        }
    )
}