package com.churchmanagement.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/** Duração mínima de loading exibida em toda página. */
const val MIN_LOADING_MS = 500L

/**
 * Mostra [loading] por NO MÍNIMO [minMs] e enquanto [ready] for false; só então renderiza [content].
 *
 * Isso dá uma sensação de carregamento uniforme em todas as telas (nenhuma "pula" o loading por
 * carregar mais rápido) e, na prática, absorve a troca cache→servidor do Firestore dentro do loading.
 * O cache continua servindo para não rechamar o banco — mas o loading de [minMs] sempre aparece.
 */
@Composable
fun LoadingGate(
    ready: Boolean,
    modifier: Modifier = Modifier,
    minMs: Long = MIN_LOADING_MS,
    key: Any? = Unit,
    loading: @Composable () -> Unit = { ListSkeleton(modifier) },
    content: @Composable () -> Unit,
) {
    // [key] reinicia o timer ao trocar de tela quando o composable é reaproveitado no mesmo
    // ponto de chamada (ex.: DynamicScreen, que serve várias telas) — assim cada tela tem seu 1s.
    var minElapsed by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        delay(minMs)
        minElapsed = true
    }
    if (minElapsed && ready) content() else loading()
}
