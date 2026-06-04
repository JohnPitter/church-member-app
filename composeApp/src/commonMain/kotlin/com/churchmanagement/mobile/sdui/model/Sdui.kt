package com.churchmanagement.mobile.sdui.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Modelos do Server-Driven UI. Uma tela inteira é descrita por uma [ScreenSpec],
 * que vem do Firestore (`appScreens/{id}`) serializada como JSON.
 *
 * O nó é GENÉRICO de propósito (`type: String` + `props`), e não uma sealed class:
 * assim o app tolera tipos de componente que ainda não conhece (forward-compat) — o
 * renderizador simplesmente ignora o que não está no catálogo.
 */

@Serializable
data class ScreenSpec(
    val schemaVersion: Int = 1,
    val id: String = "",
    val title: String = "",
    /** Versão mínima do app capaz de desenhar esta tela; apps mais antigos a ignoram. */
    val minAppVersion: Int = 0,
    val root: UiNode,
)

/** Nó genérico da árvore de UI. */
@Serializable
data class UiNode(
    val type: String,
    val id: String? = null,
    val props: JsonObject = JsonObject(emptyMap()),
    val children: List<UiNode> = emptyList(),
    /** Modelo de item para o componente `list`: renderizado uma vez por registro, com bindings `{{item.*}}`. */
    val itemTemplate: UiNode? = null,
    val action: UiAction? = null,
)

/** Configuração do shell do app (abas/navegação), vinda de `appConfig/main`. */
@Serializable
data class AppConfigSpec(
    val schemaVersion: Int = 1,
    val minAppVersion: Int = 0,
    val tabs: List<TabSpec> = emptyList(),
)

/** Uma aba da barra inferior. */
@Serializable
data class TabSpec(
    val label: String = "",
    val icon: String = "",
    /** Rota: "dynamic:<id>" (tela server-driven) ou rota nativa ("events", "profile"...). */
    val screen: String = "",
)

/** Ação disparada por um componente clicável. */
@Serializable
data class UiAction(
    /** navigate | openUrl | back */
    val type: String,
    /** Rota declarativa ("events"), "dynamic:<id>" ou rota de detalhe ("eventDetail"). */
    val screen: String? = null,
    val url: String? = null,
    /** Parâmetro da rota (ex.: id do item); aceita binding `{{item.id}}`. */
    val param: String? = null,
)

/**
 * JSON tolerante: ignora chaves desconhecidas para que um schema mais novo (campos extras)
 * não quebre apps antigos. `explicitNulls = false` mantém os documentos enxutos.
 */
val SduiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

// ---- Helpers de leitura de props ----

fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
