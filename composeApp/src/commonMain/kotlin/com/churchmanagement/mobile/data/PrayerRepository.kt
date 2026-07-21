package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.firestore.toInstant
import com.churchmanagement.mobile.data.model.CreatePrayerRequestDto
import com.churchmanagement.mobile.data.model.PrayerRequestDto
import com.churchmanagement.mobile.domain.PrayerRequestItem
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

class PrayerRepository(private val firestore: FirebaseFirestore) {

    /**
     * Pedidos dos últimos [days] dias (exceto rejeitados), mais recentes primeiro.
     * Filtragem no cliente para não depender de índice composto.
     */
    fun observeCommunity(days: Int = 7): StateFlow<List<PrayerRequestItem>?> =
        firestore.collection(Collections.PRAYER_REQUESTS).snapshots.map { snapshot ->
            val since = Clock.System.now() - days.days
            snapshot.documents
                .mapNotNull { doc ->
                    runCatching {
                        val dto = doc.data(PrayerRequestDto.serializer())
                        if (dto.status.equals("rejected", ignoreCase = true)) return@runCatching null
                        val created = dto.createdAt?.toInstant()
                        if (created != null && created < since) return@runCatching null
                        dto.toDomain(doc.id)
                    }.getOrNull()
                }
                .sortedByDescending { it.createdAt }
        }.sharedState("prayerCommunity:$days")

    /** Envia um novo pedido de oração (status inicial: pendente de aprovação). */
    suspend fun submit(
        name: String,
        request: String,
        isUrgent: Boolean,
        isAnonymous: Boolean,
    ) {
        val now = Timestamp.now()
        val dto = CreatePrayerRequestDto(
            name = if (isAnonymous) "Anônimo" else name,
            request = request.trim(),
            isUrgent = isUrgent,
            isAnonymous = isAnonymous,
            createdAt = now,
            updatedAt = now,
        )
        firestore.collection(Collections.PRAYER_REQUESTS).add(dto)
    }

    /**
     * Alterna "orei" (curtida de oração) para o usuário identificado por [userKey] (e-mail ou uid).
     * @return true se passou a orar, false se removeu
     */
    suspend fun togglePrayedBy(requestId: String, userKey: String, currentlyPrayed: Boolean): Boolean {
        val key = userKey.trim()
        require(key.isNotEmpty()) { "Usuário não identificado" }

        val doc = firestore.collection(Collections.PRAYER_REQUESTS).document(requestId)
        if (currentlyPrayed) {
            doc.update(
                "prayedBy" to FieldValue.arrayRemove(key),
                "updatedAt" to Timestamp.now(),
            )
            return false
        }
        doc.update(
            "prayedBy" to FieldValue.arrayUnion(key),
            "updatedAt" to Timestamp.now(),
        )
        return true
    }
}

private fun PrayerRequestDto.toDomain(id: String) = PrayerRequestItem(
    id = id,
    name = name,
    request = request,
    isUrgent = isUrgent,
    isAnonymous = isAnonymous,
    status = status,
    prayedBy = prayedBy,
    createdAt = createdAt?.toInstant(),
)
