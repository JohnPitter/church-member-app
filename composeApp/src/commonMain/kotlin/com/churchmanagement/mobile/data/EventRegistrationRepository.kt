package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.firestore.toInstant
import com.churchmanagement.mobile.data.model.CreateEventConfirmationDto
import com.churchmanagement.mobile.data.model.EventConfirmationDto
import com.churchmanagement.mobile.domain.AppUser
import com.churchmanagement.mobile.domain.EventConfirmation
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Inscrição (confirmação de presença) em eventos. Grava na coleção `eventConfirmations`,
 * compartilhada com o web (mesmo schema). Inscrever = doc com status "confirmed"; cancelar = remover.
 */
class EventRegistrationRepository(private val firestore: FirebaseFirestore) {

    /** Confirmações de um evento em tempo real (StateFlow quente). `null` = carregando. */
    fun observeConfirmations(eventId: String): StateFlow<List<EventConfirmation>?> =
        firestore.collection(Collections.EVENT_CONFIRMATIONS).snapshots.map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                runCatching {
                    val dto = doc.data(EventConfirmationDto.serializer())
                    if (dto.eventId == eventId) dto.toDomain(doc.id) else null
                }.getOrNull()
            }
        }.sharedState("eventConfirmations:$eventId")

    /**
     * Confirma a presença do usuário. Reusa a confirmação existente ([existingId]), se houver,
     * para não duplicar documentos (mesmo comportamento do web).
     */
    suspend fun confirm(eventId: String, user: AppUser, existingId: String?) {
        val collection = firestore.collection(Collections.EVENT_CONFIRMATIONS)
        val dto = CreateEventConfirmationDto(
            eventId = eventId,
            userId = user.uid,
            userName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "Membro",
            confirmedAt = Timestamp.now(),
        )
        if (existingId != null) {
            collection.document(existingId).set(dto)
        } else {
            collection.add(dto)
        }
        println("[EventReg] presença confirmada: event=$eventId user=${user.uid}")
    }

    /** Cancela a inscrição (remove o documento). */
    suspend fun cancel(confirmationId: String) {
        firestore.collection(Collections.EVENT_CONFIRMATIONS).document(confirmationId).delete()
        println("[EventReg] inscrição cancelada: confirmation=$confirmationId")
    }
}

private fun EventConfirmationDto.toDomain(id: String) = EventConfirmation(
    id = id,
    eventId = eventId,
    userId = userId,
    userName = userName,
    status = status,
    confirmedAt = confirmedAt?.toInstant(),
)
