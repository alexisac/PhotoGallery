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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.photogallery.model.PhotoFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoScreen(
    viewModel: GalleryViewModel,
    startIndex: Int
) {
    val items by viewModel.images.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDuplicateConfirm by remember { mutableStateOf(false) }
    var showSaveConfirm by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(PhotoFilter.None) }
    var filtersExpanded by remember { mutableStateOf(false) }
    val isEditing = selectedFilter != PhotoFilter.None

    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { items.size }
    )

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No images")
        }
        return
    }

    Scaffold(
        bottomBar = {
            BottomAppBar {
                // Filter menu
                Box {
                    TextButton(onClick = { filtersExpanded = true }) { Text("Filters") }
                    DropdownMenu(
                        expanded = filtersExpanded,
                        onDismissRequest = { filtersExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = { selectedFilter = PhotoFilter.None; filtersExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sepia") },
                            onClick = { selectedFilter = PhotoFilter.Sepia; filtersExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Black & White") },
                            onClick = { selectedFilter = PhotoFilter.GrayScale; filtersExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Invert colors") },
                            onClick = { selectedFilter = PhotoFilter.Invert; filtersExpanded = false }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Save button
                if (isEditing) {
                    TextButton(onClick = { showSaveConfirm = true }) { Text("Save") }
                    TextButton(onClick = { selectedFilter = PhotoFilter.None }) { Text("Cancel") }
                }

                Spacer(Modifier.weight(1f))

                // Duplicate button
                IconButton(
                    onClick = { showDuplicateConfirm = true },
                    enabled = !isEditing
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isEditing
            ) { page ->
                val uri = items[page]
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Inside, // sau Fit
                    colorFilter = selectedFilter.toColorFilterOrNull()
                )
            }
        }
    }

    if (showDuplicateConfirm) {
        AlertDialog(
            onDismissRequest = { showDuplicateConfirm = false },
            title = { Text("Duplicate photo") },
            text = { Text("Are you sure you want to duplicate this photo?") },
            confirmButton = {
                TextButton(onClick = {
                    showDuplicateConfirm = false
                    val current = pagerState.currentPage
                    viewModel.duplicatePhoto(current) { newIndex ->
                        scope.launch {
                            // deschide noua poza cand apare in lista
                            pagerState.animateScrollToPage(newIndex)
                        }
                    }
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateConfirm = false }) { Text("No") }
            }
        )
    }

    if (showSaveConfirm) {
        AlertDialog(
            onDismissRequest = { showSaveConfirm = false },
            title = { Text("Save filtered photo") },
            text = { Text("Do you want to save a new photo with the current filter?") },
            confirmButton = {
                TextButton(onClick = {
                    showSaveConfirm = false
                    val current = pagerState.currentPage
                    viewModel.saveFilter(current, selectedFilter) { newIndex ->
                        selectedFilter = PhotoFilter.None
                        scope.launch { pagerState.animateScrollToPage(newIndex) }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PhotoFilter.toColorFilterOrNull(): ColorFilter? {
    val matrix = when (this) {
        PhotoFilter.None -> return null
        PhotoFilter.GrayScale -> ColorMatrix().apply { setToSaturation(0f) }
        PhotoFilter.Sepia -> ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f,     0f,     0f,     1f, 0f
        ))
        PhotoFilter.Invert -> ColorMatrix(floatArrayOf(
            -1f, 0f,  0f,  0f, 255f,
            0f,-1f, 0f,  0f, 255f,
            0f, 0f,-1f,  0f, 255f,
            0f, 0f, 0f,  1f,   0f
        ))
    }
    return ColorFilter.colorMatrix(matrix)
}
