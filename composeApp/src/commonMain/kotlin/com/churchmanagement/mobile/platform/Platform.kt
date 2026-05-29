package com.churchmanagement.mobile.platform

import kotlinx.coroutines.flow.StateFlow
import org.koin.core.module.Module

/** Compartilhamento de texto via folha de compartilhamento nativa. */
interface Sharer {
    fun share(text: String)
}

/** Observa o estado de conexão com a internet. */
interface ConnectivityObserver {
    val isOnline: StateFlow<Boolean>
}

/** Exibe notificações do sistema (bandeja). */
interface Notifier {
    fun show(title: String, message: String)
}

/** Fornece o token FCM do dispositivo (para push). Null se indisponível. */
interface PushTokenProvider {
    suspend fun token(): String?
}

/** Módulo Koin com as implementações específicas de cada plataforma. */
expect fun platformModule(): Module
