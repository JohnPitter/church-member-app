package com.churchmanagement.mobile.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.platform.Sharer
import org.koin.compose.koinInject

/** Botão "Compartilhar" que abre a folha de compartilhamento nativa com [text]. */
@Composable
fun ShareButton(text: String, modifier: Modifier = Modifier) {
    val sharer: Sharer = koinInject()
    OutlinedButton(onClick = { sharer.share(text) }, modifier = modifier) {
        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Compartilhar")
    }
}
