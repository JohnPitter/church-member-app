package com.churchmanagement.mobile.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.util.whatsappUrl

private val WhatsAppGreen = Color(0xFF25D366)

/** Botão "WhatsApp" que abre a conversa com [phone]. Não renderiza nada se o número for inválido. */
@Composable
fun WhatsAppButton(phone: String?, modifier: Modifier = Modifier) {
    val url = whatsappUrl(phone) ?: return
    val uriHandler = LocalUriHandler.current
    FilledTonalButton(
        onClick = { uriHandler.openUri(url) },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = WhatsAppGreen,
            contentColor = Color.White,
        ),
    ) {
        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("WhatsApp", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
    }
}
