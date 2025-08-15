package com.example.photogallery.features.galleryScreen.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.photogallery.features.galleryScreen.GalleryViewModel
import com.example.photogallery.utils.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsSheet(
    viewModel: GalleryViewModel,
    photoUri: Uri,
    onClose: () -> Unit
){
    val subjects by viewModel.subjectsFlow.collectAsState(initial = emptyList())
    var newName by remember { mutableStateOf("") }
    val checkedIds by viewModel.subjectIdsForPhoto(photoUri).collectAsState(initial = emptySet())

    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
            Text(Strings.SUBJECTS)

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
                    subjects,
                    key = { it.id }
                ) { subject ->
                    val isChecked = subject.id in checkedIds
                    ListItem(
                        headlineContent = { Text(subject.name) },
                        trailingContent = {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { c ->
                                    viewModel.setPhotoInSubject(photoUri, subject.id, c)
                                }
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.End)
            ) {
                Text(Strings.CLOSE)
            }
        }
    }
}