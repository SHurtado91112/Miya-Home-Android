package com.hurtado.miya.views

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A transparent top bar carrying only a back button — mirrors iOS's `serifBackButton()` +
 * `toolbarBackground(.hidden)` (a custom back control over an otherwise chrome-free nav bar).
 * Every detail screen (Section/Album/AuthorDetail) needs *some* visible way back: relying on the
 * system back gesture alone with no on-screen affordance is a real usability gap the iOS app
 * doesn't have (its `NavigationStack` always shows a back chevron).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}
