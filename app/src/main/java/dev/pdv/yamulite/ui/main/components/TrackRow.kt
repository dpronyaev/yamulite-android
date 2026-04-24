package dev.pdv.yamulite.ui.main.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.pdv.yamulite.data.music.dto.TrackDto

fun TrackDto.displayLine(): String {
    val artist = artists.joinToString(", ") { it.name }
    val t = title.orEmpty()
    return when {
        artist.isBlank() && t.isBlank() -> "(без названия)"
        artist.isBlank() -> t
        t.isBlank() -> artist
        else -> "$artist — $t"
    }
}

@Composable
fun TrackRow(
    track: TrackDto,
    isLiked: Boolean,
    onClick: () -> Unit,
    onLikeToggle: () -> Unit,
) {
    val cover = track.coverUri ?: track.albums.firstOrNull()?.coverUri
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        CoverImage(coverUri = cover)
        Text(
            text = track.displayLine(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onLikeToggle, modifier = Modifier.size(40.dp)) {
            if (isLiked) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Убрать из избранного",
                    tint = Color(0xFFE53935),
                )
            } else {
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = "В избранное",
                )
            }
        }
    }
}
