package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.firestore.toInstant
import com.churchmanagement.mobile.data.model.NotificationDto
import com.churchmanagement.mobile.domain.NotificationItem
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepository(private val firestore: FirebaseFirestore) {

    /**
     * Notificações destinadas ao usuário (ou broadcasts sem destinatário), mais recentes
     * primeiro. As regras do Firestore permitem que membros aprovados leiam a coleção;
     * a filtragem por destinatário é feita no cliente.
     */
    fun observeForUser(userId: String): Flow<List<NotificationItem>> =
        firestore.collection(Collections.NOTIFICATIONS).snapshots.map { snapshot ->
            snapshot.documents
                .mapNotNull { doc ->
                    runCatching {
                        val dto = doc.data(NotificationDto.serializer())
                        if (dto.userId == userId || dto.userId.isBlank()) dto.toDomain(doc.id) else null
                    }.getOrNull()
                }
                .sortedByDescending { it.createdAt }
        }

    fun observeUnreadCount(userId: String): Flow<Int> =
        observeForUser(userId).map { list -> list.count { it.isUnread } }
}

private fun NotificationDto.toDomain(id: String) = NotificationItem(
    id = id,
    title = title,
    message = message,
    type = type,
    isUnread = status == "unread",
    createdAt = createdAt?.toInstant(),
)
