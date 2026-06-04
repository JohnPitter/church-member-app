package com.churchmanagement.mobile.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

/** Destinos do app. As abas de topo são os destinos sem parâmetros. */
sealed interface Screen {
    data object Home : Screen
    data object Events : Screen
    data object Devotionals : Screen
    data object Notifications : Screen
    data object Profile : Screen

    // Seções secundárias (acessadas pela Início, navegação push)
    data object Blog : Screen
    data object Forum : Screen
    data object Projects : Screen
    data object Lives : Screen
    data object Leadership : Screen
    data object Prayer : Screen
    data object NewTopic : Screen
    data object Birthdays : Screen

    /** Tela renderizada por um layout server-driven (`appScreens/{id}`). */
    data class Dynamic(val id: String, val title: String = "") : Screen

    data class EventDetail(val id: String) : Screen
    data class DevotionalDetail(val id: String) : Screen
    data class BlogDetail(val id: String) : Screen
    data class ForumTopicDetail(val id: String) : Screen
    data class ProjectDetail(val id: String) : Screen
}

enum class Tab(val screen: Screen, val label: String, val icon: ImageVector) {
    HOME(Screen.Home, "Início", Icons.Outlined.Home),
    EVENTS(Screen.Events, "Eventos", Icons.Outlined.CalendarMonth),
    DEVOTIONALS(Screen.Devotionals, "Devoção", Icons.AutoMirrored.Outlined.MenuBook),
    NOTIFICATIONS(Screen.Notifications, "Avisos", Icons.Outlined.Notifications),
    PROFILE(Screen.Profile, "Perfil", Icons.Outlined.Person),
}

/** Navegador simples com pilha de retorno para telas de detalhe. */
class Navigator(start: Screen = Screen.Home) {
    var current by mutableStateOf(start)
        private set

    private val backStack = mutableStateListOf<Screen>()

    val canGoBack: Boolean get() = backStack.isNotEmpty()

    /** Empilha o destino atual e abre um novo (usado para telas de detalhe). */
    fun push(screen: Screen) {
        backStack.add(current)
        current = screen
    }

    /** Define a tela-raiz de uma aba (limpa a pilha de detalhes). */
    fun selectRoot(screen: Screen) {
        backStack.clear()
        current = screen
    }

    fun back(): Boolean {
        if (backStack.isEmpty()) return false
        current = backStack.removeAt(backStack.lastIndex)
        return true
    }
}

/**
 * Mapeia uma rota declarativa (vinda de uma ação no JSON do SDUI) para um destino.
 * `"dynamic:<id>"` abre outra tela server-driven; rotas de detalhe usam [param] (ex.: id do item);
 * rotas desconhecidas retornam null (ignoradas).
 */
fun screenForRoute(route: String, param: String? = null): Screen? = when {
    route.startsWith("dynamic:") -> Screen.Dynamic(route.removePrefix("dynamic:"))
    route == "home" -> Screen.Home
    route == "events" -> Screen.Events
    route == "devotionals" -> Screen.Devotionals
    route == "notifications" -> Screen.Notifications
    route == "profile" -> Screen.Profile
    route == "blog" -> Screen.Blog
    route == "forum" -> Screen.Forum
    route == "projects" -> Screen.Projects
    route == "lives" -> Screen.Lives
    route == "leadership" -> Screen.Leadership
    route == "prayer" -> Screen.Prayer
    route == "birthdays" -> Screen.Birthdays
    route == "newTopic" -> Screen.NewTopic
    route == "eventDetail" -> param?.takeIf { it.isNotBlank() }?.let { Screen.EventDetail(it) }
    route == "devotionalDetail" -> param?.takeIf { it.isNotBlank() }?.let { Screen.DevotionalDetail(it) }
    route == "blogDetail" -> param?.takeIf { it.isNotBlank() }?.let { Screen.BlogDetail(it) }
    route == "forumTopicDetail" -> param?.takeIf { it.isNotBlank() }?.let { Screen.ForumTopicDetail(it) }
    route == "projectDetail" -> param?.takeIf { it.isNotBlank() }?.let { Screen.ProjectDetail(it) }
    else -> null
}
