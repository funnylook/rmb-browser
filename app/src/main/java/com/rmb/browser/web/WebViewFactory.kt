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

object WebViewFactory {

    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("UNUSED_PARAMETER")
    fun createWebView(
        context: Context,
        tabId: String,
        isIncognito: Boolean,
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
                // JavaScript
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true

                // Cache
                cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT

                // Display
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false

                // Media
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = false

                // Mixed content
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                // User agent
                userAgentString = userAgentString + " RmbBrowser/1.0"

                // Incognito
                if (isIncognito) {
                    @Suppress("DEPRECATION")
                    saveFormData = false
                    @Suppress("DEPRECATION")
                    savePassword = false
                }
            }

            // Cookie settings
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webView, !isIncognito)
            }

            // WebViewClient
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    url?.let { onPageStarted(it) }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    url?.let { onPageFinished(it) }
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        onReceivedError(error?.description?.toString() ?: "加载失败")
                    }
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    // Allow proceeding with SSL errors for developer flexibility
                    handler?.proceed()
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false // Let WebView handle all URLs
                }
            }

            // WebChromeClient
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

                // Support file upload
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    // File upload not supported in this version
                    return false
                }
            }

            // Scroll bar
            isScrollbarFadingEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

            // Developer tools
            WebView.setWebContentsDebuggingEnabled(true)
        }
        return webView
    }
}
