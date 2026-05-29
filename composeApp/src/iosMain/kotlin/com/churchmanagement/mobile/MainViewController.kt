package com.churchmanagement.mobile

import androidx.compose.ui.window.ComposeUIViewController
import com.churchmanagement.mobile.di.initKoin

private val doInitKoin by lazy { initKoin() }

fun MainViewController() = ComposeUIViewController {
    doInitKoin
    App()
}
