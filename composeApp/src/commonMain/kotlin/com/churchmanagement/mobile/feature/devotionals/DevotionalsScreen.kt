package com.churchmanagement.mobile.feature.devotionals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import com.churchmanagement.mobile.domain.Devotional
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.ListSkeleton
import com.churchmanagement.mobile.util.toLongDate
import org.koin.compose.koinInject

@Composable
fun DevotionalsScreen(
    onOpenDevotional: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val repo: DevotionalRepository = koinInject()
    val flow = remember { repo.observePublished() }
    val devotionals by flow.collectAsState(initial = null)

    when (val list = devotionals) {
        null -> ListSkeleton(modifier)
        else -> if (list.isEmpty()) {
            EmptyState(
                title = "Nenhum devocional disponível",
                subtitle = "Os devocionais publicados aparecerão aqui.",
                modifier = modifier,
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { devotional ->
                    DevotionalCard(devotional = devotional, onClick = { onOpenDevotional(devotional.id) })
                }
            }
        }
    }
}

@Composable
private fun DevotionalCard(devotional: Devotional, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            devotional.publishDate?.let {
                Text(
                    text = it.toLongDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = devotional.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (devotional.bibleReference.isNotBlank()) {
                Text(
                    text = devotional.bibleReference,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
