package com.hurtado.miya.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hurtado.miya.features.home.Album
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.features.home.MediaKind

/**
 * Adaptive wrapping grid of up to 5 item cards + a trailing "see all" card, mirrors
 * `SectionCardGrid.swift`. Uses Compose's `FlowRow` (wraps by available width, not a fixed
 * column count) rather than `LazyVGrid`, matching the iOS gap-flexes-4-8pt behavior closely
 * enough with a fixed 8dp gap.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SectionCardGrid(
    items: List<HomeSectionItem>,
    onItemClick: (HomeSectionItem) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxVisible: Int = 5,
) {
    FlowRow(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.take(maxVisible).forEach { item ->
            if (item.kind == MediaKind.ALBUM && item.coverPreviewURLs.isNotEmpty()) {
                Column(modifier = Modifier.width(84.dp)) {
                    StackedCoverCard(coverUrls = item.coverPreviewURLs)
                    ReservedCaption(title = item.title, subtitle = item.subtitle)
                }
            } else {
                PreviewCard(item = item, onClick = { onItemClick(item) })
            }
        }
        if (items.size > maxVisible) {
            MoreCard(onClick = onSeeAllClick)
        }
    }
}

/** Album grid variant used by AlbumDetail/AuthorDetail shelves (Stage 3), kept here for reuse. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlbumCardGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        albums.forEach { album ->
            Column(
                modifier = Modifier
                    .width(84.dp)
                    .clickable { onAlbumClick(album) },
            ) {
                StackedCoverCard(coverUrls = album.items.take(3).mapNotNull { it.smallImageURL })
                ReservedCaption(title = album.title, subtitle = album.subtitle)
            }
        }
    }
}
