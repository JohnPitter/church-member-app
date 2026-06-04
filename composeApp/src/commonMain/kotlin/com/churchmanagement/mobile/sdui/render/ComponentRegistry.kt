package com.churchmanagement.mobile.sdui.render

import androidx.compose.runtime.Composable
import com.churchmanagement.mobile.sdui.SduiLog
import com.churchmanagement.mobile.sdui.model.UiNode

/** Assinatura de um renderizador de componente: recebe o nó e o contexto de renderização. */
typealias NodeRenderer = @Composable (node: UiNode, scope: RenderScope) -> Unit

/**
 * Catálogo de componentes que o app sabe desenhar. A chave é o `type` do JSON.
 * Tipos ausentes do catálogo são ignorados — é isto que permite publicar telas com
 * componentes novos sem derrubar apps antigos que ainda não os conhecem.
 */
object ComponentRegistry {
    val renderers: Map<String, NodeRenderer> = CoreComponents.entries
}

/** Renderiza um nó da árvore pelo seu `type`; tipo desconhecido não desenha nada. */
@Composable
fun RenderNode(node: UiNode, scope: RenderScope) {
    val renderer = ComponentRegistry.renderers[node.type]
    if (renderer == null) {
        SduiLog.d("Componente desconhecido ignorado: '${node.type}'")
        return
    }
    renderer(node, scope)
}
