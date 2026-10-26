package com.churchmanagement.mobile.util

import androidx.compose.ui.platform.UriHandler

/**
 * Links públicos do sistema web (Firebase Hosting do projeto `church-management-ibc`).
 * As lojas (Google Play / App Store) exigem uma URL de política de privacidade e uma URL
 * para solicitar a exclusão da conta fora do app — ambas são páginas públicas do site.
 */
object AppLinks {
    const val WEB_BASE_URL = "https://church-management-ibc.web.app"
    const val PRIVACY_POLICY_URL = "$WEB_BASE_URL/privacidade"
    const val DELETE_ACCOUNT_URL = "$WEB_BASE_URL/excluir-conta"

    /** E-mail oficial da igreja (controlador/encarregado, direitos LGPD e pedidos de exclusão). */
    const val CHURCH_CONTACT_EMAIL = "igreja.ibc.cacote@gmail.com"
    const val CHURCH_CONTACT_MAILTO = "mailto:$CHURCH_CONTACT_EMAIL"
}

/**
 * Abre um link sem derrubar o app quando não há app para tratá-lo (ex.: `mailto:` sem cliente
 * de e-mail instalado). Retorna false se não foi possível abrir.
 */
fun UriHandler.openSafely(uri: String): Boolean = try {
    openUri(uri)
    true
} catch (_: Exception) {
    false
}
