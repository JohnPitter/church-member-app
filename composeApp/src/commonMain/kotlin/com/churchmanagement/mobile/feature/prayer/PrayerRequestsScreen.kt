package com.churchmanagement.mobile.feature.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.churchmanagement.mobile.data.PrayerRepository
import com.churchmanagement.mobile.domain.AppUser
import com.churchmanagement.mobile.domain.PrayerRequestItem
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.ListSkeleton
import com.churchmanagement.mobile.util.toShortDate
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val DAYS_WINDOW = 7

@Composable
fun PrayerRequestsScreen(
    user: AppUser,
    onSubmitRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repo: PrayerRepository = koinInject()
    val flow = remember { repo.observeCommunity(DAYS_WINDOW) }
    val requests by flow.collectAsState()
    val scope = rememberCoroutineScope()
    var processingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val userKey = user.email?.takeIf { it.isNotBlank() } ?: user.uid

    Box(modifier.fillMaxSize()) {
        when (val list = requests) {
            null -> ListSkeleton()
            else -> if (list.isEmpty()) {
                EmptyState(
                    title = "Nenhum pedido recente",
                    subtitle = "Ainda não há pedidos de oração nos últimos $DAYS_WINDOW dias.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = "Pedidos dos últimos $DAYS_WINDOW dias. Toque em Orar para interceder.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (error != null) {
                        item {
                            Text(
                                text = error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    items(list, key = { it.id }) { item ->
                        PrayerRequestCard(
                            item = item,
                            userKey = userKey,
                            processing = processingId == item.id,
                            onTogglePray = {
                                if (processingId != null) return@PrayerRequestCard
                                val already = item.prayedBy.contains(userKey)
                                processingId = item.id
                                error = null
                                scope.launch {
                                    try {
                                        repo.togglePrayedBy(item.id, userKey, currentlyPrayed = already)
                                    } catch (_: Throwable) {
                                        error = "Não foi possível registrar a oração. Tente de novo."
                                    } finally {
                                        processingId = null
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onSubmitRequest,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Enviar pedido")
        }
    }
}

@Composable
private fun PrayerRequestCard(
    item: PrayerRequestItem,
    userKey: String,
    processing: Boolean,
    onTogglePray: () -> Unit,
) {
    val iPrayed = item.prayedBy.contains(userKey)
    val count = item.prayedBy.size
    val displayName = when {
        item.isAnonymous -> "Anônimo"
        item.name.isNotBlank() -> item.name
        else -> "Irmão(ã)"
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (item.isUrgent) {
                    Text(
                        text = "Urgente",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            item.createdAt?.let {
                Text(
                    text = it.toShortDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = item.request,
                style = MaterialTheme.typography.bodyLarge,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (count) {
                        0 -> "Ninguém orou ainda"
                        1 -> "1 pessoa está orando"
                        else -> "$count pessoas estão orando"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )

                if (iPrayed) {
                    Button(
                        onClick = onTogglePray,
                        enabled = !processing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(if (processing) "..." else "🙏 Orei ($count)")
                    }
                } else {
                    OutlinedButton(onClick = onTogglePray, enabled = !processing) {
                        Text(if (processing) "..." else "🙏 Orar${if (count > 0) " ($count)" else ""}")
                    }
                }
            }
        }
    }
}
