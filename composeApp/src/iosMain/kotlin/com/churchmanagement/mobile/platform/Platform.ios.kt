package com.churchmanagement.mobile.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

// Implementações iOS mínimas. Compartilhar usa UIActivityViewController.
// Conectividade e notificações iOS ainda são stubs (a implementar quando o iOS for ativado no Mac).
actual fun platformModule(): Module = module {
    single<Sharer> { IosSharer() }
    single<ConnectivityObserver> { IosConnectivityObserver() }
    single<Notifier> { IosNotifier() }
    single<PushTokenProvider> { IosPushTokenProvider() }
}

private class IosPushTokenProvider : PushTokenProvider {
    override suspend fun token(): String? = null // TODO(iOS): APNs/FCM ao ativar a build iOS.
}

private class IosSharer : Sharer {
    override fun share(text: String) {
        val controller = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null,
        )
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController
        root?.presentViewController(controller, animated = true, completion = null)
    }
}

private class IosConnectivityObserver : ConnectivityObserver {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
}

private class IosNotifier : Notifier {
    override fun show(title: String, message: String) {
        // TODO(iOS): UNUserNotificationCenter ao ativar a build iOS no macOS.
    }
}
