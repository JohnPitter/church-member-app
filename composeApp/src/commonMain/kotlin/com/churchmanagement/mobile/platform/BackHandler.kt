package com.churchmanagement.mobile.platform

import androidx.compose.runtime.Composable

/** Intercepta o "voltar" do sistema enquanto [enabled] (Android). No iOS não há botão voltar do sistema. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
