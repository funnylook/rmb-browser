package com.rmb.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rmb.browser.web.BrowserTab
import com.rmb.browser.web.TabManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabListScreen(
    tabManager: TabManager,
    onTabSelected: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("标签管理 (${tabManager.tabCount()})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // New normal tab
                    IconButton(onClick = { onNewTab(false) }) {
                        Icon(Icons.Default.Add, "新建标签")
                    }
                    // New incognito tab
                    IconButton(onClick = { onNewTab(true) }) {
                        Icon(Icons.Default.VisibilityOff, "无痕标签")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabManager.tabs) { tab ->
                TabItem(
                    tab = tab,
                    isActive = tab.id == tabManager.activeTabId,
                    onClick = { onTabSelected(tab.id) },
                    onClose = { onCloseTab(tab.id) }
                )
            }

            // Add new tab buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNewTab(false) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("普通标签")
                    }
                    OutlinedButton(
                        onClick = { onNewTab(true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VisibilityOff, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("无痕标签")
                    }
                }
            }
        }
    }
}

@Composable
fun TabItem(
    tab: BrowserTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (tab.isIncognito) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (tab.isIncognito) {
                    Icon(
                        Icons.Default.VisibilityOff,
                        "无痕",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Icon(
                        Icons.Default.Public,
                        "网页",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title and URL
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tab.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    tab.url.ifEmpty { "新标签页" },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    "关闭",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
