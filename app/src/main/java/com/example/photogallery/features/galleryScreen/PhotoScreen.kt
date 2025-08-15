package com.example.photogallery.features.galleryScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photogallery.features.galleryScreen.components.FiltersMenu
import kotlinx.coroutines.launch
import com.example.photogallery.model.PhotoFilter
import com.example.photogallery.utils.Strings
import com.example.photogallery.features.galleryScreen.components.ConfirmDialog
import com.example.photogallery.features.galleryScreen.components.PasswordDialog
import com.example.photogallery.features.galleryScreen.components.SubjectsSheet
import com.example.photogallery.features.galleryScreen.utils.toColorFilterOrNull
import java.io.File
import javax.crypto.AEADBadTagException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoScreen(
    viewModel: GalleryViewModel,
    startIndex: Int,
    onClose: () -> Unit
) {
    val items by viewModel.images.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showDuplicateConfirm by remember { mutableStateOf(false) }
    var showSaveConfirm by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(PhotoFilter.None) }
    var filtersExpanded by remember { mutableStateOf(false) }
    var showSubjects by remember { mutableStateOf(false) }
    val isEditing = selectedFilter != PhotoFilter.None

    val pageBytes = remember { mutableStateMapOf<Int, ByteArray>() }
    var askPasswordForPage by remember { mutableStateOf<Int?>(null) }
    var decryptError by remember { mutableStateOf<String?>(null) }

    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { items.size }
    )

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(Strings.NO_IMAGES)
        }
        return
    }

    LaunchedEffect(
        pagerState.currentPage,
        items
    ) {
        val page = pagerState.currentPage
        val uri = items.getOrNull(page) ?: return@LaunchedEffect
        val path = uri.path ?: return@LaunchedEffect
        if (path.endsWith(".enc", true) && !pageBytes.containsKey(page)) {
            askPasswordForPage = page
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            BottomAppBar {
                // Filter menu
                Box {
                    TextButton(onClick = { filtersExpanded = true }) { Text(Strings.FILTERS) }
                    FiltersMenu(
                        expanded = filtersExpanded,
                        onDismiss = { filtersExpanded = false },
                        onSelect = { selectedFilter = it }
                    )
                }

                Spacer(Modifier.weight(1f))

                // Save button
                if (isEditing) {
                    TextButton(onClick = { showSaveConfirm = true }) { Text(Strings.SAVE) }
                    TextButton(onClick = {
                        selectedFilter = PhotoFilter.None
                    }) { Text(Strings.CANCEL) }
                }

                // Choose subjects button
                TextButton(onClick = { showSubjects = true }) { Text(Strings.SUBJECTS) }
                if (showSubjects) {
                    val currentUri = items.getOrNull(pagerState.currentPage)
                    if (currentUri != null) {
                        SubjectsSheet(
                            viewModel = viewModel,
                            photoUri = currentUri,
                            onClose = { showSubjects = false }
                        )
                    } else showSubjects = false
                }

                Spacer(Modifier.weight(1f))

                // Duplicate button
                IconButton(
                    onClick = { showDuplicateConfirm = true },
                    enabled = !isEditing
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = Strings.DUPLICATE)
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = askPasswordForPage == null && !isEditing
            ) { page ->
                val uri = items[page]
                val path = uri.path.orEmpty()
                val enc = path.endsWith(".enc", true)
                if (enc && pageBytes[page] == null) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Encrypted photo",
                            color = Color.White
                        )
                    }
                } else {
                    val model: Any = if (enc) {
                        ImageRequest.Builder(context)
                            .data(pageBytes[page])
                            .memoryCacheKey("dec-$path")
                            .build()
                    } else {
                        uri
                    }

                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (!enc) Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                                else Modifier
                            ),
                        contentScale = ContentScale.Inside,
                        colorFilter = selectedFilter.toColorFilterOrNull()
                    )
                }
            }
        }
    }

    if (showDuplicateConfirm) {
        ConfirmDialog(
            title = "Duplicate photo",
            message = "Are you sure you want to duplicate this photo?",
            onConfirm = {
                showDuplicateConfirm = false
                val current = pagerState.currentPage
                viewModel.duplicatePhoto(current) { newIndex ->
                    scope.launch {
                        // deschide noua poza cand apare in lista
                        pagerState.animateScrollToPage(newIndex)
                    }
                }
            },
            onDismiss = { showDuplicateConfirm = false }
        )
    }

    if (showSaveConfirm) {
        ConfirmDialog(
            title = "Save filtered photo",
            message = "Do you want to save a new photo with the current filter?",
            confirmText = Strings.SAVE,
            dismissText = Strings.CANCEL,
            onConfirm = {
                showSaveConfirm = false
                val current = pagerState.currentPage
                viewModel.saveFilter(current, selectedFilter) { newIndex ->
                    selectedFilter = PhotoFilter.None
                    scope.launch {
                        // deschide noua poza cand apare in lista
                        pagerState.animateScrollToPage(newIndex)
                    }
                }
            },
            onDismiss = { showSaveConfirm = false }
        )
    }

    if (askPasswordForPage != null) {
        val page = askPasswordForPage!!
        PasswordDialog(
            title = "Enter password",
            needConfirm = false,
            onConfirm = { password ->
                val uri = items.getOrNull(page)
                val file = uri?.path?.let { File(it) }
                if (file == null || !file.exists()) {
                    decryptError = "File not found"
                    askPasswordForPage = null
                    return@PasswordDialog
                }
                scope.launch {
                    try {
                        val bytes = viewModel.decryptPhoto(file, password)
                        pageBytes[page] = bytes
                        askPasswordForPage = null
                    } catch (e: AEADBadTagException) {
                        decryptError = "Wrong password."
                        askPasswordForPage = page
                    } catch (e: Exception) {
                        decryptError = "Wrong password or file corrupted."
                    } finally {
                        password.fill('\u0000')
                    }
                }
            },
            onDismiss = {
                onClose()
            }
        )
    }

    if (decryptError != null) {
        ConfirmDialog(
            title = Strings.ERROR,
            message = decryptError!!,
            confirmText = Strings.OKAY,
            dismissText = Strings.EMPTY_STRING,
            onConfirm = { decryptError = null },
            onDismiss = { decryptError = null }
        )
    }

}
