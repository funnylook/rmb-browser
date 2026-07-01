package com.rmb.browser.devtools

import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * JavaScript interface injected into WebView.
 * Receives data from injected JS and exposes it as StateFlows.
 */
class DevToolsBridge {

    private val _consoleLogs = MutableStateFlow<List<ConsoleEntry>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleEntry>> = _consoleLogs.asStateFlow()

    private val _networkRequests = MutableStateFlow<List<NetworkEntry>>(emptyList())
    val networkRequests: StateFlow<List<NetworkEntry>> = _networkRequests.asStateFlow()

    private val _elementInfo = MutableStateFlow<ElementInfo?>(null)
    val elementInfo: StateFlow<ElementInfo?> = _elementInfo.asStateFlow()

    private val _evalResult = MutableStateFlow("")
    val evalResult: StateFlow<String> = _evalResult.asStateFlow()

    private val maxConsoleEntries = 500
    private val maxNetworkEntries = 200

    @JavascriptInterface
    fun onConsole(level: String, message: String) {
        val entryLevel = when (level.uppercase()) {
            "INFO" -> ConsoleEntry.Level.INFO
            "WARN" -> ConsoleEntry.Level.WARN
            "ERROR" -> ConsoleEntry.Level.ERROR
            "DEBUG" -> ConsoleEntry.Level.DEBUG
            else -> ConsoleEntry.Level.LOG
        }
        val entry = ConsoleEntry(entryLevel, message)
        val current = _consoleLogs.value.toMutableList()
        current.add(entry)
        if (current.size > maxConsoleEntries) current.removeAt(0)
        _consoleLogs.value = current
    }

    @JavascriptInterface
    fun onNetworkStart(id: String, method: String, url: String, body: String, type: String) {
        val requestType = when (type.uppercase()) {
            "XHR" -> NetworkEntry.RequestType.XHR
            "FETCH" -> NetworkEntry.RequestType.FETCH
            else -> NetworkEntry.RequestType.OTHER
        }
        val entry = NetworkEntry(
            method = method,
            url = url,
            requestBody = body,
            type = requestType,
            startTime = System.currentTimeMillis()
        )
        val current = _networkRequests.value.toMutableList()
        synchronized(pendingRequests) { pendingRequests[id] = entry }
        current.add(entry)
        if (current.size > maxNetworkEntries) current.removeAt(0)
        _networkRequests.value = current
    }

    @JavascriptInterface
    fun onNetworkEnd(id: String, status: Int, statusText: String, headers: String, body: String) {
        val entry = synchronized(pendingRequests) { pendingRequests.remove(id) } ?: return
        val endTime = System.currentTimeMillis()
        val duration = endTime - entry.startTime

        val responseHeaders = try {
            val obj = JSONObject(headers)
            val map = mutableMapOf<String, String>()
            obj.keys().forEach { map[it] = obj.getString(it) }
            map
        } catch (e: Exception) { emptyMap() }

        val updated = entry.copy(
            status = status,
            statusText = statusText,
            responseHeaders = responseHeaders,
            responseBody = body,
            endTime = endTime,
            duration = duration
        )

        val current = _networkRequests.value.toMutableList()
        val index = current.indexOfFirst { it.url == entry.url && it.startTime == entry.startTime }
        if (index >= 0) current[index] = updated
        _networkRequests.value = current
    }

    @JavascriptInterface
    fun onElementInspect(info: String, tree: String) {
        try {
            val obj = JSONObject(info)
            val attrs = mutableMapOf<String, String>()
            obj.optJSONObject("attrs")?.let { a ->
                a.keys().forEach { attrs[it] = a.getString(it) }
            }
            val styles = mutableMapOf<String, String>()
            obj.optJSONObject("styles")?.let { s ->
                s.keys().forEach { styles[it] = s.getString(it) }
            }

            _elementInfo.value = ElementInfo(
                tagName = obj.optString("tag", ""),
                id = obj.optString("id", ""),
                className = obj.optString("class", ""),
                text = obj.optString("text", ""),
                attributes = attrs,
                styles = styles,
                selector = obj.optString("selector", ""),
                html = obj.optString("html", "")
            )
        } catch (e: Exception) {
            _elementInfo.value = ElementInfo(html = info)
        }
    }

    @JavascriptInterface
    fun onEvalResult(result: String) {
        _evalResult.value = result
    }

    fun clearConsole() {
        _consoleLogs.value = emptyList()
    }

    fun clearNetwork() {
        _networkRequests.value = emptyList()
        synchronized(pendingRequests) { pendingRequests.clear() }
    }

    fun clearElement() {
        _elementInfo.value = null
    }

    companion object {
        private val pendingRequests = mutableMapOf<String, NetworkEntry>()
    }
}
