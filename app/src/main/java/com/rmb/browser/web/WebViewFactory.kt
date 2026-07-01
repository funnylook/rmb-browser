package com.rmb.browser.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import com.rmb.browser.devtools.DevToolsBridge
import com.rmb.browser.devtools.DevToolsJs

object WebViewFactory {

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(
        context: Context,
        @Suppress("UNUSED_PARAMETER") tabId: String,
        isIncognito: Boolean,
        devToolsBridge: DevToolsBridge? = null,
        onPageStarted: (String) -> Unit,
        onPageFinished: (String) -> Unit,
        onProgressChanged: (Int) -> Unit,
        onTitleReceived: (String) -> Unit,
        onFaviconReceived: (Bitmap?) -> Unit,
        onReceivedError: (String) -> Unit
    ): WebView {
        val webView = WebView(context)
        webView.apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                userAgentString = userAgentString + " RmbBrowser/1.0"

                if (isIncognito) {
                    @Suppress("DEPRECATION")
                    saveFormData = false
                    @Suppress("DEPRECATION")
                    savePassword = false
                }
            }

            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webView, !isIncognito)
            }

            // DevTools JS interface
            if (devToolsBridge != null) {
                addJavascriptInterface(devToolsBridge, "DevToolsBridge")
            }

            // WebViewClient
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    url?.let { onPageStarted(it) }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    url?.let { onPageFinished(it) }
                    injectDevToolsJs(view, devToolsBridge)
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    // SPA navigation may not trigger onPageFinished
                    injectDevToolsJs(view, devToolsBridge)
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        onReceivedError(error?.description?.toString() ?: "加载失败")
                    }
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handler?.proceed()
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    onProgressChanged(newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    title?.let { onTitleReceived(it) }
                }

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    onFaviconReceived(icon)
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    return false
                }
            }

            isScrollbarFadingEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        }
        return webView
    }

    private fun injectDevToolsJs(view: WebView?, bridge: DevToolsBridge?) {
        if (bridge != null && view != null) {
            view.evaluateJavascript(DevToolsJs.CONSOLE_INTERCEPTOR, null)
            view.evaluateJavascript(DevToolsJs.NETWORK_INTERCEPTOR, null)
            view.evaluateJavascript(DevToolsJs.ELEMENT_INSPECTOR, null)
        }
    }
}
