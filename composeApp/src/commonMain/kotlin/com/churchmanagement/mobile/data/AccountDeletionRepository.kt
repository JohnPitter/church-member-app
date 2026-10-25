package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.feature.profile.ACCOUNT_DELETED_NOTICE
import com.churchmanagement.mobile.feature.profile.DeletionError
import com.churchmanagement.mobile.feature.profile.DeletionErrorKind
import com.churchmanagement.mobile.feature.profile.callableDeletionError
import com.churchmanagement.mobile.feature.profile.deletionReasonFrom
import com.churchmanagement.mobile.feature.profile.reauthDeletionError
import com.churchmanagement.mobile.platform.PushTokenProvider
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.FirebaseTooManyRequestsException
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.functions.FirebaseFunctionsException
import dev.gitlive.firebase.functions.code
import dev.gitlive.firebase.functions.details
import dev.gitlive.firebase.functions.functions
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Exclusão da própria conta (callable `deleteOwnAccount`, região southamerica-east1).
 *
 * Ordem: reautenticação com senha → ID token renovado (o servidor exige auth_time ≤ 5 min)
 * → callable → só então sign-out local e remoção do token FCM do aparelho.
 *
 * Recriação do perfil: o app não tem listener nem auto-cadastro de `users/{uid}`; as únicas escritas
 * (`fcmTokens` em [UserRepository] e no AppMessagingService) usam `update`, que falha em documento
 * inexistente — e o servidor grava o tombstone `deletedAccounts/{uid}`, que as regras usam para
 * recusar o `create`. Mesmo assim, enquanto a exclusão roda, [isDeleting] suspende o registro
 * do token FCM (ver App.kt) e o sign-out só acontece depois do sucesso do servidor.
 */
class AccountDeletionRepository(
    private val auth: FirebaseAuth,
    private val pushTokens: PushTokenProvider,
) {
    private val functions by lazy { Firebase.functions("southamerica-east1") }

    // Escopo próprio: a exclusão não pode ser cancelada no meio se a tela sair de composição
    // (ex.: o App troca para o login assim que o sign-out acontece).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)

    /** Aviso pendente para a tela de login ("Sua conta foi excluída."). */
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun consumeNotice() {
        _notice.value = null
    }

    /** Provedores de login da conta atual (ex.: `password`, `google.com`). */
    fun providerIds(): List<String> = auth.currentUser?.providerData?.map { it.providerId }.orEmpty()

    /** Exclui a conta atual. Retorna null em caso de sucesso ou o erro a exibir. */
    suspend fun deleteOwnAccount(password: String): DeletionError? =
        scope.async { runDeletion(password) }.await()

    private suspend fun runDeletion(password: String): DeletionError? {
        val user = auth.currentUser ?: return reauthDeletionError(DeletionErrorKind.SESSION_EXPIRED)
        val email = user.email ?: return reauthDeletionError(DeletionErrorKind.GENERIC)

        _isDeleting.value = true
        try {
            try {
                user.reauthenticate(EmailAuthProvider.credential(email, password))
                // Força um token novo para o servidor enxergar o auth_time da reautenticação.
                user.getIdToken(true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: FirebaseAuthInvalidCredentialsException) {
                return reauthDeletionError(DeletionErrorKind.WRONG_PASSWORD)
            } catch (_: FirebaseAuthInvalidUserException) {
                return reauthDeletionError(DeletionErrorKind.SESSION_EXPIRED)
            } catch (_: FirebaseTooManyRequestsException) {
                return reauthDeletionError(DeletionErrorKind.TOO_MANY_REQUESTS)
            } catch (_: FirebaseNetworkException) {
                return reauthDeletionError(DeletionErrorKind.NETWORK)
            } catch (failure: Exception) {
                println("[AccountDeletion] Falha na reautenticação: ${failure::class.simpleName}")
                return reauthDeletionError(DeletionErrorKind.GENERIC)
            }

            try {
                // Sem parâmetros: o servidor usa somente o uid do token de quem chama.
                functions.httpsCallable("deleteOwnAccount", timeout = 120.seconds).invoke()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: FirebaseFunctionsException) {
                return callableDeletionError(failure.code.name, deletionReasonFrom(failure.details), failure.message)
            } catch (failure: Exception) {
                println("[AccountDeletion] Falha ao chamar deleteOwnAccount: ${failure::class.simpleName}")
                return callableDeletionError(null, null, null)
            }

            // Conta excluída no servidor: aviso para o login, sign-out e limpeza local (melhor esforço).
            _notice.value = ACCOUNT_DELETED_NOTICE
            try {
                auth.signOut()
            } catch (failure: Exception) {
                println("[AccountDeletion] Sign-out após exclusão falhou: ${failure::class.simpleName}")
            }
            try {
                pushTokens.deleteToken()
            } catch (failure: Exception) {
                println("[AccountDeletion] Remoção do token FCM falhou: ${failure::class.simpleName}")
            }
            return null
        } finally {
            _isDeleting.value = false
        }
    }
}
