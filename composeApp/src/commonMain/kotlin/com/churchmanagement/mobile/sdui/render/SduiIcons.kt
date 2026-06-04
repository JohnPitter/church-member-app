package com.churchmanagement.mobile.sdui.render

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Diversity3
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector

/** Mapeia uma chave de ícone (string vinda do JSON) para um ImageVector. Chave desconhecida → Info. */
fun sduiIcon(key: String?): ImageVector = when (key) {
    "home" -> Icons.Outlined.Home
    "calendar", "events" -> Icons.Outlined.CalendarMonth
    "book", "devotionals" -> Icons.AutoMirrored.Outlined.MenuBook
    "bell", "notifications" -> Icons.Outlined.Notifications
    "person", "profile" -> Icons.Outlined.Person
    "article", "blog" -> Icons.AutoMirrored.Outlined.Article
    "forum" -> Icons.Outlined.Forum
    "projects" -> Icons.Outlined.Diversity3
    "live", "lives" -> Icons.Outlined.LiveTv
    "groups", "leadership" -> Icons.Outlined.Groups
    "prayer" -> Icons.Outlined.VolunteerActivism
    "cake", "birthdays" -> Icons.Outlined.Cake
    "star", "novidades" -> Icons.Outlined.AutoAwesome
    else -> Icons.Outlined.Info
}
