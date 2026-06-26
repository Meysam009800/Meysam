package javanepoya.ir.cloudpanel.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javanepoya.ir.cloudpanel.ui.theme.*
import javanepoya.ir.cloudpanel.viewmodel.CloudflareViewModel
import kotlinx.coroutines.launch

@Composable
fun ZoneManagerScreenView(
    viewModel: CloudflareViewModel,
    lang: String,
    onBack: () -> Unit
) {
    val sslMode by viewModel.zoneSslMode.collectAsState()
    val securityLevel by viewModel.zoneSecurityLevel.collectAsState()
    val firewallActive by viewModel.zoneFirewallActive.collectAsState()
    val wafActive by viewModel.zoneWafActive.collectAsState()
    val proxyDefault by viewModel.zoneProxyDefault.collectAsState()
    val cacheLevel by viewModel.zoneCacheLevel.collectAsState()

    var cachePurgeUrl by remember { mutableStateOf("") }
 
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
 
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepObsidian
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AppHeader(
                title = Translations.get("zone_manager", lang),
                lang = lang,
                onBack = onBack,
                onSearchClicked = {},
                onAddAccountClicked = {}
            )
 
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SSL Mode Selection Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, "", tint = CfOrange, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Translations.get("ssl", lang), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Off", "Flexible", "Full", "Strict").forEach { mode ->
                                    val isSelected = sslMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CfOrange else DeepObsidian)
                                            .border(1.dp, if (isSelected) Color.White else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.updateZoneSslMode(mode) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(mode, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
 
                // Security Level Selector
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, "", tint = CfOrange, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Translations.get("security", lang), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Under Attack", "High", "Medium", "Off").forEach { lvl ->
                                    val isSelected = securityLevel == lvl
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CfOrange else DeepObsidian)
                                            .border(1.dp, if (isSelected) Color.White else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.updateZoneSecurityLevel(lvl) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(lvl.split(" ").first(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
 
                // Firewall & WAF & Proxy Configs
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Block, "", tint = CfOrange, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(Translations.get("firewall", lang), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Switch(
                                    checked = firewallActive,
                                    onCheckedChange = { viewModel.toggleZoneFirewall(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CfOrange)
                                )
                            }
                            Divider(color = BorderColor)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, "", tint = CfOrange, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(Translations.get("waf", lang), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Switch(
                                    checked = wafActive,
                                    onCheckedChange = { viewModel.toggleZoneWaf(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CfOrange)
                                )
                            }
                            Divider(color = BorderColor)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Cloud, "", tint = CfOrange, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (lang == "fa") "پروکسی پیش‌فرض" else "Global Proxy Default", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Switch(
                                    checked = proxyDefault,
                                    onCheckedChange = { viewModel.toggleZoneProxy(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CfOrange)
                                )
                            }
                        }
                    }
                }
 
                // Cache Purge & Settings Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cached, "", tint = CfOrange, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Translations.get("cache", lang), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.purgeCache(all = true)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(if (lang == "fa") "حافظه موقت کاملاً پاک شد" else "Complete cache successfully purged")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CfOrange),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(Translations.get("purge_cache", lang), color = Color.White, fontSize = 11.sp)
                                }
 
                                Button(
                                    onClick = {
                                        if (cachePurgeUrl.isNotEmpty()) {
                                            viewModel.purgeCache(all = false, url = cachePurgeUrl)
                                            val msg = if (lang == "fa") "آدرس $cachePurgeUrl پاک شد" else "Purged URL: $cachePurgeUrl"
                                            cachePurgeUrl = ""
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepObsidian),
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(Translations.get("purge_url", lang), color = Color.White, fontSize = 11.sp)
                                }
                            }
 
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = cachePurgeUrl,
                                onValueChange = { cachePurgeUrl = it },
                                label = { Text("Enter URL to purge (e.g. style.css)", color = Color.White) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CfOrange,
                                    unfocusedBorderColor = BorderColor
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = BorderColor)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(if (lang == "fa") "سطح کشینگ" else "Caching Level", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Bypass", "Standard", "Aggressive").forEach { lvl ->
                                    val isSelected = cacheLevel == lvl
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CfOrange else DeepObsidian)
                                            .border(1.dp, if (isSelected) Color.White else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.updateZoneCacheLevel(lvl) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(lvl, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
