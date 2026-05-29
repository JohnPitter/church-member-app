package com.churchmanagement.mobile.feature.leadership

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.churchmanagement.mobile.data.LeaderRepository
import com.churchmanagement.mobile.domain.Leader
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.ListSkeleton
import org.koin.compose.koinInject

@Composable
fun LeadershipScreen(modifier: Modifier = Modifier) {
    val repo: LeaderRepository = koinInject()
    val flow = remember { repo.observeLeaders() }
    val leaders by flow.collectAsState(initial = null)

    when (val list = leaders) {
        null -> ListSkeleton(modifier)
        else -> if (list.isEmpty()) {
            EmptyState(
                title = "Liderança não cadastrada",
                subtitle = "A equipe de liderança aparecerá aqui.",
                modifier = modifier,
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { leader ->
                    LeaderCard(leader)
                }
            }
        }
    }
}

@Composable
private fun LeaderCard(leader: Leader) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LeaderAvatar(leader)
            Column(Modifier.padding(start = 16.dp)) {
                Text(text = leader.name, style = MaterialTheme.typography.titleMedium)
                if (leader.role.isNotBlank()) {
                    Text(
                        text = leader.role,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (!leader.ministry.isNullOrBlank()) {
                    Text(
                        text = leader.ministry,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!leader.bio.isNullOrBlank()) {
                    Text(
                        text = leader.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderAvatar(leader: Leader) {
    if (!leader.photo.isNullOrBlank()) {
        AsyncImage(
            model = leader.photo,
            contentDescription = leader.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp).clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = leader.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
