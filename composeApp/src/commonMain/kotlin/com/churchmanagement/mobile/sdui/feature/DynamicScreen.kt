package com.churchmanagement.mobile.sdui.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.churchmanagement.mobile.domain.AppUser
import com.churchmanagement.mobile.sdui.data.DataResolver
import com.churchmanagement.mobile.sdui.data.LayoutRepository
import com.churchmanagement.mobile.sdui.data.LayoutResult
import com.churchmanagement.mobile.sdui.model.ScreenSpec
import com.churchmanagement.mobile.sdui.model.UiAction
import com.churchmanagement.mobile.sdui.model.UiNode
import com.churchmanagement.mobile.sdui.model.int
import com.churchmanagement.mobile.sdui.model.string
import com.churchmanagement.mobile.sdui.render.RenderNode
import com.churchmanagement.mobile.sdui.render.RenderScope
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.ListSkeleton
import com.churchmanagement.mobile.ui.LoadingGate
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
        is LayoutResult.Ready -> RenderWhenReady(current.spec, scope)
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

/**
 * Só renderiza a árvore quando TODAS as fontes de dados dela já carregaram — evita o efeito de
 * seções aparecendo/mudando em momentos diferentes. Enquanto falta algo, mostra o skeleton.
 * (Com os StateFlows quentes, a revisita já vem pronta e nem pisca.)
 */
@Composable
private fun RenderWhenReady(spec: ScreenSpec, scope: RenderScope) {
    val resolver: DataResolver = koinInject()
    val sources = remember(spec) { collectSources(spec.root) }
    val allReady = sources.isEmpty() || sources.all { (source, limit) ->
        resolver.stream(source, limit, scope.userId).collectAsState().value != null
    }
    // Loading mínimo (e estende se os dados ainda não vieram). Uniformiza a sensação em todas as
    // telas e absorve a troca cache→servidor do Firestore dentro do loading. O skeleton é DERIVADO
    // do próprio layout (mesma árvore em modo skeleton), então combina com cada tela.
    // key = id da tela: o DynamicScreen serve várias telas no mesmo ponto de chamada.
    LoadingGate(
        ready = allReady,
        key = spec.id,
        loading = { RenderNode(spec.root, scope.asSkeleton()) },
    ) {
        RenderNode(spec.root, scope)
    }
}

/** Coleta recursivamente as fontes de dados (`source` + `limit`) declaradas na árvore. */
private fun collectSources(
    node: UiNode,
    acc: MutableList<Pair<String, Int?>> = mutableListOf(),
): MutableList<Pair<String, Int?>> {
    node.props.string("source")?.let { acc.add(it to node.props.int("limit")) }
    node.itemTemplate?.let { collectSources(it, acc) }
    node.children.forEach { collectSources(it, acc) }
    return acc
}
