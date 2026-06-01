package com.churchmanagement.mobile.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Diversity3
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.data.DevotionalRepository
import com.churchmanagement.mobile.data.EventRepository
import com.churchmanagement.mobile.domain.AppUser
import com.churchmanagement.mobile.domain.Devotional
import com.churchmanagement.mobile.feature.events.EventCard
import com.churchmanagement.mobile.ui.CardSkeleton
import com.churchmanagement.mobile.ui.SkeletonBox
import com.churchmanagement.mobile.ui.shimmerBrush
import com.churchmanagement.mobile.util.firstName
import com.churchmanagement.mobile.util.toLongDate
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
    user: AppUser,
    onOpenEvent: (String) -> Unit,
    onOpenDevotional: (String) -> Unit,
    onSeeAllEvents: () -> Unit,
    onOpenBlog: () -> Unit,
    onOpenForum: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenLives: () -> Unit,
    onOpenLeadership: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenBirthdays: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val eventRepo: EventRepository = koinInject()
    val devotionalRepo: DevotionalRepository = koinInject()

    val eventsFlow = remember { eventRepo.observeUpcoming(limit = 3) }
    val devotionalsFlow = remember { devotionalRepo.observePublished(limit = 1) }

    val events by eventsFlow.collectAsState(initial = null)
    val devotionals by devotionalsFlow.collectAsState(initial = null)
    val brush = shimmerBrush()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Olá, ${user.firstName()} 👋",
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        when (val devs = devotionals) {
            null -> item {
                SkeletonBox(
                    brush = brush,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = RoundedCornerShape(16.dp),
                )
            }
            else -> devs.firstOrNull()?.let { devotional ->
                item { DevotionalHighlight(devotional, onClick = { onOpenDevotional(devotional.id) }) }
            }
        }

        item { SectionHeader(title = "Próximos eventos", onSeeAll = onSeeAllEvents) }

        when (val evs = events) {
            null -> items(3) { CardSkeleton(brush) }
            else -> if (evs.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum evento agendado no momento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(evs, key = { it.id }) { event ->
                    EventCard(event = event, onClick = { onOpenEvent(event.id) })
                }
            }
        }

        item {
            Text(
                text = "Acesse também",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        val shortcuts = listOf(
            Shortcut("Blog", Icons.AutoMirrored.Outlined.Article, onOpenBlog),
            Shortcut("Fórum", Icons.Outlined.Forum, onOpenForum),
            Shortcut("Projetos", Icons.Outlined.Diversity3, onOpenProjects),
            Shortcut("Transmissões", Icons.Outlined.LiveTv, onOpenLives),
            Shortcut("Liderança", Icons.Outlined.Groups, onOpenLeadership),
            Shortcut("Oração", Icons.Outlined.VolunteerActivism, onOpenPrayer),
            Shortcut("Aniversários", Icons.Outlined.Cake, onOpenBirthdays),
        )
        items(shortcuts.chunked(2)) { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { shortcut ->
                    ShortcutCard(
                        label = shortcut.label,
                        icon = shortcut.icon,
                        onClick = shortcut.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class Shortcut(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun ShortcutCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun DevotionalHighlight(devotional: Devotional, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "DEVOCIONAL DO DIA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = devotional.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (devotional.bibleVerse.isNotBlank()) {
                Text(
                    text = "\"${devotional.bibleVerse}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            devotional.publishDate?.let {
                Text(
                    text = it.toLongDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onSeeAll) { Text("Ver todos") }
    }
}
