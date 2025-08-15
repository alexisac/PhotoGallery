package com.example.photogallery.features.galleryScreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.photogallery.features.galleryScreen.components.PasswordDialog
import com.example.photogallery.utils.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onOpenPhoto: (Int) -> Unit = {},
    onOpenSubjects: () -> Unit
){
    val normalPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        viewModel.addUris(uris)
    }

    val items by viewModel.images.collectAsStateWithLifecycle()

    val encryptedPickerResult = remember { mutableStateListOf<Uri>() }
    var showFloatingActionButtonMenu by remember { mutableStateOf(false) }
    var showEncryptPassword by remember { mutableStateOf(false) }

    val encryptedPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        encryptedPickerResult.clear()
        encryptedPickerResult.addAll(uris)
        if (uris.isNotEmpty())
            showEncryptPassword = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.GALLERY) },
                actions = {
                    TextButton(onClick = onOpenSubjects) { Text(Strings.SUBJECTS) }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showFloatingActionButtonMenu = !showFloatingActionButtonMenu }
                ) {
                    Icon(Icons.Default.Add, contentDescription = Strings.ADD)
                }

                DropdownMenu(
                    expanded = showFloatingActionButtonMenu,
                    onDismissRequest = { showFloatingActionButtonMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.ADD_PHOTOS) },
                        onClick = {
                            showFloatingActionButtonMenu = false
                            normalPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.ADD_ENCRYPTED_PHOTOS) },
                        onClick = {
                            showFloatingActionButtonMenu = false
                            encryptedPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            items(items.size) { i ->
                val uri = items[i]
                val isEncrypted = uri.path?.endsWith(".enc", ignoreCase = true) == true
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenPhoto(i) }
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (isEncrypted) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) { Text(Strings.LOCK_ICON) }
                    }
                }
            }
        }
    }

    if (showEncryptPassword) {
        PasswordDialog(
            title = "Encrypt selected photos",
            needConfirm = true,
            onConfirm = { password ->
                // salveaza fiecare poza criptata
                encryptedPickerResult.forEach { u ->
                    viewModel.addEncryptedPhoto(u, password.copyOf())
                }
                password.fill('\u0000')
                encryptedPickerResult.clear()
                showEncryptPassword = false
            },
            onDismiss = {
                encryptedPickerResult.clear()
                showEncryptPassword = false
            }
        )
    }
}