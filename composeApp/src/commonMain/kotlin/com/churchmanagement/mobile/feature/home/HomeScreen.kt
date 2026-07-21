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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.data.DevotionalRepository
import com.churchmanagement.mobile.data.EventRepository
import com.churchmanagement.mobile.data.MemberRepository
import com.churchmanagement.mobile.domain.AppUser
import com.churchmanagement.mobile.domain.Birthday
import com.churchmanagement.mobile.domain.Devotional
import com.churchmanagement.mobile.feature.events.EventCard
import com.churchmanagement.mobile.ui.ListSkeleton
import com.churchmanagement.mobile.ui.MemberAvatar
import com.churchmanagement.mobile.ui.WhatsAppButton
import com.churchmanagement.mobile.util.currentLocalDate
import com.churchmanagement.mobile.util.firstName
import com.churchmanagement.mobile.util.monthNamePt
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
    onOpenDynamic: (id: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val eventRepo: EventRepository = koinInject()
    val devotionalRepo: DevotionalRepository = koinInject()
    val memberRepo: MemberRepository = koinInject()

    val today = remember { currentLocalDate() }
    val eventsFlow = remember { eventRepo.observeUpcoming(limit = 3) }
    val devotionalsFlow = remember { devotionalRepo.observePublished(limit = 1) }
    val birthdaysFlow = remember { memberRepo.observeBirthdays(today.monthNumber) }

    val events by eventsFlow.collectAsState()
    val devotionals by devotionalsFlow.collectAsState()
    val birthdaysState by birthdaysFlow.collectAsState()

    // Só renderiza quando o conteúdo principal carregou — sem seções aparecendo em momentos diferentes.
    // Aniversariantes é best-effort (seção opcional): não trava a tela se `members` falhar.
    val ev = events
    val dev = devotionals
    if (ev == null || dev == null) {
        ListSkeleton(modifier)
        return
    }
    val bd = birthdaysState.orEmpty()

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

        dev.firstOrNull()?.let { devotional ->
            item { DevotionalHighlight(devotional, onClick = { onOpenDevotional(devotional.id) }) }
        }

        if (bd.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Aniversariantes de ${monthNamePt(today.monthNumber)} 🎂",
                    onSeeAll = onOpenBirthdays,
                )
            }
            item { BirthdayCarousel(birthdays = bd, todayDay = today.dayOfMonth) }
        }

        item { SectionHeader(title = "Próximos eventos", onSeeAll = onSeeAllEvents) }

        if (ev.isEmpty()) {
            item {
                Text(
                    text = "Nenhum evento agendado no momento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(ev, key = { it.id }) { event ->
                EventCard(event = event, onClick = { onOpenEvent(event.id) })
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
            Shortcut("Pedidos de Oração", Icons.Outlined.VolunteerActivism, onOpenPrayer),
            Shortcut("Aniversários", Icons.Outlined.Cake, onOpenBirthdays),
            Shortcut("Novidades", Icons.Outlined.AutoAwesome) { onOpenDynamic("novidades", "Novidades") },
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
private fun BirthdayCarousel(birthdays: List<Birthday>, todayDay: Int) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(birthdays, key = { it.id }) { birthday ->
            BirthdayCarouselCard(birthday = birthday, isToday = birthday.day == todayDay)
        }
    }
}

@Composable
private fun BirthdayCarouselCard(birthday: Birthday, isToday: Boolean) {
    Card(
        modifier = Modifier.width(150.dp),
        colors = if (isToday) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MemberAvatar(name = birthday.name, photoUrl = birthday.photoUrl, size = 56.dp)
            Text(
                text = birthday.name,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp),
            )
            Text(
                text = if (isToday) "Hoje! 🎂" else "Dia ${birthday.day}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            WhatsAppButton(phone = birthday.phone, modifier = Modifier.fillMaxWidth())
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
