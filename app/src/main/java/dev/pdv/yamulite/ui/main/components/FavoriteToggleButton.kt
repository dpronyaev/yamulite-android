package dev.pdv.yamulite.ui.main.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FavoriteToggleButton(
    isLiked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
) {
    var showConfirm by remember { mutableStateOf(false) }

    IconButton(
        onClick = { if (isLiked) showConfirm = true else onToggle() },
        modifier = modifier,
    ) {
        if (isLiked) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Убрать из избранного",
                tint = Color(0xFFE53935),
                modifier = Modifier.size(iconSize),
            )
        } else {
            Icon(
                Icons.Outlined.FavoriteBorder,
                contentDescription = "В избранное",
                modifier = Modifier.size(iconSize),
            )
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Убрать из избранного?") },
            text = { Text("Трек будет удалён из избранного.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onToggle()
                }) { Text("Убрать") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Отмена") }
            },
        )
    }
}
