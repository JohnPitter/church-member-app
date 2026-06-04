package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.firestore.toInstant
import com.churchmanagement.mobile.data.model.EventDto
import com.churchmanagement.mobile.domain.Event
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class EventRepository(private val firestore: FirebaseFirestore) {

    /**
     * Observa todos os eventos em tempo real (StateFlow quente compartilhado). Filtragem/ordenação
     * são feitas no cliente (escala de membros é pequena). `null` = ainda carregando.
     */
    fun observeEvents(): StateFlow<List<Event>?> =
        firestore.collection(Collections.EVENTS).snapshots.map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                runCatching { doc.data(EventDto.serializer()).toDomain(doc.id) }.getOrNull()
            }
        }.sharedState("events")

    /** Próximos eventos (não cancelados, com data futura ou de hoje), ordenados por data. */
    fun observeUpcoming(limit: Int? = null): StateFlow<List<Event>?> =
        observeEvents().map { events ->
            events?.let { list ->
                val upcoming = list
                    .filter { it.status != "cancelled" && it.date != null }
                    .sortedBy { it.date }
                if (limit != null) upcoming.take(limit) else upcoming
            }
        }.sharedState("events:upcoming:$limit")
}

private fun EventDto.toDomain(id: String) = Event(
    id = id,
    title = title,
    description = description,
    date = date?.toInstant(),
    time = time,
    location = location,
    categoryName = category?.name ?: "",
    categoryColor = category?.color ?: "",
    imageUrl = imageURL,
    streamingUrl = streamingURL,
    responsible = responsible,
    status = status,
)
