package com.rmb.browser.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DevToolsTab(val label: String, val icon: String) {
    CONSOLE("Console", "⌨"),
    NETWORK("Network", "🌐"),
    ELEMENTS("Elements", "🔍")
}

@Composable
fun DevToolsPanel(
    bridge: DevToolsBridge,
    onExecuteJs: (String) -> Unit,
    onStartInspect: () -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DevToolsTab.CONSOLE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .background(Color(0xFF1E1E1E))
    ) {
        // Tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DevToolsTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                TextButton(
                    onClick = { selectedTab = tab },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (selected) Color(0xFF4FC3F7) else Color(0xFF9E9E9E)
                    ),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        "${tab.icon} ${tab.label}",
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                bridge.clearConsole()
                bridge.clearNetwork()
            }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, "清除", tint = Color(0xFF9E9E9E), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "关闭", tint = Color(0xFF9E9E9E), modifier = Modifier.size(16.dp))
            }
        }

        // Divider
        Divider(color = Color(0xFF424242), thickness = 1.dp)

        // Content
        when (selectedTab) {
            DevToolsTab.CONSOLE -> ConsoleTab(bridge, onExecuteJs)
            DevToolsTab.NETWORK -> NetworkTab(bridge)
            DevToolsTab.ELEMENTS -> ElementsTab(bridge, onStartInspect)
        }
    }
}

