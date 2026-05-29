package com.churchmanagement.mobile.feature.lives

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.churchmanagement.mobile.data.LiveStreamRepository
import com.churchmanagement.mobile.domain.LiveStream
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.ListSkeleton
import com.churchmanagement.mobile.util.toLongDate
import org.koin.compose.koinInject

@Composable
fun LivesScreen(modifier: Modifier = Modifier) {
    val repo: LiveStreamRepository = koinInject()
    val flow = remember { repo.observeStreams() }
    val streams by flow.collectAsState(initial = null)
    val uriHandler = LocalUriHandler.current

    when (val list = streams) {
        null -> ListSkeleton(modifier)
        else -> if (list.isEmpty()) {
            EmptyState(
                title = "Nenhuma transmissão",
                subtitle = "As lives e transmissões aparecerão aqui.",
                modifier = modifier,
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { stream ->
                    StreamCard(
                        stream = stream,
                        onClick = { if (stream.streamUrl.isNotBlank()) uriHandler.openUri(stream.streamUrl) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamCard(stream: LiveStream, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            if (!stream.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = stream.thumbnailUrl,
                    contentDescription = stream.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            }
            Column(Modifier.padding(16.dp)) {
                if (stream.isLive) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDC2626), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "● AO VIVO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = stream.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = if (stream.isLive) 8.dp else 0.dp),
                )
                stream.scheduledDate?.let {
                    Text(
                        text = it.toLongDate(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = "Toque para assistir",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
