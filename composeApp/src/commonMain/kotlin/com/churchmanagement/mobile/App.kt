package com.churchmanagement.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.data.AuthRepository
import com.churchmanagement.mobile.data.NotificationRepository
import com.churchmanagement.mobile.data.SettingsRepository
import com.churchmanagement.mobile.data.UserRepository
import com.churchmanagement.mobile.domain.AppUser
import com.churchmanagement.mobile.domain.OrgSettings
import com.churchmanagement.mobile.platform.ConnectivityObserver
import com.churchmanagement.mobile.platform.Notifier
import com.churchmanagement.mobile.platform.PushTokenProvider
import com.churchmanagement.mobile.feature.auth.LoginScreen
import com.churchmanagement.mobile.feature.birthdays.BirthdaysScreen
import com.churchmanagement.mobile.feature.blog.BlogDetailScreen
import com.churchmanagement.mobile.feature.blog.BlogScreen
import com.churchmanagement.mobile.feature.forum.ForumScreen
import com.churchmanagement.mobile.feature.forum.ForumTopicDetailScreen
import com.churchmanagement.mobile.feature.forum.NewTopicScreen
import com.churchmanagement.mobile.feature.leadership.LeadershipScreen
import com.churchmanagement.mobile.feature.lives.LivesScreen
import com.churchmanagement.mobile.feature.prayer.PrayerScreen
import com.churchmanagement.mobile.feature.projects.ProjectDetailScreen
import com.churchmanagement.mobile.feature.projects.ProjectsScreen
import com.churchmanagement.mobile.feature.intro.SplashScreen
import com.churchmanagement.mobile.feature.intro.WelcomeScreen
import kotlinx.coroutines.delay
import com.churchmanagement.mobile.feature.devotionals.DevotionalDetailScreen
import com.churchmanagement.mobile.feature.devotionals.DevotionalsScreen
import com.churchmanagement.mobile.feature.events.EventDetailScreen
import com.churchmanagement.mobile.feature.events.EventsScreen
import com.churchmanagement.mobile.feature.home.HomeScreen
import com.churchmanagement.mobile.feature.notifications.NotificationsScreen
import com.churchmanagement.mobile.feature.profile.ProfileScreen
import com.churchmanagement.mobile.navigation.Navigator
import com.churchmanagement.mobile.navigation.Screen
import com.churchmanagement.mobile.navigation.Tab
import com.churchmanagement.mobile.navigation.screenForRoute
import com.churchmanagement.mobile.sdui.data.AppConfigRepository
import com.churchmanagement.mobile.sdui.feature.DynamicScreen
import com.churchmanagement.mobile.sdui.model.AppConfigSpec
import com.churchmanagement.mobile.sdui.model.UiAction
import com.churchmanagement.mobile.sdui.render.sduiIcon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import com.churchmanagement.mobile.ui.theme.AppTheme
import com.churchmanagement.mobile.ui.theme.DefaultPrimary
import com.churchmanagement.mobile.ui.theme.DefaultSecondary
import com.churchmanagement.mobile.ui.theme.parseHexColor
import org.koin.compose.koinInject

private const val SPLASH_MIN_MS = 1600L

