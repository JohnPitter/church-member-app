package com.churchmanagement.mobile.feature.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.data.AuthRepository
import com.churchmanagement.mobile.data.PrayerRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun PrayerScreen(modifier: Modifier = Modifier) {
    val repo: PrayerRepository = koinInject()
    val auth: AuthRepository = koinInject()
    val scope = rememberCoroutineScope()

    var request by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }
    var isAnonymous by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (sent) {
        PrayerSentState(onNewRequest = {
            request = ""
            isUrgent = false
            isAnonymous = false
            sent = false
        })
        return
    }

    fun submit() {
        if (request.isBlank()) {
            error = "Escreva seu pedido."
            return
        }
        loading = true
        error = null
        scope.launch {
            try {
                val name = auth.currentUser?.let { it.displayName ?: it.email } ?: "Membro"
                repo.submit(name = name, request = request, isUrgent = isUrgent, isAnonymous = isAnonymous)
                sent = true
            } catch (e: Throwable) {
                error = "Não foi possível enviar. Tente novamente."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Text(
            text = "Compartilhe seu pedido de oração. Nossa equipe vai interceder por você. 🙏",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = request,
            onValueChange = { request = it },
            label = { Text("Seu pedido") },
            enabled = !loading,
            minLines = 4,
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp).padding(top = 16.dp),
        )

        ToggleRow(
            label = "Marcar como urgente",
            checked = isUrgent,
            enabled = !loading,
            onCheckedChange = { isUrgent = it },
        )
        ToggleRow(
            label = "Enviar como anônimo",
            checked = isAnonymous,
            enabled = !loading,
            onCheckedChange = { isAnonymous = it },
        )

        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Button(
            onClick = ::submit,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp).padding(end = 4.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(if (loading) "Enviando..." else "Enviar pedido")
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun PrayerSentState(onNewRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = "Recebemos seu pedido!",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Estaremos orando por você. 🙏",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        TextButton(onClick = onNewRequest, modifier = Modifier.padding(top = 16.dp)) {
            Text("Enviar outro pedido")
        }
    }
}
