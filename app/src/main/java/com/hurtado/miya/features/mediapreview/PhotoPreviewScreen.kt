package com.hurtado.miya.features.mediapreview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hurtado.miya.features.home.AuthorRef
import com.hurtado.miya.views.CoverTile
import kotlin.math.max
import kotlin.math.min

private const val MAX_SCALE = 4f
private const val DOUBLE_TAP_SCALE = 2.5f

/** Port of `PhotoPreviewView`'s full-screen presentation. */
@Composable
fun PhotoPreviewScreen(
    state: PhotoPreviewState,
    onAction: (PhotoPreviewAction) -> Unit,
) {
    var scale by remember(state.item.id) { mutableFloatStateOf(1f) }
    var offset by remember(state.item.id) { mutableStateOf(Offset.Zero) }

    BackHandler { onAction(PhotoPreviewAction.View.MinimizeTapped) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.item.id) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val next = (scale * zoom).coerceIn(1f, MAX_SCALE)
                        scale = next
                        offset = if (next <= 1f) Offset.Zero else offset + pan
                    }
                }
                .pointerInput(state.item.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = DOUBLE_TAP_SCALE
                            }
                        },
                    )
                },
        ) {
            if (state.item.imageURL != null) {
                AsyncImage(
                    model = state.item.imageURL,
                    contentDescription = state.item.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        ),
                )
            }
        }

        // Chrome: close (top-left), metadata toggle (top-right). windowInsetsPadding keeps
        // these below the status bar — iOS hides the status bar entirely for this screen
        // (`.statusBarHidden()`); insetting the buttons is the simpler Android equivalent and
        // avoids the status bar's touch-intercept zone silently swallowing taps.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ChromeIconButton(Icons.Filled.Close, "Close") { onAction(PhotoPreviewAction.View.CloseTapped) }
            ChromeIconButton(Icons.Filled.Info, "Info") { onAction(PhotoPreviewAction.View.ToggleMetadataTapped) }
        }

        if (state.showsMetadata) {
            PhotoMetadataPanel(
                state = state,
                onAuthorTapped = { onAction(PhotoPreviewAction.View.AuthorTapped(it)) },
                onViewAlbumTapped = { onAction(PhotoPreviewAction.View.ViewAlbumTapped) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ChromeIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(Color.Black.copy(alpha = 0.35f), CircleShape),
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = Color.White)
    }
}

@Composable
private fun PhotoMetadataPanel(
    state: PhotoPreviewState,
    onAuthorTapped: (AuthorRef) -> Unit,
    onViewAlbumTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xCC1C1B1F),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = state.item.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(
                text = state.item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = state.item.detail,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp),
            )
            val author = state.item.author
            if (author != null) {
                Text(
                    text = "By ${author.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { onAuthorTapped(author) },
                )
            }
            if (state.item.albumID != null) {
                Text(
                    text = "View Album",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { onViewAlbumTapped() },
                )
            }
        }
    }
}

/**
 * The collapsed photo bar — thumbnail, title/subtitle, close. Mirrors `PhotoMiniBar`. Docked at
 * the bottom by [com.hurtado.miya.features.mediapreview.MediaPreviewHost]; currently unreachable
 * on its own (nothing lowers a solo photo's detent to MINI yet — see [MediaPreviewState] doc),
 * but exercised once Stage 5 adds the song slot and the cross-detent rule.
 */
@Composable
fun PhotoMiniBar(
    state: PhotoPreviewState,
    onAction: (PhotoPreviewAction) -> Unit,
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
                    detectTapGestures(onTap = { onAction(PhotoPreviewAction.View.ExpandTapped) })
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverTile(
                imageUrl = state.item.smallImageURL,
                kind = state.item.kind,
                modifier = Modifier.size(44.dp),
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .fillMaxWidth(0.7f),
            ) {
                Text(text = state.item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = state.item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onAction(PhotoPreviewAction.View.CloseTapped) }) {
                Icon(Icons.Filled.Close, contentDescription = "Close photo preview")
            }
        }
    }
}
