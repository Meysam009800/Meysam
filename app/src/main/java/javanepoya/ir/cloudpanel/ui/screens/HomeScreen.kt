package javanepoya.ir.cloudpanel.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javanepoya.ir.cloudpanel.data.AccountEntity
import javanepoya.ir.cloudpanel.ui.theme.*
import javanepoya.ir.cloudpanel.viewmodel.CloudflareViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreenView(
    viewModel: CloudflareViewModel,
    lang: String,
    onNavigate: (String) -> Unit
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()
    val workspace by viewModel.selectedWorkspace.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var accountToRename by remember { mutableStateOf<AccountEntity?>(null) }
    var showSearchOverlay by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(syncError) {
        syncError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearSyncError()
        }
    }

    // Filter accounts based on workspace selector
    val filteredAccounts = accounts.filter {
        (workspace == "All" || it.workspace == workspace) &&
        (searchQuery.isEmpty() || it.alias.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true))
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Top Header
            AppHeader(
                title = Translations.get("app_name", lang),
                lang = lang,
                onSearchClicked = { showSearchOverlay = true },
                onAddAccountClicked = { showAddDialog = true }
            )

            // Dynamic Workspace Filters
            WorkspaceSelector(
                selected = workspace,
                lang = lang,
                onSelect = { viewModel.selectWorkspace(it) }
            )

            // Dynamic Scrollable Dashboard Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Currently selected Account Detailed Dashboard Card
                item {
                    currentAccount?.let { account ->
                        ActiveAccountDashboardCard(
                            account = account,
                            lang = lang,
                            isSyncing = isSyncing,
                            onRenameClicked = {
                                accountToRename = account
                                showRenameDialog = true
                            },
                            onRemoveClicked = { viewModel.removeAccount(account) },
                            onModeToggle = { newMode ->
                                viewModel.switchAccountMode(newMode)
                            },
                            onSyncClicked = { viewModel.syncCurrentAccountData() }
                        )
                    } ?: run {
                        // Empty Account Placeholder Card
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CloudQueue, "", tint = CfOrange, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (lang == "fa") "هیچ حسابی ثبت نشده است" else "No Accounts Configured",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (lang == "fa") "برای مدیریت دامنه و ورکرها، یک حساب جدید اضافه کنید" else "Add a Cloudflare account to start switching",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
                                ) {
                                    Icon(Icons.Default.Add, "")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(Translations.get("add_account", lang))
                                }
                            }
                        }
                    }
                }

                // Isolated Account Multi-Container Fast Switching Slider
                if (filteredAccounts.size > 1) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Translations.get("fast_switching", lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "(${filteredAccounts.size})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CfOrange
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                filteredAccounts.forEach { account ->
                                    val isActive = currentAccount?.id == account.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isActive) CfOrange else DarkSlateCard)
                                            .border(1.dp, if (isActive) Color.White else BorderColor, RoundedCornerShape(12.dp))
                                            .clickable { viewModel.selectAccount(account) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = account.alias,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = account.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isActive) Color.White.copy(0.8f) else TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Navigation System
                currentAccount?.let { account ->
                    item {
                        QuickNavigationGrid(
                            mode = account.mode,
                            lang = lang,
                            onNavigate = onNavigate
                        )
                    }
                }

                // System Isolation Warning Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = CfOrange.copy(0.08f)),
                        border = BorderStroke(1.dp, CfOrange.copy(0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Security, "", tint = CfOrange, modifier = Modifier.size(24.dp))
                            Text(
                                text = Translations.get("isolated_warning", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // Add Account Dialog Form overlay
        if (showAddDialog) {
            AddAccountDialog(
                lang = lang,
                onDismiss = { showAddDialog = false },
                onAdd = { alias, email, token, ws, mode ->
                    viewModel.createAccount(alias, email, token, ws, mode)
                    showAddDialog = false
                }
            )
        }

        // Rename Account Dialog overlay
        if (showRenameDialog && accountToRename != null) {
            RenameAccountDialog(
                account = accountToRename!!,
                lang = lang,
                onDismiss = {
                    showRenameDialog = false
                    accountToRename = null
                },
                onRename = { alias ->
                    viewModel.renameAccount(accountToRename!!.id, alias)
                    showRenameDialog = false
                    accountToRename = null
                }
            )
        }

        // Dynamic Global Search Overlay Sheet
        if (showSearchOverlay) {
            GlobalSearchOverlay(
                viewModel = viewModel,
                lang = lang,
                onDismiss = { showSearchOverlay = false },
                onNavigate = onNavigate
            )
        }
    }
}

@Composable
fun ActiveAccountDashboardCard(
    account: AccountEntity,
    lang: String,
    isSyncing: Boolean,
    onRenameClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
    onModeToggle: (String) -> Unit,
    onSyncClicked: () -> Unit
) {
    val dateString = remember(account.lastActiveTime) {
        val sdf = SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale.getDefault())
        sdf.format(Date(account.lastActiveTime))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(colors = listOf(Color(0xFF0F172A), Color(0xFF161B22))))
            .border(1.dp, BorderColor, RoundedCornerShape(28.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Card Title & Mode Toggle Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isConnected = account.connectionStatus == "Connected"
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pulseScale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pulseAlpha"
                    )
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(14.dp)
                    ) {
                        if (isConnected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                        alpha = pulseAlpha
                                    }
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) AccentGreen else AccentRed)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = account.alias,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Simple vs Professional toggle switch
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DeepObsidian)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSimple = account.mode == "Simple"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSimple) CfOrange else Color.Transparent)
                            .clickable { onModeToggle("Simple") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = Translations.get("simple_mode", lang).split(" ").first(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSimple) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!isSimple) CfOrange else Color.Transparent)
                            .clickable { onModeToggle("Professional") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = Translations.get("professional_mode", lang).split(" ").first(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!isSimple) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Credentials detail rows
            Text(
                text = "${Translations.get("email", lang)}: ${account.email}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${Translations.get("workspace", lang)}: ${
                    when (account.workspace) {
                        "Personal" -> Translations.get("personal", lang)
                        "Client" -> Translations.get("client", lang)
                        else -> Translations.get("company", lang)
                    }
                }",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${Translations.get("last_active", lang)}: $dateString",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = BorderColor)
            Spacer(modifier = Modifier.height(16.dp))

            // Badges showing zone and worker count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = account.workerCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = CfOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Translations.get("worker_count", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(BorderColor))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = account.zoneCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Translations.get("zone_count", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = BorderColor)
            Spacer(modifier = Modifier.height(12.dp))

            // Action row (Rename / Remove account / Sync API)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AccentBlue,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lang == "fa") "همگام‌سازی..." else "Syncing...",
                        color = AccentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    TextButton(onClick = onSyncClicked) {
                        Icon(Icons.Default.Refresh, "", modifier = Modifier.size(16.dp), tint = AccentBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (lang == "fa") "همگام‌سازی" else "Sync API", color = AccentBlue)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(onClick = onRenameClicked) {
                    Icon(Icons.Default.Edit, "", modifier = Modifier.size(16.dp), tint = CfOrange)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Translations.get("rename_account", lang), color = CfOrange)
                }
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(onClick = onRemoveClicked) {
                    Icon(Icons.Default.Delete, "", modifier = Modifier.size(16.dp), tint = AccentRed)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Translations.get("remove_account", lang), color = AccentRed)
                }
            }
        }
    }
}

