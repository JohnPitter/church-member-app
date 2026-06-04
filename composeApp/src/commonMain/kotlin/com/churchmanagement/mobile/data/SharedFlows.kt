package com.churchmanagement.mobile.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Escopo de processo que mantém vivas as subscriptions dos fluxos compartilhados. */
private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
    val upstream: Flow<T?> = this
    return sharedFlows.getOrPut(key) {
        upstream.stateIn(repoScope, SharingStarted.Eagerly, null)
    } as StateFlow<T?>
}
