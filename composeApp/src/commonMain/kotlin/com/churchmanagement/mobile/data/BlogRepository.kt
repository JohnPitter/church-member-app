package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.firestore.toInstant
import com.churchmanagement.mobile.data.model.BlogPostDto
import com.churchmanagement.mobile.domain.BlogPost
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BlogRepository(private val firestore: FirebaseFirestore) {

    /** Posts publicados visíveis a membros (público ou somente-membros), recentes primeiro. */
    fun observePublished(limit: Int? = null): Flow<List<BlogPost>> =
        firestore.collection(Collections.BLOG_POSTS).snapshots.map { snapshot ->
            val posts = snapshot.documents
                .mapNotNull { doc ->
                    runCatching {
                        val dto = doc.data(BlogPostDto.serializer())
                        val visible = dto.visibility == "public" || dto.visibility == "members_only"
                        if (dto.status == "published" && visible) dto.toDomain(doc.id) else null
                    }.getOrNull()
                }
                .filter { it.publishedAt != null }
                .sortedByDescending { it.publishedAt }
            if (limit != null) posts.take(limit) else posts
        }
}

private fun BlogPostDto.toDomain(id: String) = BlogPost(
    id = id,
    title = title,
    excerpt = excerpt,
    content = content,
    authorName = author?.name ?: "",
    featuredImage = featuredImage,
    publishedAt = publishedAt?.toInstant(),
    tags = tags,
)