@Composable
private fun ConsoleTab(bridge: DevToolsBridge, onExecuteJs: (String) -> Unit) {
    val logs by bridge.consoleLogs.collectAsState()
    var jsInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Auto scroll to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Console output
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(logs) { entry ->
                ConsoleEntryRow(entry)
            }
            if (logs.isEmpty()) {
                item {
                    Text(
                        "Console output will appear here...",
                        color = Color(0xFF616161),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // JS input
        Divider(color = Color(0xFF424242))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF252526))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("›", color = Color(0xFF4FC3F7), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = jsInput,
                onValueChange = { jsInput = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = Color(0xFFD4D4D4),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(Color(0xFF4FC3F7)),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    if (jsInput.isNotBlank()) {
                        onExecuteJs(jsInput)
                        jsInput = ""
                        focusManager.clearFocus()
                    }
                })
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    if (jsInput.isNotBlank()) {
                        onExecuteJs(jsInput)
                        jsInput = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.PlayArrow, "执行", tint = Color(0xFF4FC3F7), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ConsoleEntryRow(entry: ConsoleEntry) {
    val bgColor = when (entry.level) {
        ConsoleEntry.Level.ERROR -> Color(0xFF3B1515)
        ConsoleEntry.Level.WARN -> Color(0xFF3B3215)
        ConsoleEntry.Level.INFO -> Color(0xFF1A2A3B)
        else -> Color.Transparent
    }
    val textColor = when (entry.level) {
        ConsoleEntry.Level.ERROR -> Color(0xFFF48771)
        ConsoleEntry.Level.WARN -> Color(0xFFCCA700)
        ConsoleEntry.Level.INFO -> Color(0xFF4FC3F7)
        ConsoleEntry.Level.DEBUG -> Color(0xFF9E9E9E)
        else -> Color(0xFFD4D4D4)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            entry.timeStr(),
            color = Color(0xFF616161),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(75.dp)
        )
        Text(
            entry.level.icon,
            fontSize = 11.sp,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            entry.message,
            color = textColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 10,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NetworkTab(bridge: DevToolsBridge) {
    val requests by bridge.networkRequests.collectAsState()
    var selectedRequest by remember { mutableStateOf<NetworkEntry?>(null) }

    if (selectedRequest != null) {
        NetworkDetailScreen(selectedRequest!!) { selectedRequest = null }
        return
    }

    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No network requests yet", color = Color(0xFF616161), fontSize = 13.sp)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(requests.size) { index ->
            val entry = requests[requests.size - 1 - index]
            NetworkEntryRow(entry) { selectedRequest = entry }
        }
    }
}

@Composable
private fun NetworkEntryRow(entry: NetworkEntry, onClick: () -> Unit) {
    val statusColor = when {
        entry.status in 200..299 -> Color(0xFF4CAF50)
        entry.status in 300..399 -> Color(0xFFFFC107)
        entry.status >= 400 -> Color(0xFFF44336)
        entry.status == 0 -> Color(0xFF9E9E9E)
        else -> Color(0xFFD4D4D4)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            entry.method,
            color = Color(0xFF4FC3F7),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(40.dp)
        )
        Text(
            if (entry.status > 0) "${entry.status}" else "...",
            color = statusColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(32.dp)
        )
        Text(
            entry.url,
            color = Color(0xFFD4D4D4),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            if (entry.duration > 0) "${entry.duration}ms" else "",
            color = Color(0xFF9E9E9E),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun NetworkDetailScreen(entry: NetworkEntry, onBack: () -> Unit) {
    var detailTab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ArrowBack, "返回", tint = Color(0xFF9E9E9E), modifier = Modifier.size(16.dp))
            }
            Text(entry.method, color = Color(0xFF4FC3F7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text(
                entry.url,
                color = Color(0xFFD4D4D4),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        // Detail tabs
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF252526)).horizontalScroll(rememberScrollState())) {
            listOf("Headers", "Payload", "Response").forEachIndexed { i, label ->
                TextButton(onClick = { detailTab = i }) {
                    Text(label, fontSize = 11.sp, color = if (detailTab == i) Color(0xFF4FC3F7) else Color(0xFF9E9E9E))
                }
            }
        }
        Divider(color = Color(0xFF424242))
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            when (detailTab) {
                0 -> { // Headers
                    item { Text("General", color = Color(0xFF4FC3F7), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    item { Text("URL: ${entry.url}", color = Color(0xFFD4D4D4), fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                    item { Text("Method: ${entry.method}", color = Color(0xFFD4D4D4), fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                    item { Text("Status: ${entry.status} ${entry.statusText}", color = Color(0xFFD4D4D4), fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                    if (entry.responseHeaders.isNotEmpty()) {
                        item { Spacer(Modifier.height(8.dp)); Text("Response Headers", color = Color(0xFF4FC3F7), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        items(entry.responseHeaders.entries.toList()) { (k, v) ->
                            Text("$k: $v", color = Color(0xFFD4D4D4), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                1 -> { // Payload
                    item {
                        Text(
                            if (entry.requestBody.isNotEmpty()) entry.requestBody else "(no payload)",
                            color = Color(0xFFD4D4D4),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                2 -> { // Response
                    item {
                        Text(
                            if (entry.responseBody.isNotEmpty()) entry.responseBody else "(no response body)",
                            color = Color(0xFFD4D4D4),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ElementsTab(bridge: DevToolsBridge, onStartInspect: () -> Unit) {
    val elementInfo by bridge.elementInfo.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Inspect button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF252526))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onStartInspect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D47A1),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.TouchApp, "检查", modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Inspect Element", fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))
            if (elementInfo != null) {
                IconButton(
                    onClick = { bridge.clearElement() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Clear, "清除", tint = Color(0xFF9E9E9E), modifier = Modifier.size(16.dp))
                }
            }
        }

        Divider(color = Color(0xFF424242))

        val info = elementInfo
        if (info == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap 'Inspect Element' then tap on a page element", color = Color(0xFF616161), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Tag & selector
                item {
                    Text(
                        "<${info.tagName}>",
                        color = Color(0xFF4EC9B0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (info.selector.isNotEmpty()) {
                    item {
                        Text(info.selector, color = Color(0xFFD7BA7D), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // Attributes
                if (info.attributes.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)); Text("Attributes", color = Color(0xFF4FC3F7), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    items(info.attributes.entries.toList()) { (k, v) ->
                        Text("$k=\"$v\"", color = Color(0xFF9CDCFE), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // Styles
                if (info.styles.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)); Text("Computed Styles", color = Color(0xFF4FC3F7), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    items(info.styles.entries.toList()) { (k, v) ->
                        if (v.isNotEmpty() && v != "none" && v != "auto" && v != "normal") {
                            Text("$k: $v;", color = Color(0xFFD4D4D4), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // HTML
                if (info.html.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)); Text("HTML", color = Color(0xFF4FC3F7), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    item {
                        Text(
                            info.html,
                            color = Color(0xFFD4D4D4),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
