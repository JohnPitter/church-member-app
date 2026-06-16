package com.churchmanagement.mobile.sdui.data

import com.churchmanagement.mobile.data.BlogRepository
import com.churchmanagement.mobile.data.DevotionalRepository
import com.churchmanagement.mobile.data.EventRepository
import com.churchmanagement.mobile.data.ForumRepository
import com.churchmanagement.mobile.data.LeaderRepository
import com.churchmanagement.mobile.data.LiveStreamRepository
import com.churchmanagement.mobile.data.MemberRepository
import com.churchmanagement.mobile.data.NotificationRepository
import com.churchmanagement.mobile.data.ProjectRepository
import com.churchmanagement.mobile.domain.BlogPost
import com.churchmanagement.mobile.domain.Birthday
import com.churchmanagement.mobile.domain.Devotional
import com.churchmanagement.mobile.domain.Event
import com.churchmanagement.mobile.domain.Leader
import com.churchmanagement.mobile.domain.LiveStream
import com.churchmanagement.mobile.domain.NotificationItem
import com.churchmanagement.mobile.domain.Project
import com.churchmanagement.mobile.domain.ForumTopic
import com.churchmanagement.mobile.sdui.SduiLog
import com.churchmanagement.mobile.util.currentLocalDate
import com.churchmanagement.mobile.util.toLongDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Resolve fontes de dados declaradas no JSON (`source`) para listas de itens vinculáveis
 * (`Map<String,String>`), reaproveitando os repositórios existentes (golden source).
 * Cada item expõe campos como `{{item.title}}`, `{{item.id}}` etc. para o template do `list`.
 */
class DataResolver(
    private val events: EventRepository,
    private val devotionals: DevotionalRepository,
    private val blog: BlogRepository,
    private val projects: ProjectRepository,
    private val lives: LiveStreamRepository,
    private val leaders: LeaderRepository,
    private val notifications: NotificationRepository,
    private val forum: ForumRepository,
    private val members: MemberRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Fluxos QUENTES e compartilhados por fonte: a subscription do Firestore fica viva,
    // então revisitar a tela entrega o `.value` na hora (sem re-buscar nem piscar). null = carregando.
    private val streams = mutableMapOf<String, StateFlow<List<Map<String, String>>?>>()

    private fun key(source: String, limit: Int?, userId: String?) = "$source|$limit|$userId"

    fun stream(source: String, limit: Int?, userId: String?): StateFlow<List<Map<String, String>>?> =
        streams.getOrPut(key(source, limit, userId)) {
            base(source, limit, userId)
                // Erro no listener (ex.: PERMISSION_DENIED) → null (sem dados), sem crashar.
                .catch { e ->
                    SduiLog.e("Fonte '$source' falhou", e)
                    emit(null)
                }
                .stateIn(scope, SharingStarted.Eagerly, null)
        }

    // `null` é propagado como "ainda carregando"; lista preenchida = pronto.
    private fun base(source: String, limit: Int?, userId: String?): Flow<List<Map<String, String>>?> =
        when (source) {
            "events" -> events.observeUpcoming(limit).map { it?.map(::eventFields) }
            "devotionals" -> devotionals.observePublished(limit).map { it?.map(::devotionalFields) }
            "blog" -> blog.observePublished(limit).map { it?.map(::blogFields) }
            "projects" -> projects.observeProjects().map { it?.limited(limit)?.map(::projectFields) }
            "lives" -> lives.observeStreams().map { it?.limited(limit)?.map(::liveFields) }
            "leaders" -> leaders.observeLeaders().map { it?.limited(limit)?.map(::leaderFields) }
            "forumTopics" -> forum.observeTopics().map { it?.limited(limit)?.map(::forumTopicFields) }
            "notifications" -> notifications.observeForUser(userId.orEmpty())
                .map { it?.limited(limit)?.map(::notificationFields) }
            "birthdays" -> {
                val todayDay = currentLocalDate().dayOfMonth
                members.observeBirthdays(currentLocalDate().monthNumber)
                    .map { list -> list?.limited(limit)?.map { birthdayFields(it, todayDay) } }
            }
            else -> {
                SduiLog.d("DataSource desconhecido: '$source'")
                flowOf(emptyList())
            }
        }
}

private fun <T> List<T>.limited(limit: Int?): List<T> = if (limit != null) take(limit) else this

private fun eventFields(e: Event) = mapOf(
    "id" to e.id,
    "title" to e.title,
    "description" to e.description,
    "time" to e.time,
    "location" to e.location,
    "date" to (e.date?.toLongDate() ?: ""),
    "categoryName" to e.categoryName,
    "categoryColor" to e.categoryColor,
    "imageUrl" to (e.imageUrl ?: ""),
    "responsible" to e.responsible,
)

private fun devotionalFields(d: Devotional) = mapOf(
    "id" to d.id,
    "title" to d.title,
    "bibleVerse" to d.bibleVerse,
    "bibleReference" to d.bibleReference,
    "author" to d.author,
    "publishDate" to (d.publishDate?.toLongDate() ?: ""),
    "imageUrl" to (d.imageUrl ?: ""),
    "readingTime" to d.readingTime.toString(),
)

private fun blogFields(b: BlogPost) = mapOf(
    "id" to b.id,
    "title" to b.title,
    "excerpt" to b.excerpt,
    "authorName" to b.authorName,
    "featuredImage" to (b.featuredImage ?: ""),
    "publishedAt" to (b.publishedAt?.toLongDate() ?: ""),
)

private fun projectFields(p: Project) = mapOf(
    "id" to p.id,
    "title" to p.name,
    "description" to p.description,
    "responsible" to p.responsible,
    "category" to p.category,
    "imageUrl" to (p.imageUrl ?: ""),
    "status" to p.status,
)

private fun liveFields(l: LiveStream) = mapOf(
    "id" to l.id,
    "title" to l.title,
    "description" to l.description,
    "streamUrl" to l.streamUrl,
    "thumbnailUrl" to (l.thumbnailUrl ?: ""),
    "isLive" to l.isLive.toString(),
    "scheduledDate" to (l.scheduledDate?.toLongDate() ?: ""),
)

private fun leaderFields(l: Leader) = mapOf(
    "id" to l.id,
    "title" to l.name,
    "role" to l.role,
    "ministry" to (l.ministry ?: ""),
    "bio" to (l.bio ?: ""),
    "imageUrl" to (l.photo ?: ""),
)

private fun forumTopicFields(t: ForumTopic) = mapOf(
    "id" to t.id,
    "title" to t.title,
    "content" to t.content,
    "authorName" to t.authorName,
    "categoryName" to t.categoryName,
    "replyCount" to t.replyCount.toString(),
    "createdAt" to (t.createdAt?.toLongDate() ?: ""),
)

private fun notificationFields(n: NotificationItem) = mapOf(
    "id" to n.id,
    "title" to n.title,
    "message" to n.message,
    "type" to n.type,
    "createdAt" to (n.createdAt?.toLongDate() ?: ""),
    "isUnread" to n.isUnread.toString(),
)

private fun birthdayFields(b: Birthday, todayDay: Int) = mapOf(
    "id" to b.id,
    "title" to b.name,
    "day" to b.day.toString(),
    "dayLabel" to if (b.day == todayDay) "Hoje! 🎂" else "Dia ${b.day}",
    "imageUrl" to (b.photoUrl ?: ""),
    "phone" to (b.phone ?: ""),
)
