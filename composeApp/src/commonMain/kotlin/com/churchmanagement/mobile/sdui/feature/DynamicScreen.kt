package com.churchmanagement.mobile.sdui.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.churchmanagement.mobile.domain.AppUser
import com.churchmanagement.mobile.sdui.data.LayoutRepository
import com.churchmanagement.mobile.sdui.data.LayoutResult
import com.churchmanagement.mobile.sdui.model.UiAction
import com.churchmanagement.mobile.sdui.render.RenderNode
import com.churchmanagement.mobile.sdui.render.RenderScope
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.ListSkeleton
import org.koin.compose.koinInject

/**
 * Tela renderizada inteiramente a partir de um layout server-driven (`appScreens/{screenId}`).
 * Três estados: carregando, pronta (renderiza a árvore) e indisponível. No estado indisponível
 * cai no [fallback] nativo, se houver (rede de segurança da migração), senão num aviso amigável.
 */
@Composable
fun DynamicScreen(
    screenId: String,
    user: AppUser,
    onAction: (UiAction) -> Unit,
    fallback: (@Composable () -> Unit)? = null,
) {
    val repo: LayoutRepository = koinInject()
    // StateFlow quente e compartilhado → ao revisitar a tela o layout aparece na hora, sem loading.
    val state by remember(screenId) { repo.stream(screenId) }.collectAsState()
    val scope = remember(user, onAction) { RenderScope(user = user, onAction = onAction) }

    when (val current = state) {
        null -> ListSkeleton()
        is LayoutResult.Ready -> RenderNode(current.spec.root, scope)
        LayoutResult.Unavailable -> if (fallback != null) {
            fallback()
        } else {
            EmptyState(
                title = "Conteúdo indisponível",
                subtitle = "Este conteúdo ainda não foi publicado ou requer uma versão mais nova do app.",
            )
        }
    }
}
