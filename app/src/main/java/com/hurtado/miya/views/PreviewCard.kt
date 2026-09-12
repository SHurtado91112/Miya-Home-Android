package com.hurtado.miya.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hurtado.miya.features.home.HomeSectionItem

/** ~84dp square tile for one item, mirrors `PreviewCard.swift`. */
@Composable
fun PreviewCard(
    item: HomeSectionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(84.dp)
            .clickable(onClick = onClick),
    ) {
        CoverTile(
            imageUrl = item.smallImageURL,
            kind = item.kind,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        ReservedCaption(title = item.title, subtitle = item.subtitle)
    }
}

/** "See all" dashed tile, mirrors `MoreCard` in `PreviewCard.swift`. */
@Composable
fun MoreCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(84.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.MoreHoriz, contentDescription = "See all")
        }
        ReservedCaption(title = "See All", subtitle = "")
    }
}

/**
 * Two-line-height caption so grid rows align regardless of whether the subtitle is present,
 * mirrors `ReservedCaption` in `CoverTile.swift`.
 */
@Composable
fun ReservedCaption(title: String, subtitle: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 4.dp),
    )
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
