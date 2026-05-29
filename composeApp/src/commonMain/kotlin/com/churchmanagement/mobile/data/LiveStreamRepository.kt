package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.firestore.toInstant
import com.churchmanagement.mobile.data.model.LiveStreamDto
import com.churchmanagement.mobile.domain.LiveStream
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LiveStreamRepository(private val firestore: FirebaseFirestore) {

    /** Transmissões não canceladas: ao vivo primeiro, depois agendadas mais recentes. */
    fun observeStreams(): Flow<List<LiveStream>> =
        firestore.collection(Collections.LIVE_STREAMS).snapshots.map { snapshot ->
            snapshot.documents
                .mapNotNull { doc ->
                    runCatching {
                        val dto = doc.data(LiveStreamDto.serializer())
                        if (dto.status != "cancelled") dto.toDomain(doc.id) else null
                    }.getOrNull()
                }
                .sortedWith(
                    compareByDescending<LiveStream> { it.isLive }
                        .thenByDescending { it.scheduledDate }
                )
        }
}

private fun LiveStreamDto.toDomain(id: String) = LiveStream(
    id = id,
    title = title,
    description = description,
    streamUrl = streamUrl,
    thumbnailUrl = thumbnailUrl,
    isLive = isLive,
    scheduledDate = scheduledDate?.toInstant(),
)
