package com.hurtado.miya.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hurtado.miya.features.home.MediaKind

/**
 * Image tile with an SF-Symbol-equivalent Material icon fallback, mirrors `CoverTile.swift`'s
 * `AsyncImage` + fallback `Image(systemName:)`. `systemImage` strings from the fixture/GraphQL
 * data ("music.note", "photo", "square.stack") have no Android equivalent, so [kind] is used to
 * pick the nearest Material icon instead of trying to map SF Symbol names 1:1.
 */
@Composable
fun CoverTile(
    imageUrl: String?,
    kind: MediaKind,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = kind.fallbackIcon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun MediaKind.fallbackIcon(): ImageVector = when (this) {
    MediaKind.SONG -> Icons.Filled.MusicNote
    MediaKind.PHOTO -> Icons.Filled.Photo
    MediaKind.ALBUM -> Icons.Filled.Album
}
