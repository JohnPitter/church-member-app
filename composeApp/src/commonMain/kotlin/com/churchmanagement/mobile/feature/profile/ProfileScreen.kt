package com.churchmanagement.mobile.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.churchmanagement.mobile.data.AuthRepository
import com.churchmanagement.mobile.domain.AppUser
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(user: AppUser, modifier: Modifier = Modifier) {
    val auth: AuthRepository = koinInject()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (!user.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(96.dp).background(
                    MaterialTheme.colorScheme.surfaceVariant, CircleShape
                ),
            )
        } else {
            Box(
                modifier = Modifier.size(96.dp).background(
                    MaterialTheme.colorScheme.primary, CircleShape
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (user.displayName ?: user.email ?: "?").take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Text(
            text = user.displayName ?: "Membro",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
            textAlign = TextAlign.Center,
        )
        if (!user.email.isNullOrBlank()) {
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        OutlinedButton(
            onClick = { scope.launch { auth.signOut() } },
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        ) {
            Text("Sair")
        }
    }
}
