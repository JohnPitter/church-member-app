package com.churchmanagement.mobile.data

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

/**
 * Escopo de processo que mantém vivas as subscriptions dos fluxos compartilhados.
 * O handler é uma rede de segurança: nenhum erro de listener pode derrubar o app.
 */
private val repoScope = CoroutineScope(
    SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
        println("[SharedFlows] erro nao tratado no escopo: ${e.message}")
    }
)

/** Memo de StateFlows por chave — uma subscription do Firestore por consulta, reaproveitada por toda a UI. */
private val sharedFlows = mutableMapOf<String, StateFlow<*>>()

/**
 * Memoiza este fluxo como um StateFlow QUENTE e compartilhado por [key]: a subscription do Firestore
 * fica viva (SharingStarted.Eagerly), então telas que re-assinam ao navegar recebem o último valor
 * NA HORA — sem re-buscar nem mostrar loading de novo. `null` = ainda carregando (1ª vez).
 *
 * Use com `collectAsState()` (sem initial) para que a tela já abra com o valor em cache.
 */
@Suppress("UNCHECKED_CAST")
fun <T> Flow<T>.sharedState(key: String): StateFlow<T?> {
    // .catch garante que um erro de listener (ex.: PERMISSION_DENIED, rede) vire `null`
    // (tratado como "sem dados") em vez de crashar o app.
    val upstream: Flow<T?> = this
    return sharedFlows.getOrPut(key) {
        upstream
            .catch { e ->
                println("[SharedFlows] erro em '$key': ${e.message}")
                emit(null)
            }
            .stateIn(repoScope, SharingStarted.Eagerly, null)
    } as StateFlow<T?>
}
