package com.churchmanagement.mobile.util

/**
 * Links públicos do sistema web (Firebase Hosting do projeto `church-management-ibc`).
 * As lojas (Google Play / App Store) exigem uma URL de política de privacidade e uma URL
 * para solicitar a exclusão da conta fora do app — ambas são páginas públicas do site.
 */
object AppLinks {
    const val WEB_BASE_URL = "https://church-management-ibc.web.app"
    const val PRIVACY_POLICY_URL = "$WEB_BASE_URL/privacidade"
    const val DELETE_ACCOUNT_URL = "$WEB_BASE_URL/excluir-conta"
}
