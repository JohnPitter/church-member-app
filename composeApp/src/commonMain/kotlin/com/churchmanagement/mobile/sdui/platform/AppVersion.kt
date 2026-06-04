package com.churchmanagement.mobile.sdui.platform

/**
 * Código de versão do build atual (espelha o `versionCode` do Gradle).
 * Usado para o gating de schema do SDUI: uma tela pode declarar `minAppVersion`
 * e apps mais antigos a ignoram graciosamente em vez de tentar desenhar algo que não conhecem.
 */
expect val appVersionCode: Int
