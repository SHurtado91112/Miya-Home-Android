package com.hurtado.miya.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hurtado.miya.features.home.MediaKind

/**
 * Fanned 1–3 album covers, offset/scaled/darkened by depth, mirrors `StackedCoverCard.swift`.
 * [coverUrls] is the album's `items(first: 3)` cover list (front-to-back).
 */
@Composable
fun StackedCoverCard(
    coverUrls: List<String>,
    modifier: Modifier = Modifier,
) {
    val covers = coverUrls.take(3).ifEmpty { listOf(null) }
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        // Draw back-to-front so the first cover ends up on top.
        covers.reversed().forEachIndexed { reversedIndex, url ->
            val depth = covers.size - 1 - reversedIndex
            val offsetPx = with(density) { (depth * 4).dp.toPx() }
            val scale = 1f - depth * 0.06f
            val darken = 1f - depth * 0.12f
            CoverTile(
                imageUrl = url,
                kind = MediaKind.ALBUM,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = -offsetPx,
                        translationY = offsetPx,
                    )
                    .alpha(darken),
            )
        }
    }
}
