package com.churchmanagement.mobile.feature.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.churchmanagement.mobile.data.EventRepository
import com.churchmanagement.mobile.domain.Event
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.ListSkeleton
import com.churchmanagement.mobile.util.toLongDate
import org.koin.compose.koinInject

@Composable
fun EventsScreen(
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val repo: EventRepository = koinInject()
    val flow = remember { repo.observeUpcoming() }
    val events by flow.collectAsState(initial = null)

    when (val list = events) {
        null -> ListSkeleton(modifier)
        else -> if (list.isEmpty()) {
            EmptyState(
                title = "Nenhum evento agendado",
                subtitle = "Novos eventos aparecerão aqui assim que forem publicados.",
                modifier = modifier,
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { event ->
                    EventCard(event = event, onClick = { onOpenEvent(event.id) })
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            if (!event.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                )
            }
            Column(Modifier.padding(16.dp)) {
                if (event.categoryName.isNotBlank()) {
                    Text(
                        text = event.categoryName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
                event.date?.let { date ->
                    InfoRow(Icons.Outlined.Schedule, buildString {
                        append(date.toLongDate())
                        if (event.time.isNotBlank()) append(" · ${event.time}")
                    })
                }
                if (event.location.isNotBlank()) {
                    InfoRow(Icons.Outlined.LocationOn, event.location)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.height(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
