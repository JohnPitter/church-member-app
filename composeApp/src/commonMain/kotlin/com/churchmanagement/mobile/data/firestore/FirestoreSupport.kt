package com.churchmanagement.mobile.data.firestore

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.Instant

/** Nomes das coleções do Firestore (mesmos usados pelo web app). */
object Collections {
    const val EVENTS = "events"
    const val DEVOTIONALS = "devotionals"
    const val NOTIFICATIONS = "notifications"
    const val USERS = "users"
    const val BLOG_POSTS = "blogPosts"
    const val FORUM_TOPICS = "forumTopics"
    const val FORUM_REPLIES = "forumReplies"
    const val FORUM_CATEGORIES = "forumCategories"
    const val PROJECTS = "projects"
    const val LIVE_STREAMS = "liveStreams"
    const val LEADERS = "leaders"
    const val PRAYER_REQUESTS = "prayerRequests"
    const val EVENT_CONFIRMATIONS = "eventConfirmations"
    const val SETTINGS = "settings"
    const val SETTINGS_DOC_CHURCH = "church"
    const val MEMBERS = "members"
}

fun Timestamp.toInstant(): Instant =
    Instant.fromEpochSeconds(seconds, nanoseconds.toLong())
