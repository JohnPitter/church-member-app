package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.firestore.toInstant
import com.churchmanagement.mobile.data.model.EventDto
import com.churchmanagement.mobile.domain.Event
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepository(private val firestore: FirebaseFirestore) {

    /**
     * Observa todos os eventos em tempo real. Filtragem/ordenação são feitas no cliente
     * (escala de membros é pequena) para evitar dependência da API de query do Firestore.
     */
    fun observeEvents(): Flow<List<Event>> =
        firestore.collection(Collections.EVENTS).snapshots.map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                runCatching { doc.data(EventDto.serializer()).toDomain(doc.id) }.getOrNull()
            }
        }

    /** Próximos eventos (não cancelados, com data futura ou de hoje), ordenados por data. */
    fun observeUpcoming(limit: Int? = null): Flow<List<Event>> =
        observeEvents().map { events ->
            val upcoming = events
                .filter { it.status != "cancelled" && it.date != null }
                .sortedBy { it.date }
            if (limit != null) upcoming.take(limit) else upcoming
        }
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
