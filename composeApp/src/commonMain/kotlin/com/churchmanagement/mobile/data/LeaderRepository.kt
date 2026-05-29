package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.model.LeaderDto
import com.churchmanagement.mobile.domain.Leader
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LeaderRepository(private val firestore: FirebaseFirestore) {

    /** Líderes ativos, na ordem definida pela liderança. */
    fun observeLeaders(): Flow<List<Leader>> =
        firestore.collection(Collections.LEADERS).snapshots.map { snapshot ->
            snapshot.documents
                .mapNotNull { doc ->
                    runCatching {
                        val dto = doc.data(LeaderDto.serializer())
                        if (dto.status == "ativo") doc.id to dto else null
                    }.getOrNull()
                }
                .sortedBy { (_, dto) -> dto.ordem }
                .map { (id, dto) -> dto.toDomain(id) }
        }
}

private val ROLE_LABELS = mapOf(
    "pastor" to "Pastor",
    "auxiliar" to "Pastor Auxiliar",
    "diacono" to "Diácono",
    "lider" to "Líder",
    "coordenador" to "Coordenador",
    "missionario" to "Missionário",
    "evangelista" to "Evangelista",
)

private fun LeaderDto.toDomain(id: String): Leader {
    val roleLabel = when {
        cargo == "outro" && !cargoPersonalizado.isNullOrBlank() -> cargoPersonalizado
        else -> ROLE_LABELS[cargo] ?: cargo.replaceFirstChar { it.uppercase() }
    }
    return Leader(
        id = id,
        name = nome,
        role = roleLabel,
        ministry = ministerio,
        bio = bio,
        photo = foto,
    )
}
