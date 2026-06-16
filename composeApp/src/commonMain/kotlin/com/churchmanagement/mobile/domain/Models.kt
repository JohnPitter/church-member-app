package com.churchmanagement.mobile.domain

import kotlinx.datetime.Instant

/** Modelos de domínio consumidos pela UI (sem tipos do Firebase). */

data class OrgSettings(
    val churchName: String,
    val logoUrl: String?,
    val primaryColor: String,
    val secondaryColor: String,
) {
    companion object {
        val DEFAULT = OrgSettings(
            churchName = "Comunidade",
            logoUrl = null,
            primaryColor = "#3B82F6",
            secondaryColor = "#8B5CF6",
        )
    }
}

data class AppUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val date: Instant?,
    val time: String,
    val location: String,
    val categoryName: String,
    val categoryColor: String,
    val imageUrl: String?,
    val streamingUrl: String?,
    val responsible: String,
    val status: String,
    val requiresConfirmation: Boolean,
    val maxParticipants: Int?,
)

/** Confirmação de presença de um membro num evento (coleção `eventConfirmations`, compartilhada com o web). */
data class EventConfirmation(
    val id: String,
    val eventId: String,
    val userId: String,
    val userName: String,
    val status: String,
    val confirmedAt: Instant?,
)

data class Devotional(
    val id: String,
    val title: String,
    val content: String,
    val bibleVerse: String,
    val bibleReference: String,
    val reflection: String,
    val prayer: String,
    val author: String,
    val publishDate: Instant?,
    val tags: List<String>,
    val imageUrl: String?,
    val readingTime: Int,
)

data class BlogPost(
    val id: String,
    val title: String,
    val excerpt: String,
    val content: String,
    val authorName: String,
    val featuredImage: String?,
    val publishedAt: Instant?,
    val tags: List<String>,
)

data class ForumTopic(
    val id: String,
    val title: String,
    val content: String,
    val authorName: String,
    val categoryName: String,
    val isPinned: Boolean,
    val replyCount: Int,
    val createdAt: Instant?,
)

data class ForumCategory(
    val id: String,
    val name: String,
    val color: String,
)

data class ForumReply(
    val id: String,
    val content: String,
    val authorName: String,
    val createdAt: Instant?,
)

data class Project(
    val id: String,
    val name: String,
    val description: String,
    val objectives: List<String>,
    val startDate: Instant?,
    val endDate: Instant?,
    val responsible: String,
    val category: String,
    val imageUrl: String?,
    val status: String,
)

data class LiveStream(
    val id: String,
    val title: String,
    val description: String,
    val streamUrl: String,
    val thumbnailUrl: String?,
    val isLive: Boolean,
    val scheduledDate: Instant?,
)

data class Leader(
    val id: String,
    val name: String,
    val role: String,
    val ministry: String?,
    val bio: String?,
    val photo: String?,
)

data class Birthday(
    val id: String,
    val name: String,
    val day: Int,
    val month: Int,
    val photoUrl: String?,
    val phone: String?,
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val isUnread: Boolean,
    val createdAt: Instant?,
)
