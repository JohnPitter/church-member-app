package com.churchmanagement.mobile.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.data.NotificationRepository
import com.churchmanagement.mobile.domain.NotificationItem
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.LoadingGate
import com.churchmanagement.mobile.util.toShortDate
import org.koin.compose.koinInject

@Composable
fun NotificationsScreen(userId: String, modifier: Modifier = Modifier) {
    val repo: NotificationRepository = koinInject()
    val flow = remember(userId) { repo.observeForUser(userId) }
    val notifications by flow.collectAsState()

    LoadingGate(ready = notifications != null, modifier = modifier) {
        val list = notifications.orEmpty()
        if (list.isEmpty()) {
            EmptyState(
                title = "Sem avisos",
                subtitle = "Você está em dia! Novos avisos aparecerão aqui.",
                modifier = modifier,
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(list, key = { it.id }) { item ->
                    NotificationCard(item)
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(item: NotificationItem) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isUnread) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = if (item.isUnread) 8.dp else 0.dp),
                )
            }
            if (item.message.isNotBlank()) {
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            item.createdAt?.let {
                Text(
                    text = it.toShortDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
