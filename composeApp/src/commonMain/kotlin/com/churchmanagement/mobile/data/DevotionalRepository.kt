package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.firestore.toInstant
import com.churchmanagement.mobile.data.model.DevotionalDto
import com.churchmanagement.mobile.domain.Devotional
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DevotionalRepository(private val firestore: FirebaseFirestore) {

    /** Devocionais publicados, mais recentes primeiro. */
    fun observePublished(limit: Int? = null): Flow<List<Devotional>> =
        firestore.collection(Collections.DEVOTIONALS).snapshots.map { snapshot ->
            val items = snapshot.documents
                .mapNotNull { doc ->
                    runCatching {
                        val dto = doc.data(DevotionalDto.serializer())
                        if (dto.isPublished) dto.toDomain(doc.id) else null
                    }.getOrNull()
                }
                .filter { it.publishDate != null }
                .sortedByDescending { it.publishDate }
            if (limit != null) items.take(limit) else items
        }
}

private fun DevotionalDto.toDomain(id: String) = Devotional(
    id = id,
    title = title,
    content = content,
    bibleVerse = bibleVerse,
    bibleReference = bibleReference,
    reflection = reflection,
    prayer = prayer,
    author = author,
    publishDate = publishDate?.toInstant(),
    tags = tags,
    imageUrl = imageUrl,
    readingTime = readingTime,
)
