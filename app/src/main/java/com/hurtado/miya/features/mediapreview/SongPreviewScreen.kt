package com.hurtado.miya.features.mediapreview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hurtado.miya.views.CoverTile

/** Port of `SongPreviewView`'s full-screen presentation. */
@Composable
fun SongPreviewScreen(
    state: SongPreviewState,
    onAction: (SongPreviewAction) -> Unit,
) {
    BackHandler { onAction(SongPreviewAction.View.MinimizeTapped) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Opaque background — without it this screen renders transparently over whatever
            // was behind it (a real bug caught on-device: Home's content bled straight through).
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier
                .size(320.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            if (state.item.imageURL != null) {
                AsyncImage(
                    model = state.item.imageURL,
                    contentDescription = state.item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(top = 32.dp, start = 32.dp, end = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.item.title,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
            )
            val author = state.item.author
            if (author != null) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { onAction(SongPreviewAction.View.AuthorTapped(author)) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = author.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = state.item.subtitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (state.item.albumID != null) {
                Text(
                    text = "View Album",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { onAction(SongPreviewAction.View.ViewAlbumTapped) },
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(top = 28.dp, start = 32.dp, end = 32.dp)
                .fillMaxWidth(),
        ) {
            Slider(
                value = state.currentTime.toFloat(),
                valueRange = 0f..(state.duration?.toFloat() ?: 1f),
                enabled = state.duration != null,
                onValueChange = { onAction(SongPreviewAction.View.ScrubChanged(it.toDouble())) },
                onValueChangeFinished = { onAction(SongPreviewAction.View.ScrubEnded) },
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = formatPlaybackTime(state.currentTime), style = MaterialTheme.typography.bodySmall)
                Text(
                    text = formatRemainingTime(state.currentTime, state.duration),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onAction(SongPreviewAction.View.PreviousTapped) },
                enabled = state.canGoBack,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous track")
            }
            IconButton(
                onClick = { onAction(SongPreviewAction.View.PlayPauseTapped) },
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(44.dp),
                )
            }
            IconButton(
                onClick = { onAction(SongPreviewAction.View.NextTapped) },
                enabled = state.nextItem != null,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next track")
            }
        }
    }
}

/** The collapsed song bar — artwork, title, play/pause, close. Mirrors `SongMiniBar`. */
@Composable
fun SongMiniBar(
    state: SongPreviewState,
    onAction: (SongPreviewAction) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .pointerInput(state.item.id) {
                    detectTapGestures(onTap = { onAction(SongPreviewAction.View.ExpandTapped) })
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverTile(
                imageUrl = state.item.smallImageURL,
                kind = state.item.kind,
                modifier = Modifier.size(44.dp),
            )
            Text(
                text = state.item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            )
            IconButton(onClick = { onAction(SongPreviewAction.View.PlayPauseTapped) }) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                )
            }
            IconButton(onClick = { onAction(SongPreviewAction.View.CloseTapped) }) {
                Icon(Icons.Filled.Close, contentDescription = "Close song preview")
            }
        }
    }
}