@Composable
fun QuickNavigationGrid(
    mode: String,
    lang: String,
    onNavigate: (String) -> Unit
) {
    // Determine menus based on simple vs professional mode
    val items = if (mode == "Simple") {
        listOf(
            Triple("workers", Translations.get("workers", lang), Icons.Default.Code),
            Triple("dns", Translations.get("dns", lang), Icons.Default.Dns),
            Triple("settings", Translations.get("pin_lock", lang), Icons.Default.Lock)
        )
    } else {
        listOf(
            Triple("workers", Translations.get("workers", lang), Icons.Default.Code),
            Triple("dns", Translations.get("dns", lang), Icons.Default.Dns),
            Triple("zone_manager", Translations.get("zone_manager", lang), Icons.Default.Language),
            Triple("r2", Translations.get("r2", lang), Icons.Default.Storage),
            Triple("kv", Translations.get("kv", lang), Icons.Default.List),
            Triple("d1", Translations.get("d1", lang), Icons.Default.Storage),
            Triple("analytics", Translations.get("analytics", lang), Icons.Default.BarChart),
            Triple("tunnels", Translations.get("tunnels", lang), Icons.Default.CompareArrows),
            Triple("notifications", Translations.get("notifications", lang), Icons.Default.Notifications),
            Triple("settings", Translations.get("advanced_settings", lang), Icons.Default.Settings)
        )
    }

    Column {
        Text(
            text = Translations.get("quick_actions", lang),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Build elegant grid rows of cards
        val chunked = items.chunked(2)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    val itemColor = remember(item.first) {
                        when (item.first) {
                            "workers" -> Color(0xFF3B82F6) // blue
                            "dns" -> Color(0xFF6366F1) // indigo
                            "zone_manager" -> Color(0xFF10B981) // emerald
                            "r2" -> Color(0xFFF59E0B) // amber
                            "kv" -> Color(0xFFEC4899) // pink
                            "d1" -> Color(0xFF8B5CF6) // purple
                            "analytics" -> Color(0xFF06B6D4) // cyan
                            "tunnels" -> Color(0xFF14B8A6) // teal
                            "notifications" -> Color(0xFFEF4444) // red
                            else -> Color(0xFF94A3B8) // slate
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(DarkSlateCard)
                            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
                            .clickable { onNavigate(item.first) }
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(itemColor.copy(alpha = 0.12f))
                                    .border(1.dp, itemColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.third,
                                    contentDescription = item.second,
                                    tint = itemColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = item.second,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    lang: String,
    onDismiss: () -> Unit,
    onAdd: (alias: String, email: String, token: String, workspace: String, mode: String) -> Unit
) {
    var alias by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var workspace by remember { mutableStateOf("Personal") } // "Personal", "Client", "Company"
    var mode by remember { mutableStateOf("Simple") } // "Simple", "Professional"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                Translations.get("add_account", lang),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        containerColor = DarkSlateCard,
        confirmButton = {
            Button(
                onClick = {
                    if (alias.isNotEmpty() && email.isNotEmpty() && token.isNotEmpty()) {
                        onAdd(alias, email, token, workspace, mode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
            ) {
                Text(Translations.get("add_account", lang), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Translations.get("cancel", lang), color = TextSecondary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text(Translations.get("account_alias", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CfOrange,
                        unfocusedBorderColor = BorderColor
                    )
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(Translations.get("email", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CfOrange,
                        unfocusedBorderColor = BorderColor
                    )
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(Translations.get("api_token", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CfOrange,
                        unfocusedBorderColor = BorderColor
                    )
                )

                // Workspace Choice Selector
                Text(
                    text = Translations.get("workspace", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Personal", "Client", "Company").forEach { ws ->
                        val isSelected = workspace == ws
                        val label = when (ws) {
                            "Personal" -> Translations.get("personal", lang).split(" ").first()
                            "Client" -> Translations.get("client", lang).split(" ").first()
                            else -> Translations.get("company", lang).split(" ").first()
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CfOrange else DeepObsidian)
                                .border(1.dp, if (isSelected) Color.White else BorderColor, RoundedCornerShape(8.dp))
                                .clickable { workspace = ws }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Initial mode choice
                Text(
                    text = Translations.get("mode", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Simple", "Professional").forEach { md ->
                        val isSelected = mode == md
                        val label = when (md) {
                            "Simple" -> Translations.get("simple_mode", lang).split(" ").first()
                            else -> Translations.get("professional_mode", lang).split(" ").first()
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CfOrange else DeepObsidian)
                                .border(1.dp, if (isSelected) Color.White else BorderColor, RoundedCornerShape(8.dp))
                                .clickable { mode = md }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun RenameAccountDialog(
    account: AccountEntity,
    lang: String,
    onDismiss: () -> Unit,
    onRename: (alias: String) -> Unit
) {
    var alias by remember { mutableStateOf(account.alias) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Translations.get("rename_account", lang), color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = DarkSlateCard,
        confirmButton = {
            Button(
                onClick = { if (alias.isNotEmpty()) onRename(alias) },
                colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
            ) {
                Text(Translations.get("save", lang), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Translations.get("cancel", lang), color = TextSecondary)
            }
        },
        text = {
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text(Translations.get("account_alias", lang)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CfOrange,
                    unfocusedBorderColor = BorderColor
                )
            )
        }
    )
}

// Global dynamic search popup modal
@Composable
fun GlobalSearchOverlay(
    viewModel: CloudflareViewModel,
    lang: String,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val workers by viewModel.currentWorkers.collectAsState()
    val dnsRecords by viewModel.currentDnsRecords.collectAsState()
    val r2Buckets by viewModel.currentR2Buckets.collectAsState()
    val d1Databases by viewModel.currentD1Databases.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(Translations.get("search_hint", lang), color = TextSecondary, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, "", tint = CfOrange) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CfOrange,
                    unfocusedBorderColor = BorderColor
                )
            )
        },
        containerColor = DarkSlateCard,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Translations.get("cancel", lang), color = CfOrange)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (query.isNotEmpty()) {
                    Text(
                        text = Translations.get("search_results", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // 1. Search Workers
                    val matchedWorkers = workers.filter { it.name.contains(query, ignoreCase = true) }
                    matchedWorkers.forEach { worker ->
                        SearchOverlayItem(
                            title = worker.name,
                            category = Translations.get("workers", lang),
                            icon = Icons.Default.Code,
                            onClick = {
                                onNavigate("workers")
                                onDismiss()
                            }
                        )
                    }

                    // 2. Search DNS
                    val matchedDns = dnsRecords.filter { it.name.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true) }
                    matchedDns.forEach { dns ->
                        SearchOverlayItem(
                            title = "${dns.type} ${dns.name}",
                            category = Translations.get("dns_manager", lang),
                            icon = Icons.Default.Dns,
                            onClick = {
                                onNavigate("dns")
                                onDismiss()
                            }
                        )
                    }

                    // 3. Search R2 Buckets
                    val matchedR2 = r2Buckets.filter { it.name.contains(query, ignoreCase = true) }
                    matchedR2.forEach { bucket ->
                        SearchOverlayItem(
                            title = bucket.name,
                            category = Translations.get("r2", lang),
                            icon = Icons.Default.Storage,
                            onClick = {
                                onNavigate("r2")
                                onDismiss()
                            }
                        )
                    }

                    // 4. Search Databases
                    val matchedD1 = d1Databases.filter { it.name.contains(query, ignoreCase = true) }
                    matchedD1.forEach { db ->
                        SearchOverlayItem(
                            title = db.name,
                            category = Translations.get("d1", lang),
                            icon = Icons.Default.Storage,
                            onClick = {
                                onNavigate("d1")
                                onDismiss()
                            }
                        )
                    }

                    if (matchedWorkers.isEmpty() && matchedDns.isEmpty() && matchedR2.isEmpty() && matchedD1.isEmpty()) {
                        Text(
                            text = if (lang == "fa") "هیچ موردی یافت نشد" else "No match found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = if (lang == "fa") "برای جستجوی سراسری کلمه‌ای تایپ کنید" else "Type to search globally across accounts & resources",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    )
}

@Composable
fun SearchOverlayItem(
    title: String,
    category: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DeepObsidian)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, "", tint = CfOrange, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(category, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Default.ChevronRight, "", tint = TextTertiary, modifier = Modifier.size(16.dp))
    }
}
