package com.churchmanagement.mobile.sdui

/**
 * Log simples da jornada de Server-Driven UI. Em commonMain usamos `println`,
 * que cai no Logcat (Android) e no console (iOS) — suficiente para depurar a renderização.
 */
internal object SduiLog {
    private const val TAG = "[SDUI]"

    fun d(message: String) {
        println("$TAG $message")
    }

    fun e(message: String, error: Throwable? = null) {
        println("$TAG ERRO: $message")
        if (error != null) println("$TAG ${error::class.simpleName}: ${error.message}")
    }
}
