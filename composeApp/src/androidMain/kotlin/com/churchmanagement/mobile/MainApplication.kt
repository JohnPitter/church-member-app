package com.churchmanagement.mobile

import android.app.Application
import com.churchmanagement.mobile.di.initKoin
import com.churchmanagement.mobile.platform.ensureAvisosChannel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@MainApplication)
        }
        // Canal de notificações criado cedo para que pushes em segundo plano sejam exibidos.
        ensureAvisosChannel(this)
    }
}
