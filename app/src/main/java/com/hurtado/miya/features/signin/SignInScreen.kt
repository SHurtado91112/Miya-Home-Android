package com.hurtado.miya.features.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hurtado.miya.services.auth.UserProfile

/** The sign-in wall, port of `SignInView.swift`. */
@Composable
fun SignInScreen(
    onSignedIn: (UserProfile) -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.store.state.collectAsState()
    val store = viewModel.store

    LaunchedEffect(state.signedInProfile) {
        state.signedInProfile?.let(onSignedIn)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Miya", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your images and audio, everywhere.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = {
                store.send(SignInAction.View.GoogleTapped)
            },
            enabled = !state.isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
            ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isAuthenticating) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.background,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(imageVector = Icons.Filled.Public, contentDescription = null)
                }
                Text(text = "Continue with Google", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }

    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { store.send(SignInAction.View.ErrorDismissed) },
            title = { Text("Couldn't sign in") },
            text = { Text(state.errorMessage.orEmpty()) },
            confirmButton = {
                Button(onClick = { store.send(SignInAction.View.ErrorDismissed) }) { Text("OK") }
            },
        )
    }
}
