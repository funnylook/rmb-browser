package com.rmb.browser

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rmb.browser.web.TabManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tabManager: TabManager,
    onBack: () -> Unit
) {
    var globalIncognito by remember { mutableStateOf(tabManager.isGlobalIncognito) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showClearIncognitoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Incognito mode
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("全局无痕模式", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("新标签页默认使用无痕模式", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = globalIncognito,
                        onCheckedChange = {
                            globalIncognito = it
                            tabManager.isGlobalIncognito = it
                        }
                    )
                }
            }

            // Developer mode info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, "开发者", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开发者模式", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("远程调试已启用", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1. 手机连接电脑 USB", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Text("2. Chrome 打开 chrome://inspect", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Text("3. 选择设备和页面进行调试", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            // Clear incognito data
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showClearIncognitoDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VisibilityOff, "清除无痕", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("清除无痕数据", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("关闭所有无痕标签并清除缓存", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // Clear all data
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showClearDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteForever, "清除全部", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("清除所有数据", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                        Text("关闭所有标签，清除所有 Cookie、缓存和历史", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("关于", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                    Text("RMB浏览器 v1.0.0", fontSize = 13.sp)
                    Text("• 多标签浏览，每个标签独立会话", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Text("• 无痕模式，不保存 Cookie 和历史", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Text("• 开发者模式，支持 USB 远程调试", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Text("• 轻量设计，极速启动", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    // Clear incognito dialog
    if (showClearIncognitoDialog) {
        AlertDialog(
            onDismissRequest = { showClearIncognitoDialog = false },
            title = { Text("清除无痕数据") },
            text = { Text("将关闭所有无痕标签并清除相关缓存，确认？") },
            confirmButton = {
                TextButton(onClick = {
                    tabManager.clearIncognitoData()
                    showClearIncognitoDialog = false
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showClearIncognitoDialog = false }) { Text("取消") }
            }
        )
    }

    // Clear all dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除所有数据") },
            text = { Text("将关闭所有标签并清除所有浏览数据，确认？") },
            confirmButton = {
                TextButton(onClick = {
                    tabManager.clearAllData()
                    showClearDialog = false
                    onBack()
                }) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}
