package com.example.photogallery.features.galleryScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.photogallery.dbRoom.SubjectWithCount
import com.example.photogallery.features.galleryScreen.components.ConfirmDialog
import com.example.photogallery.features.galleryScreen.components.InfoDialog
import com.example.photogallery.utils.Strings

@Composable
fun SubjectsScreen(
    viewModel: GalleryViewModel,
    onOpenSubject: (Long, String) -> Unit
) {
    val items by viewModel.subjectsWithCount.collectAsStateWithLifecycle(initialValue = emptyList())
    var newName by remember { mutableStateOf(Strings.EMPTY_STRING) }
    var subjectToDelete by remember { mutableStateOf<SubjectWithCount?>(null) }
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = Strings.SUBJECTS)

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(Strings.NEW_SUBJECT_NAME) },
                singleLine = true
            )
            Button(onClick = {
                if (newName.isNotBlank()) {
                    viewModel.createSubject(newName)
                    newName = Strings.EMPTY_STRING
                }
            }) { Text(Strings.ADD) }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(
                items.size,
                key = { i -> items[i].id }
            ) { i ->
                val s = items[i]
                ListItem(
                    modifier = Modifier.clickable { onOpenSubject(s.id, s.name) },
                    headlineContent = { Text(s.name) },
                    supportingContent = { Text("${s.count} photos") },
                    trailingContent = {
                        IconButton(
                            onClick = { subjectToDelete = s }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = Strings.DELETE)
                        }
                    }
                )
                HorizontalDivider()
            }
        }

        if (subjectToDelete != null) {
            val s = subjectToDelete ?: return
            ConfirmDialog(
                title = "Delete subject",
                message = "Delete \"${s.name}\"? Mappings to photos will be removed.",
                confirmText = Strings.DELETE,
                dismissText = Strings.CANCEL,
                onConfirm = {
                    subjectToDelete = null
                    viewModel.deleteSubject(s.id)
                },
                onDismiss = { subjectToDelete = null }
            )
        }

        if(errorMessage != null) {
            InfoDialog(
                title = Strings.ERROR,
                message = errorMessage ?: Strings.EMPTY_STRING,
                buttonText = Strings.OKAY,
                onDismiss = { viewModel.clearError() }
            )
        }
    }
}