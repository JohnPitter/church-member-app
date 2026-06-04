package com.churchmanagement.mobile.sdui.data

import com.churchmanagement.mobile.sdui.SduiLog
import com.churchmanagement.mobile.sdui.model.ScreenSpec
import com.churchmanagement.mobile.sdui.model.SduiJson
import com.churchmanagement.mobile.sdui.platform.appVersionCode
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable

/** Resultado da carga de uma tela server-driven. */
sealed interface LayoutResult {
    data class Ready(val spec: ScreenSpec) : LayoutResult
    /** Tela ausente, com JSON inválido ou que exige uma versão de app mais nova. */
    data object Unavailable : LayoutResult
}

/**
 * Lê layouts server-driven de `appScreens/{id}`. O documento tem um único campo `json`
 * com a [ScreenSpec] serializada como string — isso mantém o parsing robusto (sem depender
 * do decoder estruturado do Firestore para árvores recursivas) e o futuro painel de edição trivial.
 *
 * Nunca lança: qualquer falha vira [LayoutResult.Unavailable] e é logada, para que um documento
 * malformado jamais derrube o app de todos os usuários.
 */
class LayoutRepository(private val firestore: FirebaseFirestore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Fluxo QUENTE e compartilhado por tela: a subscription fica viva, então revisitar
    // entrega o `.value` na hora (sem re-buscar nem mostrar loading). null = carregando.
    private val streams = mutableMapOf<String, StateFlow<LayoutResult?>>()

    fun stream(id: String): StateFlow<LayoutResult?> =
        streams.getOrPut(id) {
            firestore.collection(COLLECTION).document(id).snapshots
                .map { snapshot ->
                    val dto = runCatching { snapshot.data(ScreenDocDto.serializer()) }.getOrNull()
                    parse(id, dto?.json)
                }
                .stateIn(scope, SharingStarted.Eagerly, null)
        }

    private fun parse(id: String, raw: String?): LayoutResult {
        if (raw.isNullOrBlank()) {
            SduiLog.d("Tela '$id' sem layout publicado")
            return LayoutResult.Unavailable
        }
        val spec = runCatching { SduiJson.decodeFromString(ScreenSpec.serializer(), raw) }
            .onFailure { SduiLog.e("Falha ao parsear layout '$id'", it) }
            .getOrNull() ?: return LayoutResult.Unavailable

        if (spec.minAppVersion > appVersionCode) {
            SduiLog.d("Tela '$id' exige app v${spec.minAppVersion} (atual v$appVersionCode) — ignorada")
            return LayoutResult.Unavailable
        }
        SduiLog.d("Tela '$id' carregada (schema=${spec.schemaVersion}, raiz='${spec.root.type}')")
        return LayoutResult.Ready(spec)
    }

    private companion object {
        const val COLLECTION = "appScreens"
    }
}

/** O documento carrega a ScreenSpec como uma string JSON única no campo `json`. */
@Serializable
private data class ScreenDocDto(val json: String = "")
