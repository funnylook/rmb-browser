package com.rmb.browser.devtools

import java.text.SimpleDateFormat
import java.util.*

/**
 * Data models for DevTools
 */

data class ConsoleEntry(
    val level: Level,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Level(val label: String, val icon: String) {
        LOG("Log", "📋"),
        INFO("Info", "ℹ️"),
        WARN("Warn", "⚠️"),
        ERROR("Error", "❌"),
        DEBUG("Debug", "🔍")
    }

    fun timeStr(): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    }
}

data class NetworkEntry(
    val method: String,
    val url: String,
    val status: Int = 0,
    val statusText: String = "",
    val requestHeaders: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String> = emptyMap(),
    val requestBody: String = "",
    val responseBody: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0,
    val duration: Long = 0,
    val type: RequestType = RequestType.OTHER
) {
    enum class RequestType(val label: String) {
        XHR("XHR"), FETCH("Fetch"), SCRIPT("Script"),
        CSS("CSS"), IMG("Image"), FONT("Font"),
        DOCUMENT("Document"), WS("WebSocket"), OTHER("Other")
    }
}

data class ElementInfo(
    val tagName: String = "",
    val id: String = "",
    val className: String = "",
    val text: String = "",
    val attributes: Map<String, String> = emptyMap(),
    val styles: Map<String, String> = emptyMap(),
    val computedStyles: Map<String, String> = emptyMap(),
    val selector: String = "",
    val html: String = "",
    val children: List<ElementInfo> = emptyList()
)
