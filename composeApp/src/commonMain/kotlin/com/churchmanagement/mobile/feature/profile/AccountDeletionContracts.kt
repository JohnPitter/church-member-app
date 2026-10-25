package com.churchmanagement.mobile.feature.profile

/**
 * Regras puras da exclusão da própria conta (espelham a página web `/excluir-conta`
 * e a função `deleteOwnAccount`). Sem dependência de Firebase para poder ser testado em commonTest.
 */

/** Palavra que o usuário precisa digitar para confirmar a exclusão. */
const val DELETE_CONFIRMATION_WORD = "EXCLUIR"

/** Mensagem exibida na tela de login depois que a conta foi excluída. */
const val ACCOUNT_DELETED_NOTICE = "Sua conta foi excluída."

/** Espaços em volta são tolerados (teclados móveis costumam inserir um espaço final). */
fun isDeleteConfirmationValid(input: String): Boolean = input.trim() == DELETE_CONFIRMATION_WORD

/** Como o app consegue confirmar a identidade antes da exclusão. */
enum class ReauthMethod {
    /** Conta com senha (único login suportado no app): reautentica com e-mail + senha. */
    PASSWORD,

    /** Conta sem senha (ex.: só Google): o app não tem login Google, a exclusão é feita no site. */
    WEB_ONLY,
}

fun reauthMethodFor(providerIds: List<String>): ReauthMethod =
    if ("password" in providerIds) ReauthMethod.PASSWORD else ReauthMethod.WEB_ONLY

fun canSubmitDeletion(method: ReauthMethod, password: String, confirmation: String): Boolean =
    method == ReauthMethod.PASSWORD && password.isNotEmpty() && isDeleteConfirmationValid(confirmation)

enum class DeletionErrorKind {
    /** Último administrador aprovado (failed-precondition / last-admin). */
    LAST_ADMIN,

    /** Confirmação de identidade expirou (unauthenticated / recent-login-required): pedir de novo. */
    REAUTH_AGAIN,
    WRONG_PASSWORD,
    TOO_MANY_REQUESTS,
    NETWORK,
    SESSION_EXPIRED,
    GENERIC,
}

data class DeletionError(val kind: DeletionErrorKind, val message: String)

const val LAST_ADMIN_TITLE = "Não é possível excluir o último administrador"
private const val LAST_ADMIN_FALLBACK =
    "Você é o último administrador do sistema. Promova outro usuário a administrador antes de excluir sua conta."
private const val REAUTH_AGAIN_MESSAGE =
    "Sua confirmação de identidade expirou. Confirme sua senha novamente e toque em excluir logo em seguida."
private const val GENERIC_DELETE_MESSAGE =
    "Não foi possível excluir sua conta agora. Verifique sua conexão e tente novamente."

/**
 * Converte o erro do callable `deleteOwnAccount` em mensagem para o usuário.
 * [code] é o nome do código HttpsError no formato do SDK (ex.: `FAILED_PRECONDITION`);
 * [reason] vem de `details.reason` (`last-admin`, `recent-login-required`, `unauthenticated`).
 * Mesma regra da web: sem `reason`, `failed-precondition` é tratado como último administrador.
 */
fun callableDeletionError(code: String?, reason: String?, serverMessage: String?): DeletionError {
    val normalizedCode = code?.uppercase()?.replace('-', '_')
    return when {
        reason == "last-admin" || (reason == null && normalizedCode == "FAILED_PRECONDITION") ->
            DeletionError(DeletionErrorKind.LAST_ADMIN, serverMessage.usableOr(LAST_ADMIN_FALLBACK))
        reason == "recent-login-required" || reason == "unauthenticated" || normalizedCode == "UNAUTHENTICATED" ->
            DeletionError(DeletionErrorKind.REAUTH_AGAIN, REAUTH_AGAIN_MESSAGE)
        normalizedCode == "UNAVAILABLE" || normalizedCode == "DEADLINE_EXCEEDED" ->
            DeletionError(DeletionErrorKind.NETWORK, GENERIC_DELETE_MESSAGE)
        // Só a mensagem do erro `internal` do servidor é texto em português pensado para o usuário.
        normalizedCode == "INTERNAL" ->
            DeletionError(DeletionErrorKind.GENERIC, serverMessage.usableOr(GENERIC_DELETE_MESSAGE))
        else -> DeletionError(DeletionErrorKind.GENERIC, GENERIC_DELETE_MESSAGE)
    }
}

/** Mensagem para falhas da reautenticação (antes de chamar o servidor). */
fun reauthDeletionError(kind: DeletionErrorKind): DeletionError = DeletionError(
    kind,
    when (kind) {
        DeletionErrorKind.WRONG_PASSWORD -> "Senha incorreta. Verifique e tente novamente."
        DeletionErrorKind.TOO_MANY_REQUESTS -> "Muitas tentativas. Aguarde alguns minutos e tente novamente."
        DeletionErrorKind.NETWORK -> "Erro de conexão. Verifique sua internet."
        DeletionErrorKind.SESSION_EXPIRED -> "Sua sessão expirou. Entre novamente para excluir a conta."
        else -> "Não foi possível confirmar sua identidade. Tente novamente."
    },
)

/** Extrai `details.reason` do HttpsError (Android entrega um Map; outros formatos viram null). */
fun deletionReasonFrom(details: Any?): String? = (details as? Map<*, *>)?.get("reason") as? String

private fun String?.usableOr(fallback: String): String {
    val text = this?.trim().orEmpty()
    return if (text.isEmpty() || text.equals("internal", ignoreCase = true) || text.equals("unavailable", ignoreCase = true)) {
        fallback
    } else {
        text
    }
}
