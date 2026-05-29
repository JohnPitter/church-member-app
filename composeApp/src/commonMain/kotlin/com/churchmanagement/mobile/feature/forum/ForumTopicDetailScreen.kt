package com.churchmanagement.mobile.feature.forum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.data.AuthRepository
import com.churchmanagement.mobile.data.ForumRepository
import com.churchmanagement.mobile.domain.ForumReply
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.LoadingBox
import com.churchmanagement.mobile.util.stripHtml
import com.churchmanagement.mobile.util.toShortDate
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ForumTopicDetailScreen(topicId: String, modifier: Modifier = Modifier) {
    val repo: ForumRepository = koinInject()
    val auth: AuthRepository = koinInject()
    val scope = rememberCoroutineScope()

    val topicsFlow = remember { repo.observeTopics() }
    val repliesFlow = remember(topicId) { repo.observeReplies(topicId) }
    val topics by topicsFlow.collectAsState(initial = null)
    val replies by repliesFlow.collectAsState(initial = emptyList())

    when (val list = topics) {
        null -> LoadingBox(modifier)
        else -> {
            val topic = list.firstOrNull { it.id == topicId }
            if (topic == null) {
                EmptyState(title = "Tópico não encontrado", modifier = modifier)
                return
            }
            Column(modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(topic.title, style = MaterialTheme.typography.headlineSmall)
                        if (topic.authorName.isNotBlank()) {
                            Text(
                                text = "por ${topic.authorName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        val body = topic.content.stripHtml()
                        if (body.isNotBlank()) {
                            Text(
                                text = body,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
                        Text(
                            text = if (replies.isEmpty()) "Respostas" else "Respostas (${replies.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }

                    if (replies.isEmpty()) {
                        item {
                            Text(
                                text = "Seja o primeiro a responder.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(replies, key = { it.id }) { reply ->
                            ReplyCard(reply)
                        }
                    }
                }

                ReplyComposer(
                    onSend = { text, done ->
                        val user = auth.currentUser
                        if (user == null) {
                            done(false)
                        } else {
                            scope.launch {
                                val ok = runCatching { repo.addReply(topicId, text, user) }.isSuccess
                                done(ok)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ReplyComposer(onSend: (String, (Boolean) -> Unit) -> Unit) {
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Escreva uma resposta...") },
                enabled = !sending,
                modifier = Modifier.weight(1f),
                maxLines = 4,
            )
            IconButton(
                onClick = {
                    if (text.isNotBlank() && !sending) {
                        sending = true
                        onSend(text) { ok ->
                            sending = false
                            if (ok) text = ""
                        }
                    }
                },
                enabled = text.isNotBlank() && !sending,
            ) {
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplyCard(reply: ForumReply) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = buildString {
                    if (reply.authorName.isNotBlank()) append(reply.authorName)
                    reply.createdAt?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it.toShortDate())
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            val body = reply.content.stripHtml()
            if (body.isNotBlank()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
