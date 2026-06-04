package com.churchmanagement.mobile.sdui.data

import com.churchmanagement.mobile.sdui.SduiLog
import com.churchmanagement.mobile.sdui.model.AppConfigSpec
import com.churchmanagement.mobile.sdui.model.SduiJson
import com.churchmanagement.mobile.sdui.platform.appVersionCode
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

/**
 * Lê o shell server-driven do app (abas/navegação) de `appConfig/main` (campo `json` = [AppConfigSpec]).
 * Retorna `null` quando ausente/inválido/exige versão mais nova — o app então cai nas abas nativas padrão.
 * Nunca lança.
 */
class AppConfigRepository(private val firestore: FirebaseFirestore) {

    fun observeConfig(): Flow<AppConfigSpec?> =
        firestore.collection(COLLECTION).document(DOC).snapshots.map { snapshot ->
            val dto = runCatching { snapshot.data(ConfigDocDto.serializer()) }.getOrNull()
            parse(dto?.json)
        }

    private fun parse(raw: String?): AppConfigSpec? {
        if (raw.isNullOrBlank()) {
            SduiLog.d("appConfig ausente — usando abas nativas padrão")
            return null
        }
        val spec = runCatching { SduiJson.decodeFromString(AppConfigSpec.serializer(), raw) }
            .onFailure { SduiLog.e("Falha ao parsear appConfig", it) }
            .getOrNull() ?: return null

        if (spec.minAppVersion > appVersionCode) {
            SduiLog.d("appConfig exige app v${spec.minAppVersion} (atual v$appVersionCode) — usando padrão")
            return null
        }
        if (spec.tabs.isEmpty()) {
            SduiLog.d("appConfig sem abas — usando padrão")
            return null
        }
        SduiLog.d("appConfig carregado (${spec.tabs.size} abas)")
        return spec
    }

    private companion object {
        const val COLLECTION = "appConfig"
        const val DOC = "main"
    }
}

@Serializable
private data class ConfigDocDto(val json: String = "")
