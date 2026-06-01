package com.churchmanagement.mobile.data.model

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/**
 * DTOs que espelham os documentos do Firestore gravados pelo web app.
 * `id` não vem no corpo do documento — é preenchido a partir de `DocumentSnapshot.id`.
 * Todos os campos têm default para tolerar documentos com chaves ausentes.
 */

@Serializable
data class EventDto(
    val title: String = "",
    val description: String = "",
    val date: Timestamp? = null,
    val time: String = "",
    val location: String = "",
    val category: EventCategoryDto? = null,
    val imageURL: String? = null,
    val streamingURL: String? = null,
    val responsible: String = "",
    val status: String = "scheduled",
    val isPublic: Boolean = false,
)

@Serializable
data class EventCategoryDto(
    val id: String = "",
    val name: String = "",
    val color: String = "",
    val priority: Int = 0,
)

@Serializable
data class DevotionalDto(
    val title: String = "",
    val content: String = "",
    val bibleVerse: String = "",
    val bibleReference: String = "",
    val reflection: String = "",
    val prayer: String = "",
    val author: String = "",
    val publishDate: Timestamp? = null,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val readingTime: Int = 0,
    val isPublished: Boolean = false,
)

@Serializable
data class AuthorDto(
    val id: String = "",
    val name: String = "",
    val photoURL: String? = null,
    val role: String = "",
)

@Serializable
data class BlogPostDto(
    val title: String = "",
    val excerpt: String = "",
    val content: String = "",
    val author: AuthorDto? = null,
    val featuredImage: String? = null,
    val status: String = "draft",
    val visibility: String = "public",
    val publishedAt: Timestamp? = null,
    val tags: List<String> = emptyList(),
    val isHighlighted: Boolean = false,
)

@Serializable
data class ForumCategoryRefDto(
    val name: String = "",
    val color: String = "",
)

@Serializable
data class ForumTopicDto(
    val title: String = "",
    val content: String = "",
    val authorName: String = "",
    val category: ForumCategoryRefDto? = null,
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val replyCount: Int = 0,
    val viewCount: Int = 0,
    val createdAt: Timestamp? = null,
)

@Serializable
data class ForumReplyDto(
    val topicId: String = "",
    val content: String = "",
    val authorName: String = "",
    val createdAt: Timestamp? = null,
)

@Serializable
data class ProjectDto(
    val name: String = "",
    val description: String = "",
    val objectives: List<String> = emptyList(),
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val responsible: String = "",
    val status: String = "planning",
    val category: String = "",
    val imageURL: String? = null,
)

@Serializable
data class LiveStreamDto(
    val title: String = "",
    val description: String = "",
    val streamUrl: String = "",
    val thumbnailUrl: String? = null,
    val isLive: Boolean = false,
    val scheduledDate: Timestamp? = null,
    val status: String = "scheduled",
)

@Serializable
data class LeaderDto(
    val nome: String = "",
    val cargo: String = "",
    val cargoPersonalizado: String? = null,
    val ministerio: String? = null,
    val bio: String? = null,
    val foto: String? = null,
    val ordem: Int = 0,
    val status: String = "ativo",
)

// ---- DTOs de escrita (criação a partir do app) ----

@Serializable
data class CreatePrayerRequestDto(
    val name: String,
    val request: String,
    val isUrgent: Boolean,
    val isAnonymous: Boolean,
    val status: String = "pending",
    val source: String = "app",
    val prayedBy: List<String> = emptyList(),
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)

/** Categoria do fórum (lida de `forumCategories` e reembutida no documento do tópico). */
@Serializable
data class ForumCategoryEmbedDto(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val slug: String = "",
    val icon: String = "",
    val color: String = "",
    val isActive: Boolean = true,
    val requiresApproval: Boolean = false,
    val allowedRoles: List<String> = emptyList(),
    val topicCount: Int = 0,
    val replyCount: Int = 0,
    val moderators: List<String> = emptyList(),
    val displayOrder: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

@Serializable
data class CreateForumTopicDto(
    val title: String,
    val content: String,
    val categoryId: String,
    val category: ForumCategoryEmbedDto,
    val authorId: String,
    val authorName: String,
    val authorEmail: String,
    val tags: List<String> = emptyList(),
    val status: String = "published",
    val priority: String = "normal",
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val attachments: List<String> = emptyList(),
    val viewCount: Int = 0,
    val replyCount: Int = 0,
    val likes: List<String> = emptyList(),
    val createdBy: String,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)

@Serializable
data class CreateForumReplyDto(
    val topicId: String,
    val content: String,
    val authorId: String,
    val authorName: String,
    val authorEmail: String,
    val status: String = "published",
    val likes: List<String> = emptyList(),
    val isAcceptedAnswer: Boolean = false,
    val createdBy: String,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)

@Serializable
data class MemberBirthdayDto(
    val name: String = "",
    val phone: String? = null,
    val birthDate: Timestamp? = null,
    val dataNascimento: Timestamp? = null,
    val photoURL: String? = null,
    val status: String = "active",
)

@Serializable
data class SettingsDto(
    val churchName: String = "Comunidade",
    val logoURL: String? = null,
    val primaryColor: String = "#3B82F6",
    val secondaryColor: String = "#8B5CF6",
)

@Serializable
data class NotificationDto(
    val title: String = "",
    val message: String = "",
    val type: String = "system",
    val status: String = "unread",
    val userId: String = "",
    val createdAt: Timestamp? = null,
)
