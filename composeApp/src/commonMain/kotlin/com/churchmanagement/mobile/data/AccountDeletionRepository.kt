package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.feature.profile.ACCOUNT_DELETED_NOTICE
import com.churchmanagement.mobile.feature.profile.DeletionError
import com.churchmanagement.mobile.feature.profile.DeletionErrorKind
import com.churchmanagement.mobile.feature.profile.ReauthFailure
import com.churchmanagement.mobile.feature.profile.callableDeletionError
import com.churchmanagement.mobile.feature.profile.deletionReasonFrom
import com.churchmanagement.mobile.feature.profile.isAccountAlreadyGone
import com.churchmanagement.mobile.feature.profile.reauthDeletionError
import com.churchmanagement.mobile.feature.profile.reauthFailureError
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Exclusão da própria conta (callable `deleteOwnAccount`, região southamerica-east1).
 *
 * Ordem: reautenticação com senha → ID token renovado (o servidor exige auth_time ≤ 5 min)
 * → callable → só então sign-out local e remoção do token FCM do aparelho.
 *
 * Recriação do perfil: o app não tem listener nem auto-cadastro de `users/{uid}`; as únicas escritas
 * (`fcmTokens` em [UserRepository] e no AppMessagingService) usam `update`, que falha em documento
 * inexistente — e o servidor grava o tombstone `deletedAccounts/{uid}`, que as regras usam para
 * recusar o `create`. O sign-out local só acontece depois do sucesso do servidor; em caso de
 * falha (inclusive rede) a sessão continua intacta.
 *
 * Sessão "zumbi": se o servidor excluiu a conta mas a resposta se perdeu (timeout, rede, processo
 * encerrado), a próxima tentativa falha na reautenticação com usuário inválido/inexistente. Isso é
 * tratado como "conta já excluída": sign-out local, remoção do token FCM e aviso no login.
 */
class AccountDeletionRepository(
    private val auth: FirebaseAuth,
    private val pushTokens: PushTokenProvider,
) {
    private val functions by lazy { Firebase.functions("southamerica-east1") }

    // Escopo próprio: a exclusão não pode ser cancelada no meio se a tela sair de composição
    // (ex.: o App troca para o login assim que o sign-out acontece).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Uma exclusão por vez: toques repetidos reaproveitam a chamada em andamento.
    private val lock = Mutex()
    private var inFlight: Deferred<DeletionError?>? = null

    private val _isDeleting = MutableStateFlow(false)

    /** true enquanto a exclusão roda (a UI bloqueia navegação e novos envios). */
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
    suspend fun deleteOwnAccount(password: String): DeletionError? {
        val job = lock.withLock {
            inFlight?.takeIf { it.isActive } ?: run {
                _isDeleting.value = true
                scope.async {
                    try {
                        runDeletion(password)
                    } finally {
                        _isDeleting.value = false
                    }
                }.also { inFlight = it }
            }
        }
        return job.await()
    }

    private suspend fun runDeletion(password: String): DeletionError? {
        val user = auth.currentUser ?: return reauthDeletionError(DeletionErrorKind.SESSION_EXPIRED)
        val email = user.email ?: return reauthDeletionError(DeletionErrorKind.GENERIC)

        val reauthFailure: ReauthFailure? = try {
            user.reauthenticate(EmailAuthProvider.credential(email, password))
            // Força um token novo para o servidor enxergar o auth_time da reautenticação.
            user.getIdToken(true)
            null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            ReauthFailure.INVALID_CREDENTIALS
        } catch (_: FirebaseAuthInvalidUserException) {
            // Usuário inexistente/desativado/token revogado (ERROR_USER_NOT_FOUND, _DISABLED, _TOKEN_EXPIRED).
            ReauthFailure.INVALID_USER
        } catch (_: FirebaseTooManyRequestsException) {
            ReauthFailure.TOO_MANY_REQUESTS
        } catch (_: FirebaseNetworkException) {
            ReauthFailure.NETWORK
        } catch (failure: Exception) {
            println("[AccountDeletion] Falha na reautenticação: ${failure::class.simpleName}")
            ReauthFailure.OTHER
        }
        if (reauthFailure != null) {
            if (isAccountAlreadyGone(reauthFailure)) {
                // A conta já não existe no servidor (ex.: exclusão anterior cuja resposta se perdeu).
                finishLocally()
                return null
            }
            return reauthFailureError(reauthFailure)
        }

        try {
            // Sem parâmetros: o servidor usa somente o uid do token de quem chama.
            functions.httpsCallable("deleteOwnAccount", timeout = 120.seconds).invoke()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: FirebaseFunctionsException) {
            return callableDeletionError(failure.code.name, deletionReasonFrom(failure.details), failure.message)
        } catch (failure: Exception) {
            // Rede/timeout: não sabemos se o servidor excluiu, então NÃO deslogamos. Se excluiu,
            // a próxima tentativa cai no caso INVALID_USER acima e encerra a sessão.
            println("[AccountDeletion] Falha ao chamar deleteOwnAccount: ${failure::class.simpleName}")
            return callableDeletionError(null, null, null)
        }

        finishLocally()
        return null
    }

    /** Conta excluída no servidor: aviso para o login, sign-out e limpeza local (melhor esforço). */
    private suspend fun finishLocally() {
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
    }
}
