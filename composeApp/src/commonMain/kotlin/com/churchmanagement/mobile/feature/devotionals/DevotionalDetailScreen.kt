package com.churchmanagement.mobile.feature.devotionals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.data.DevotionalRepository
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.LoadingBox
import com.churchmanagement.mobile.ui.ShareButton
import com.churchmanagement.mobile.util.toLongDate
import org.koin.compose.koinInject

@Composable
fun DevotionalDetailScreen(devotionalId: String, modifier: Modifier = Modifier) {
    val repo: DevotionalRepository = koinInject()
    val flow = remember { repo.observePublished() }
    val devotionals by flow.collectAsState(initial = null)

    when (val list = devotionals) {
        null -> LoadingBox(modifier)
        else -> {
            val devotional = list.firstOrNull { it.id == devotionalId }
            if (devotional == null) {
                EmptyState(title = "Devocional não encontrado", modifier = modifier)
                return
            }
            Column(
                modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            ) {
                devotional.publishDate?.let {
                    Text(
                        text = it.toLongDate(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = devotional.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (devotional.author.isNotBlank()) {
                    Text(
                        text = "por ${devotional.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                if (devotional.bibleVerse.isNotBlank()) {
                    Card(
                        modifier = Modifier.padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "\"${devotional.bibleVerse}\"",
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = FontStyle.Italic,
                            )
                            if (devotional.bibleReference.isNotBlank()) {
                                Text(
                                    text = devotional.bibleReference,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }

                Section("Reflexão", devotional.reflection)
                Section("Conteúdo", devotional.content)
                Section("Oração", devotional.prayer)

                ShareButton(
                    text = buildString {
                        append(devotional.title)
                        if (devotional.bibleVerse.isNotBlank()) {
                            append("\n\n\"${devotional.bibleVerse}\"")
                            if (devotional.bibleReference.isNotBlank()) append(" — ${devotional.bibleReference}")
                        }
                        if (devotional.reflection.isNotBlank()) append("\n\n${devotional.reflection}")
                    },
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    if (body.isBlank()) return
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
    Text(text = body, style = MaterialTheme.typography.bodyLarge)
}
