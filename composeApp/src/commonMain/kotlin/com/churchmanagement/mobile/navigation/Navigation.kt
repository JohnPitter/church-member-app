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

    /** Troca de aba: limpa a pilha de detalhes. */
    fun selectTab(tab: Tab) {
        backStack.clear()
        current = tab.screen
    }

    fun back(): Boolean {
        if (backStack.isEmpty()) return false
        current = backStack.removeAt(backStack.lastIndex)
        return true
    }

    /** A aba ativa correspondente ao destino atual (null em telas de detalhe). */
    val activeTab: Tab? get() = Tab.entries.firstOrNull { it.screen == current }
}
