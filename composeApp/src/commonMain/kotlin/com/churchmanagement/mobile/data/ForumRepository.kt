package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.firestore.toInstant
import com.churchmanagement.mobile.data.model.CreateForumReplyDto
import com.churchmanagement.mobile.data.model.CreateForumTopicDto
import com.churchmanagement.mobile.data.model.ForumCategoryEmbedDto
import com.churchmanagement.mobile.data.model.ForumReplyDto
import com.churchmanagement.mobile.data.model.ForumTopicDto
import com.churchmanagement.mobile.domain.AppUser
import com.churchmanagement.mobile.domain.ForumCategory
import com.churchmanagement.mobile.domain.ForumReply
import com.churchmanagement.mobile.domain.ForumTopic
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class ForumRepository(private val firestore: FirebaseFirestore) {

    /** Tópicos do fórum: fixados primeiro, depois mais recentes (StateFlow quente). `null` = carregando. */
    fun observeTopics(): StateFlow<List<ForumTopic>?> =
        firestore.collection(Collections.FORUM_TOPICS).snapshots.map { snapshot ->
            snapshot.documents
                .mapNotNull { doc ->
                    runCatching { doc.data(ForumTopicDto.serializer()).toDomain(doc.id) }.getOrNull()
                }
                .sortedWith(
                    compareByDescending<ForumTopic> { it.isPinned }
                        .thenByDescending { it.createdAt }
                )
        }.sharedState("forumTopics")

    /** Categorias ativas do fórum, na ordem de exibição (StateFlow quente). `null` = carregando. */
    fun observeCategories(): StateFlow<List<ForumCategory>?> =
        firestore.collection(Collections.FORUM_CATEGORIES).snapshots.map { snapshot ->
            snapshot.documents
                .mapNotNull { doc ->
                    runCatching { doc.id to doc.data(ForumCategoryEmbedDto.serializer()) }.getOrNull()
                }
                .filter { (_, dto) -> dto.isActive }
                .sortedBy { (_, dto) -> dto.displayOrder }
                .map { (id, dto) -> ForumCategory(id = id, name = dto.name, color = dto.color) }
        }.sharedState("forumCategories")

    /** Cria um novo tópico. Reembute a categoria escolhida (como o web faz). */
    suspend fun addTopic(categoryId: String, title: String, content: String, author: AppUser) {
        val catSnapshot = firestore.collection(Collections.FORUM_CATEGORIES).document(categoryId).get()
        val category = catSnapshot.data(ForumCategoryEmbedDto.serializer()).copy(id = categoryId)
        val now = Timestamp.now()
        val topic = CreateForumTopicDto(
            title = title.trim(),
            content = content.trim(),
            categoryId = categoryId,
            category = category,
            authorId = author.uid,
            authorName = author.displayName ?: (author.email ?: "Membro"),
            authorEmail = author.email ?: "",
            createdBy = author.uid,
            createdAt = now,
            updatedAt = now,
        )
        firestore.collection(Collections.FORUM_TOPICS).add(topic)
    }

    /** Publica uma resposta num tópico. A contagem na tela recalcula via snapshot. */
    suspend fun addReply(topicId: String, content: String, author: AppUser) {
        val now = Timestamp.now()
        val dto = CreateForumReplyDto(
            topicId = topicId,
            content = content.trim(),
            authorId = author.uid,
            authorName = author.displayName ?: (author.email ?: "Membro"),
            authorEmail = author.email ?: "",
            createdBy = author.uid,
            createdAt = now,
            updatedAt = now,
        )
        firestore.collection(Collections.FORUM_REPLIES).add(dto)
    }

    /** Respostas de um tópico, mais antigas primeiro (StateFlow quente). `null` = carregando. */
    fun observeReplies(topicId: String): StateFlow<List<ForumReply>?> =
        firestore.collection(Collections.FORUM_REPLIES).snapshots.map { snapshot ->
            snapshot.documents
                .mapNotNull { doc ->
                    runCatching {
                        val dto = doc.data(ForumReplyDto.serializer())
                        if (dto.topicId == topicId) dto.toReply(doc.id) else null
                    }.getOrNull()
                }
                .sortedBy { it.createdAt }
        }.sharedState("forumReplies:$topicId")
}

private fun ForumTopicDto.toDomain(id: String) = ForumTopic(
    id = id,
    title = title,
    content = content,
    authorName = authorName,
    categoryName = category?.name ?: "",
    isPinned = isPinned,
    replyCount = replyCount,
    createdAt = createdAt?.toInstant(),
)

private fun ForumReplyDto.toReply(id: String) = ForumReply(
    id = id,
    content = content,
    authorName = authorName,
    createdAt = createdAt?.toInstant(),
)
