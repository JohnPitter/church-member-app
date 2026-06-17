package com.churchmanagement.mobile.sdui.render

import com.churchmanagement.mobile.domain.AppUser
import com.churchmanagement.mobile.sdui.model.UiAction
import com.churchmanagement.mobile.util.currentLocalDate
import com.churchmanagement.mobile.util.firstName
import com.churchmanagement.mobile.util.monthNamePt

/**
 * Contexto carregado pela árvore durante a renderização: usuário atual, dispatch de ações,
 * o item de dados corrente (dentro de um `list`) e a interpolação de texto (`{{...}}`).
 */
class RenderScope(
    val user: AppUser?,
    val onAction: (UiAction) -> Unit,
    val userId: String? = user?.uid,
    /** Em modo skeleton, as folhas (texto/imagem/etc.) renderizam placeholders shimmer no lugar do conteúdo. */
    val skeleton: Boolean = false,
    private val item: Map<String, String> = emptyMap(),
) {
    /** Deriva um escopo-filho vinculado a um item de dados (usado pelo componente `list`). */
    fun withItem(item: Map<String, String>): RenderScope =
        RenderScope(user = user, onAction = onAction, userId = userId, skeleton = skeleton, item = item)

    /** Deriva um escopo em modo skeleton (mantém a mesma árvore, mas as folhas viram shimmer). */
    fun asSkeleton(): RenderScope =
        RenderScope(user = user, onAction = onAction, userId = userId, skeleton = true, item = item)

    /** Resolve a ação (interpolando rota/url/param) e a despacha. */
    fun act(action: UiAction) {
        onAction(
            action.copy(
                screen = action.screen?.let { resolve(it) },
                url = action.url?.let { resolve(it) },
                param = action.param?.let { resolve(it) },
            )
        )
    }

    /** Substitui placeholders `{{user.*}}` e `{{item.*}}` no texto vindo do JSON. */
    fun resolve(template: String?): String {
        if (template == null) return ""
        if (!template.contains("{{")) return template
        var out = template
            .replace("{{user.firstName}}", user?.firstName() ?: "")
            .replace("{{user.displayName}}", user?.displayName ?: "")
            .replace("{{user.email}}", user?.email ?: "")
            .replace("{{month}}", monthNamePt(currentLocalDate().monthNumber))
        item.forEach { (key, value) -> out = out.replace("{{item.$key}}", value) }
        return out
    }
}
