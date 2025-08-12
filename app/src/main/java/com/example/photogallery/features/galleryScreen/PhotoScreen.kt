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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.photogallery.features.galleryScreen.components.FiltersMenu
import kotlinx.coroutines.launch
import com.example.photogallery.model.PhotoFilter
import com.example.photogallery.utils.Strings
import com.example.photogallery.features.galleryScreen.components.ConfirmDialog
import com.example.photogallery.features.galleryScreen.utils.toColorFilterOrNull

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
            Text(Strings.NO_IMAGES)
        }
        return
    }

    Scaffold(
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
                    TextButton(onClick = { selectedFilter = PhotoFilter.None }) { Text(Strings.CANCEL) }
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
                    contentScale = ContentScale.Inside,
                    colorFilter = selectedFilter.toColorFilterOrNull()
                )
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
}
