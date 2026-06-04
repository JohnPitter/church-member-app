package com.churchmanagement.mobile.feature.events

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.churchmanagement.mobile.data.EventRepository
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.LoadingBox
import com.churchmanagement.mobile.ui.ShareButton
import com.churchmanagement.mobile.util.toLongDate
import org.koin.compose.koinInject

@Composable
fun EventDetailScreen(eventId: String, modifier: Modifier = Modifier) {
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