@Composable
fun App() {
    val settingsRepo: SettingsRepository = koinInject()
    val settingsFlow = remember { settingsRepo.observeSettings() }
    val settings by settingsFlow.collectAsState(initial = OrgSettings.DEFAULT)

    AppTheme(
        primary = parseHexColor(settings.primaryColor, DefaultPrimary),
        secondary = parseHexColor(settings.secondaryColor, DefaultSecondary),
    ) {
        val auth: AuthRepository = koinInject()
        val user by auth.authState.collectAsState(initial = auth.currentUser)

        // Intro de abertura: tempo mínimo de splash.
        var splashDone by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(SPLASH_MIN_MS)
            splashDone = true
        }

        // Boas-vindas exibidas uma vez ao entrar (após abertura ou login).
        var welcomeShown by remember { mutableStateOf(false) }
        LaunchedEffect(user) {
            if (user == null) welcomeShown = false
        }

        // Registra o token FCM do dispositivo para receber push (mesmo com o app fechado).
        val pushTokenProvider: PushTokenProvider = koinInject()
        val userRepo: UserRepository = koinInject()
        LaunchedEffect(user?.uid) {
            val uid = user?.uid ?: return@LaunchedEffect
            runCatching {
                pushTokenProvider.token()?.let { token -> userRepo.addFcmToken(uid, token) }
            }
        }

        // Notificações do sistema para novos avisos enquanto o app está aberto.
        val notifier: Notifier = koinInject()
        val notificationRepo: NotificationRepository = koinInject()
        LaunchedEffect(user?.uid) {
            val uid = user?.uid ?: return@LaunchedEffect
            var baseline: Set<String>? = null
            notificationRepo.observeForUser(uid).collect { list ->
                if (list == null) return@collect // ainda carregando
                val unreadIds = list.filter { it.isUnread }.map { it.id }.toSet()
                val known = baseline
                if (known == null) {
                    baseline = unreadIds // primeira carga: estabelece a linha de base, sem notificar
                } else {
                    list.filter { it.isUnread && it.id !in known }
                        .forEach { notifier.show(it.title, it.message) }
                    baseline = unreadIds
                }
            }
        }

        val current = user
        when {
            !splashDone -> SplashScreen(
                churchName = settings.churchName,
                logoUrl = settings.logoUrl,
            )
            current == null -> LoginScreen()
            !welcomeShown -> WelcomeScreen(current) { welcomeShown = true }
            else -> MainScaffold(current)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(user: AppUser) {
    val navigator = remember { Navigator(Screen.Home) }
    val connectivity: ConnectivityObserver = koinInject()
    val online by connectivity.isOnline.collectAsState()
    val uriHandler = LocalUriHandler.current

    // Dispatcher de ações declaradas no JSON do SDUI (navegação, links externos, voltar).
    val dispatch: (UiAction) -> Unit = { action ->
        when (action.type) {
            "navigate" -> action.screen
                ?.let { screenForRoute(it, action.param) }
                ?.let { navigator.push(it) }
            "openUrl" -> action.url?.let { if (isSafeUrl(it)) uriHandler.openUri(it) }
            "back" -> navigator.back()
            else -> {}
        }
    }

    // Shell server-driven: abas vêm de appConfig; null = abas nativas padrão (fallback).
    val configRepo: AppConfigRepository = koinInject()
    val config by remember { configRepo.observeConfig() }.collectAsState(initial = null)
    val tabs = remember(config) { resolveTabs(config) }

    // Ao chegar um appConfig real, adota a 1ª aba como raiz (se o usuário ainda não navegou).
    LaunchedEffect(config) {
        val cfg = config ?: return@LaunchedEffect
        val resolved = resolveTabs(cfg)
        if (resolved.isNotEmpty() && !navigator.canGoBack && resolved.none { it.target == navigator.current }) {
            navigator.selectRoot(resolved.first().target)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleFor(navigator.current)) },
                navigationIcon = {
                    if (navigator.canGoBack) {
                        IconButton(onClick = { navigator.back() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = navigator.current == tab.target,
                        onClick = { navigator.selectRoot(tab.target) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!online) OfflineBanner()
            Box(Modifier.fillMaxSize()) {
            when (val screen = navigator.current) {
                Screen.Home -> NativeHome(user, navigator)
                Screen.Events -> NativeEvents(navigator)
                Screen.Devotionals -> NativeDevotionals(navigator)
                Screen.Notifications -> NotificationsScreen(userId = user.uid)
                Screen.Profile -> ProfileScreen(user = user)
                Screen.Blog -> NativeBlog(navigator)
                Screen.Forum -> NativeForum(navigator)
                Screen.NewTopic -> NewTopicScreen(onCreated = { navigator.back() })
                Screen.Projects -> NativeProjects(navigator)
                Screen.Lives -> LivesScreen()
                Screen.Leadership -> LeadershipScreen()
                Screen.Prayer -> PrayerScreen()
                Screen.Birthdays -> BirthdaysScreen()
                is Screen.Dynamic -> DynamicScreen(
                    screenId = screen.id,
                    user = user,
                    onAction = dispatch,
                    fallback = nativeFallbackFor(screen.id, user, navigator),
                )
                is Screen.EventDetail -> EventDetailScreen(eventId = screen.id)
                is Screen.DevotionalDetail -> DevotionalDetailScreen(devotionalId = screen.id)
                is Screen.BlogDetail -> BlogDetailScreen(postId = screen.id)
                is Screen.ForumTopicDetail -> ForumTopicDetailScreen(topicId = screen.id)
                is Screen.ProjectDetail -> ProjectDetailScreen(projectId = screen.id)
            }
            }
        }
    }
}

// ---- Telas nativas (reusadas tanto como destino quanto como fallback de telas dinâmicas) ----

@Composable
private fun NativeHome(user: AppUser, navigator: Navigator) {
    HomeScreen(
        user = user,
        onOpenEvent = { navigator.push(Screen.EventDetail(it)) },
        onOpenDevotional = { navigator.push(Screen.DevotionalDetail(it)) },
        onSeeAllEvents = { navigator.selectRoot(Screen.Events) },
        onOpenBlog = { navigator.push(Screen.Blog) },
        onOpenForum = { navigator.push(Screen.Forum) },
        onOpenProjects = { navigator.push(Screen.Projects) },
        onOpenLives = { navigator.push(Screen.Lives) },
        onOpenLeadership = { navigator.push(Screen.Leadership) },
        onOpenPrayer = { navigator.push(Screen.Prayer) },
        onOpenBirthdays = { navigator.push(Screen.Birthdays) },
        onOpenDynamic = { id, title -> navigator.push(Screen.Dynamic(id, title)) },
    )
}

@Composable
private fun NativeEvents(navigator: Navigator) {
    EventsScreen(onOpenEvent = { navigator.push(Screen.EventDetail(it)) })
}

@Composable
private fun NativeDevotionals(navigator: Navigator) {
    DevotionalsScreen(onOpenDevotional = { navigator.push(Screen.DevotionalDetail(it)) })
}

@Composable
private fun NativeBlog(navigator: Navigator) {
    BlogScreen(onOpenPost = { navigator.push(Screen.BlogDetail(it)) })
}

@Composable
private fun NativeForum(navigator: Navigator) {
    ForumScreen(
        onOpenTopic = { navigator.push(Screen.ForumTopicDetail(it)) },
        onNewTopic = { navigator.push(Screen.NewTopic) },
    )
}

@Composable
private fun NativeProjects(navigator: Navigator) {
    ProjectsScreen(onOpenProject = { navigator.push(Screen.ProjectDetail(it)) })
}

/** Fallback nativo para uma tela dinâmica indisponível (degradação graciosa da migração). */
private fun nativeFallbackFor(
    id: String,
    user: AppUser,
    navigator: Navigator,
): (@Composable () -> Unit)? = when (id) {
    "home" -> { { NativeHome(user, navigator) } }
    "events" -> { { NativeEvents(navigator) } }
    "devotionals" -> { { NativeDevotionals(navigator) } }
    "blog" -> { { NativeBlog(navigator) } }
    "forum" -> { { NativeForum(navigator) } }
    "projects" -> { { NativeProjects(navigator) } }
    "lives" -> { { LivesScreen() } }
    "leadership" -> { { LeadershipScreen() } }
    else -> null
}

@Composable
private fun OfflineBanner() {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Sem conexão. Mostrando dados salvos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 16.dp),
        )
    }
}

/** Aba resolvida para a barra inferior (label + ícone + destino). */
private data class ResolvedTab(val label: String, val icon: ImageVector, val target: Screen)

/** Abas nativas padrão (fallback quando não há appConfig). */
private fun defaultTabs(): List<ResolvedTab> =
    Tab.entries.map { ResolvedTab(it.label, it.icon, it.screen) }

/** Resolve as abas a partir do appConfig; vazio/inválido cai no padrão nativo. */
private fun resolveTabs(config: AppConfigSpec?): List<ResolvedTab> {
    if (config == null) return defaultTabs()
    val tabs = config.tabs.mapNotNull { spec ->
        val target = targetForRoute(spec.screen, spec.label) ?: return@mapNotNull null
        ResolvedTab(label = spec.label, icon = sduiIcon(spec.icon), target = target)
    }
    return tabs.ifEmpty { defaultTabs() }
}

private fun targetForRoute(route: String, label: String): Screen? = when {
    route.startsWith("dynamic:") -> Screen.Dynamic(route.removePrefix("dynamic:"), label)
    else -> screenForRoute(route)
}

/** Só permite abrir links http/https vindos do JSON (evita esquemas perigosos como javascript:). */
private fun isSafeUrl(url: String): Boolean =
    url.startsWith("https://") || url.startsWith("http://")

private fun titleFor(screen: Screen): String = when (screen) {
    Screen.Home -> "Início"
    Screen.Events -> "Eventos"
    Screen.Devotionals -> "Devocionais"
    Screen.Notifications -> "Avisos"
    Screen.Profile -> "Perfil"
    Screen.Blog -> "Blog"
    Screen.Forum -> "Fórum"
    Screen.Projects -> "Projetos"
    Screen.Lives -> "Transmissões"
    Screen.Leadership -> "Liderança"
    Screen.Prayer -> "Pedidos de Oração"
    Screen.NewTopic -> "Novo tópico"
    Screen.Birthdays -> "Aniversariantes"
    is Screen.Dynamic -> screen.title.ifBlank { "Conteúdo" }
    is Screen.EventDetail -> "Evento"
    is Screen.DevotionalDetail -> "Devocional"
    is Screen.BlogDetail -> "Publicação"
    is Screen.ForumTopicDetail -> "Tópico"
    is Screen.ProjectDetail -> "Projeto"
}
