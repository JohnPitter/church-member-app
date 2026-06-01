package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.firestore.toInstant
import com.churchmanagement.mobile.data.model.MemberBirthdayDto
import com.churchmanagement.mobile.domain.Birthday
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class MemberRepository(private val firestore: FirebaseFirestore) {

    /**
     * Aniversariantes do mês informado (1-12), ordenados por dia.
     * O dia/mês é extraído em UTC porque as datas estão gravadas de forma inconsistente
     * (umas em 00:00Z, outras no fuso local) — UTC dá o dia correto em ambos os casos.
     */
    fun observeBirthdays(month: Int): Flow<List<Birthday>> =
        firestore.collection(Collections.MEMBERS).snapshots.map { snapshot ->
            snapshot.documents
                .mapNotNull { doc ->
                    runCatching {
                        val dto = doc.data(MemberBirthdayDto.serializer())
                        val timestamp = dto.birthDate ?: dto.dataNascimento
                        val inactive = dto.status == "inactive" || dto.status == "transferred"
                        if (timestamp != null && !inactive && dto.name.isNotBlank()) {
                            val date = timestamp.toInstant().toLocalDateTime(TimeZone.UTC).date
                            Birthday(
                                id = doc.id,
                                name = dto.name,
                                day = date.dayOfMonth,
                                month = date.monthNumber,
                                photoUrl = dto.photoURL,
                            )
                        } else {
                            null
                        }
                    }.getOrNull()
                }
                .filter { it.month == month }
                .sortedBy { it.day }
        }
}
