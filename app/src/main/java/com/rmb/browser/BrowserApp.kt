package com.rmb.browser

import android.app.Application
import android.webkit.WebView

class BrowserApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable remote debugging globally
        WebView.setWebContentsDebuggingEnabled(true)
    }
}
