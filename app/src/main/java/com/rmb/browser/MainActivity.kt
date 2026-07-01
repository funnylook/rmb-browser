package com.rmb.browser

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.rmb.browser.ui.theme.RmbBrowserTheme
import com.rmb.browser.web.BrowserTab
import com.rmb.browser.web.TabManager
import com.rmb.browser.web.WebViewFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RmbBrowserTheme {
                BrowserApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserApp() {
    val context = LocalContext.current
    var tabManager by remember { mutableStateOf(TabManager(context)) }
    var showTabList by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var addressBarText by remember { mutableStateOf("") }
    var isEditingUrl by remember { mutableStateOf(false) }

    // Create first tab if empty
    LaunchedEffect(Unit) {
        if (tabManager.tabCount() == 0) {
            tabManager.createTab("https://www.baidu.com")
        }
    }

    val activeTab = tabManager.getActiveTab()

    // Update address bar when tab changes
    LaunchedEffect(activeTab?.id, activeTab?.url) {
        if (!isEditingUrl && activeTab != null) {
            addressBarText = activeTab.url
        }
    }

    if (showSettings) {
        SettingsScreen(
            tabManager = tabManager,
            onBack = { showSettings = false }
        )
        return
    }

    if (showTabList) {
        TabListScreen(
            tabManager = tabManager,
            onTabSelected = { tabId ->
                tabManager.switchTab(tabId)
                showTabList = false
            },
            onCloseTab = { tabId ->
                tabManager.closeTab(tabId)
                if (tabManager.tabCount() == 0) {
                    tabManager.createTab("https://www.baidu.com")
                }
            },
            onNewTab = { incognito ->
                tabManager.createTab(incognito = incognito)
                showTabList = false
            },
            onBack = { showTabList = false }
        )
        return
    }

    Scaffold(
        topBar = {
            // Address bar
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    // Tab indicator
                    if (tabManager.tabCount() > 0) {
                        LinearProgressIndicator(
                            progress = { (activeTab?.progress ?: 0) / 100f },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Tab count button
                        BadgedBox(
                            badge = {
                                if (tabManager.tabCount() > 1) {
                                    Badge { Text("${tabManager.tabCount()}") }
                                }
                            }
                        ) {
                            IconButton(onClick = { showTabList = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ViewWeek, "标签", modifier = Modifier.size(20.dp))
                            }
                        }

                        // Incognito indicator
                        if (activeTab?.isIncognito == true) {
                            Icon(
                                Icons.Default.VisibilityOff,
                                "无痕",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Address bar
                        OutlinedTextField(
                            value = addressBarText,
                            onValueChange = { addressBarText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("输入网址或搜索", fontSize = 14.sp) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    val url = formatUrl(addressBarText)
                                    tabManager.getWebView(activeTab?.id ?: "")?.loadUrl(url)
                                    isEditingUrl = false
                                }
                            ),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also {
                                LaunchedEffect(it) {
                                    it.interactions.collect { interaction ->
                                        if (interaction is androidx.compose.foundation.interaction.PressInteraction.Focus) {
                                            isEditingUrl = true
                                        }
                                    }
                                }
                            }
                        )

                        // Refresh / Stop
                        IconButton(onClick = {
                            if (activeTab?.isLoading == true) {
                                tabManager.getWebView(activeTab.id)?.stopLoading()
                            } else {
                                tabManager.getWebView(activeTab?.id ?: "")?.reload()
                            }
                        }, modifier = Modifier.size(36.dp)) {
                            if (activeTab?.isLoading == true) {
                                Icon(Icons.Default.Close, "停止", modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.Refresh, "刷新", modifier = Modifier.size(20.dp))
                            }
                        }

                        // Menu
                        IconButton(onClick = { showSettings = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.MoreVert, "菜单", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Bottom toolbar
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        tabManager.getWebView(activeTab?.id ?: "")?.goBack()
                    }, enabled = tabManager.getWebView(activeTab?.id ?: "")?.canGoBack() == true) {
                        Icon(Icons.Default.ArrowBack, "后退")
                    }
                    IconButton(onClick = {
                        tabManager.getWebView(activeTab?.id ?: "")?.goForward()
                    }, enabled = tabManager.getWebView(activeTab?.id ?: "")?.canGoForward() == true) {
                        Icon(Icons.Default.ArrowForward, "前进")
                    }
                    IconButton(onClick = {
                        tabManager.createTab()
                    }) {
                        Icon(Icons.Default.Add, "新标签")
                    }
                    IconButton(onClick = {
                        tabManager.createTab(incognito = true)
                    }) {
                        Icon(Icons.Default.VisibilityOff, "无痕")
                    }
                    IconButton(onClick = { showTabList = true }) {
                        Icon(Icons.Default.GridView, "标签管理")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (activeTab != null) {
                TabWebView(
                    tab = activeTab,
                    tabManager = tabManager
                )
            } else {
                // Empty state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌐", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("RMB浏览器", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("点击下方 + 新建标签页", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun TabWebView(tab: BrowserTab, tabManager: TabManager) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            val existingWebView = tabManager.getWebView(tab.id)
            if (existingWebView != null) {
                // Reparent if needed
                (existingWebView.parent as? ViewGroup)?.removeView(existingWebView)
                existingWebView
            } else {
                val webView = WebViewFactory.createWebView(
                    context = ctx,
                    tabId = tab.id,
                    isIncognito = tab.isIncognito,
                    onPageStarted = { url ->
                        tabManager.updateTab(tab.id) { it.copy(url = url, isLoading = true) }
                    },
                    onPageFinished = { url ->
                        tabManager.updateTab(tab.id) { it.copy(url = url, isLoading = false, progress = 100) }
                    },
                    onProgressChanged = { progress ->
                        tabManager.updateTab(tab.id) { it.copy(progress = progress) }
                    },
                    onTitleReceived = { title ->
                        tabManager.updateTab(tab.id) { it.copy(title = title) }
                    },
                    onFaviconReceived = { /* ignored */ },
                    onReceivedError = { /* ignored */ }
                )
                tabManager.setWebView(tab.id, webView)

                if (tab.url.isNotEmpty() && tab.url != "about:blank") {
                    webView.loadUrl(tab.url)
                }

                webView
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun formatUrl(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
        else -> "https://www.baidu.com/s?wd=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
    }
}
