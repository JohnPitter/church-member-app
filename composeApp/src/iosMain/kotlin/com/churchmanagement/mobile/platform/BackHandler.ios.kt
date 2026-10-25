package com.churchmanagement.mobile.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS não tem botão voltar do sistema; a navegação do app já é bloqueada pela UI.
}
