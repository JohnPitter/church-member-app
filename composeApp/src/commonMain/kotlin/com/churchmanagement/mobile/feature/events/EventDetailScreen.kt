package com.churchmanagement.mobile.feature.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.churchmanagement.mobile.data.EventRegistrationRepository
import com.churchmanagement.mobile.data.EventRepository
import com.churchmanagement.mobile.domain.AppUser
import com.churchmanagement.mobile.domain.Event
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.LoadingBox
import com.churchmanagement.mobile.ui.ShareButton
import com.churchmanagement.mobile.util.toLongDate
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.compose.koinInject

@Composable
fun EventDetailScreen(eventId: String, user: AppUser, modifier: Modifier = Modifier) {
    val repo: EventRepository = koinInject()
    val flow = remember { repo.observeEvents() }
    val events by flow.collectAsState()

    when (val list = events) {
        null -> LoadingBox(modifier)
        else -> {
            val event = list.firstOrNull { it.id == eventId }
            if (event == null) {
                EmptyState(title = "Evento não encontrado", modifier = modifier)
                return
            }
            Column(
                modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            ) {
                if (!event.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = event.imageUrl,
                        contentDescription = event.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                    )
                }
                Column(Modifier.padding(20.dp)) {
                    Text(event.title, style = MaterialTheme.typography.headlineSmall)
                    event.date?.let {
                        Text(
                            text = buildString {
                                append(it.toLongDate())
                                if (event.time.isNotBlank()) append(" · ${event.time}")
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (event.location.isNotBlank()) {
                        DetailLabel("Local", event.location)
                    }
                    if (event.responsible.isNotBlank()) {
                        DetailLabel("Responsável", event.responsible)
                    }
                    if (event.description.isNotBlank()) {
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }

                    if (event.requiresConfirmation) {
                        EventRegistration(event = event, user = user)
                    }

                    ShareButton(
                        text = buildString {
                            append(event.title)
                            event.date?.let {
                                append("\n${it.toLongDate()}")
                                if (event.time.isNotBlank()) append(" · ${event.time}")
                            }
                            if (event.location.isNotBlank()) append("\n${event.location}")
                            if (event.description.isNotBlank()) append("\n\n${event.description}")
                        },
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

/** Seção de confirmação de presença (inscrição) — só aparece em eventos que exigem confirmação. */
@Composable
private fun EventRegistration(event: Event, user: AppUser) {
    val regRepo: EventRegistrationRepository = koinInject()
    val confirmations by remember(event.id) { regRepo.observeConfirmations(event.id) }.collectAsState()
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val list = confirmations
    val confirmed = list?.filter { it.status == "confirmed" }.orEmpty()
    val mine = confirmed.firstOrNull { it.userId == user.uid }
    val existingDoc = list?.firstOrNull { it.userId == user.uid }
    val count = confirmed.size
    val max = event.maxParticipants
    val full = max != null && count >= max && mine == null
    val open = event.status == "scheduled" && (event.date?.let { it > Clock.System.now() } ?: false)

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Presença", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (max != null) "$count de $max vagas" else {
                    "$count confirmado" + if (count == 1) "" else "s"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                list == null -> Text(
                    text = "Carregando…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                !open -> Text(
                    text = "Inscrições encerradas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                mine != null -> {
                    Text(
                        text = "✓ Presença confirmada",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedButton(
                        onClick = {
                            error = null
                            working = true
                            scope.launch {
                                runCatching { regRepo.cancel(mine.id) }
                                    .onFailure { error = "Não foi possível cancelar. Tente novamente." }
                                working = false
                            }
                        },
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Cancelar inscrição") }
                }
                else -> Button(
                    onClick = {
                        error = null
                        working = true
                        scope.launch {
                            runCatching { regRepo.confirm(event.id, user, existingDoc?.id) }
                                .onFailure { error = "Não foi possível confirmar. Tente novamente." }
                            working = false
                        }
                    },
                    enabled = !working && !full,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (full) "Vagas esgotadas" else "Confirmar presença") }
            }

            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DetailLabel(label: String, value: String) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
