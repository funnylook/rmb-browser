package com.rmb.browser.web

import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新标签页",
    val url: String = "",
    val favicon: Bitmap? = null,
    val isIncognito: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0
)

class TabManager(private val context: Context) {
    val tabs = mutableListOf<BrowserTab>()
    val webViews = mutableMapOf<String, WebView>()
    var activeTabId: String? = null
        private set
    var isGlobalIncognito: Boolean = false

    fun createTab(url: String = "", incognito: Boolean = false): BrowserTab {
        val tab = BrowserTab(
            url = url.ifEmpty { "about:blank" },
            isIncognito = incognito || isGlobalIncognito
        )
        tabs.add(tab)
        activeTabId = tab.id
        return tab
    }

    fun getWebView(tabId: String): WebView? = webViews[tabId]

    fun setWebView(tabId: String, webView: WebView) {
        // Remove old WebView if exists
        webViews[tabId]?.destroy()
        webViews[tabId] = webView
    }

    fun switchTab(tabId: String) {
        if (tabs.any { it.id == tabId }) {
            activeTabId = tabId
        }
    }

    fun closeTab(tabId: String) {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index == -1) return

        // Destroy WebView
        webViews[tabId]?.apply {
            stopLoading()
            destroy()
        }
        webViews.remove(tabId)

        // Clear cookies/data for incognito
        val tab = tabs[index]
        if (tab.isIncognito) {
            clearTabData(tabId)
        }

        tabs.removeAt(index)

        // Switch to adjacent tab
        if (activeTabId == tabId) {
            activeTabId = when {
                tabs.isEmpty() -> null
                index > 0 -> tabs[index - 1].id
                else -> tabs[0].id
            }
        }
    }

    fun updateTab(tabId: String, updater: (BrowserTab) -> BrowserTab) {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index != -1) {
            tabs[index] = updater(tabs[index])
        }
    }

    fun getActiveTab(): BrowserTab? = tabs.find { it.id == activeTabId }

    fun getTabIndex(tabId: String): Int = tabs.indexOfFirst { it.id == tabId }

    fun tabCount(): Int = tabs.size

    fun clearAllData() {
        // Clear all cookies
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
        // Clear all web storage
        WebStorage.getInstance().deleteAllData()
        // Destroy all WebViews
        webViews.values.forEach { it.destroy() }
        webViews.clear()
        tabs.clear()
        activeTabId = null
    }

    fun clearIncognitoData() {
        val incognitoTabs = tabs.filter { it.isIncognito }
        incognitoTabs.forEach { tab ->
            webViews[tab.id]?.apply {
                stopLoading()
                destroy()
            }
            webViews.remove(tab.id)
            clearTabData(tab.id)
        }
        tabs.removeAll { it.isIncognito }

        if (activeTabId != null && !tabs.any { it.id == activeTabId }) {
            activeTabId = tabs.lastOrNull()?.id
        }
    }

    private fun clearTabData(tabId: String) {
        webViews[tabId]?.apply {
            clearCache(true)
            clearHistory()
            clearFormData()
        }
    }

    fun destroy() {
        webViews.values.forEach { it.destroy() }
        webViews.clear()
    }
}
