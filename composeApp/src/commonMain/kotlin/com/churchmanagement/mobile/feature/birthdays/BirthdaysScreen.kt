package com.churchmanagement.mobile.feature.birthdays

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.churchmanagement.mobile.data.MemberRepository
import com.churchmanagement.mobile.domain.Birthday
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.ListSkeleton
import com.churchmanagement.mobile.util.currentLocalDate
import com.churchmanagement.mobile.util.monthNamePt
import org.koin.compose.koinInject

@Composable
fun BirthdaysScreen(modifier: Modifier = Modifier) {
    val repo: MemberRepository = koinInject()
    val today = remember { currentLocalDate() }
    val flow = remember(today.monthNumber) { repo.observeBirthdays(today.monthNumber) }
    val birthdays by flow.collectAsState(initial = null)

    Column(modifier.fillMaxSize()) {
        Text(
            text = "Aniversariantes de ${monthNamePt(today.monthNumber)} 🎉",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )

        when (val list = birthdays) {
            null -> ListSkeleton()
            else -> if (list.isEmpty()) {
                EmptyState(
                    title = "Nenhum aniversariante este mês",
                    subtitle = "Os aniversariantes do mês aparecerão aqui.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(list, key = { it.id }) { birthday ->
                        BirthdayCard(birthday = birthday, isToday = birthday.day == today.dayOfMonth)
                    }
                }
            }
        }
    }
}

@Composable
private fun BirthdayCard(birthday: Birthday, isToday: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isToday) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(birthday)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = birthday.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (isToday) "Hoje! 🎂" else "Dia ${birthday.day}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DayBadge(birthday.day)
        }
    }
}

@Composable
private fun Avatar(birthday: Birthday) {
    if (!birthday.photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = birthday.photoUrl,
            contentDescription = birthday.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = birthday.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun DayBadge(day: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
