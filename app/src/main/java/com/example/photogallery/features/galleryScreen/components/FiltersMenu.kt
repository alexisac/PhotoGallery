package com.example.photogallery.features.galleryScreen.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.photogallery.model.PhotoFilter
import com.example.photogallery.utils.Strings

@Composable
fun FiltersMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (PhotoFilter) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(Strings.NONE) },
            onClick = {
                onSelect(PhotoFilter.None);
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text(Strings.SEPIA) },
            onClick = {
                onSelect(PhotoFilter.Sepia);
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text(Strings.BLACK_WHITE) },
            onClick = {
                onSelect(PhotoFilter.GrayScale);
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text(Strings.INVERT_COLORS) },
            onClick = {
                onSelect(PhotoFilter.Invert);
                onDismiss()
            }
        )
    }
}