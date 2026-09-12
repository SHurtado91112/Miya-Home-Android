package com.hurtado.miya.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.features.home.MediaKind

/**
 * Unpaginated-looking item grid used by the detail screens (Section/Album/AuthorDetail), mirrors
 * `SectionCardGrid(naturalCardSize:)` on iOS. `FlowRow` isn't lazy/virtualized the way iOS's
 * `LazyVGrid` is, so unlike the per-card `onAppear` proximity trigger there, pagination here is
 * driven by the trailing loading indicator entering composition — effectively auto-paginating
 * through everything once the screen is visible rather than gating strictly on scroll position.
 * Acceptable for this app's page sizes (~20/page); revisit if a screen ever needs true
 * virtualization (`LazyVerticalStaggeredGrid` + manual "near end" index checks).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailItemGrid(
    items: List<HomeSectionItem>,
    hasMore: Boolean,
    onItemClick: (HomeSectionItem) -> Unit,
    onReachedEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            if (item.kind == MediaKind.ALBUM && item.coverPreviewURLs.isNotEmpty()) {
                Column(modifier = Modifier.width(84.dp)) {
                    StackedCoverCard(coverUrls = item.coverPreviewURLs)
                    ReservedCaption(title = item.title, subtitle = item.subtitle)
                }
            } else {
                PreviewCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }

    if (hasMore) {
        LaunchedEffect(items.size) { onReachedEnd() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}
