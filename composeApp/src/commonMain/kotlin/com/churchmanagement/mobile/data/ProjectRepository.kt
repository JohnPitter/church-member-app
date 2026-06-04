package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.firestore.toInstant
import com.churchmanagement.mobile.data.model.ProjectDto
import com.churchmanagement.mobile.domain.Project
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class ProjectRepository(private val firestore: FirebaseFirestore) {

    /** Projetos não cancelados, mais recentes primeiro (StateFlow quente). `null` = carregando. */
    fun observeProjects(): StateFlow<List<Project>?> =
        firestore.collection(Collections.PROJECTS).snapshots.map { snapshot ->
            snapshot.documents
                .mapNotNull { doc ->
                    runCatching {
                        val dto = doc.data(ProjectDto.serializer())
                        if (dto.status != "cancelled") dto.toDomain(doc.id) else null
                    }.getOrNull()
                }
                .sortedByDescending { it.startDate }
        }.sharedState("projects")
}

private fun ProjectDto.toDomain(id: String) = Project(
    id = id,
    name = name,
    description = description,
    objectives = objectives,
    startDate = startDate?.toInstant(),
    endDate = endDate?.toInstant(),
    responsible = responsible,
    category = category,
    imageUrl = imageURL,
    status = status,
)
