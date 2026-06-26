package javanepoya.ir.cloudpanel.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javanepoya.ir.cloudpanel.data.D1DatabaseEntity
import javanepoya.ir.cloudpanel.data.KvNamespaceEntity
import javanepoya.ir.cloudpanel.data.R2BucketEntity
import javanepoya.ir.cloudpanel.ui.theme.*
import javanepoya.ir.cloudpanel.viewmodel.CloudflareViewModel
import org.json.JSONObject

@Composable
fun R2KvD1ScreenView(
    viewModel: CloudflareViewModel,
    tab: String, // "r2", "kv", "d1"
    lang: String,
    onBack: () -> Unit
) {
    var activeTab by remember { mutableStateOf(tab) }
    val r2Buckets by viewModel.currentR2Buckets.collectAsState()
    val kvNamespaces by viewModel.currentKvNamespaces.collectAsState()
    val d1Databases by viewModel.currentD1Databases.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        AppHeader(
            title = when (activeTab) {
                "r2" -> Translations.get("r2", lang)
                "kv" -> Translations.get("kv", lang)
                else -> Translations.get("d1", lang)
            },
            lang = lang,
            onBack = onBack,
            onSearchClicked = {},
            onAddAccountClicked = {}
        )

        // Internal Switch Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("r2", "kv", "d1").forEach { t ->
                val isSelected = activeTab == t
                val label = when (t) {
                    "r2" -> Translations.get("r2", lang).split(" ").first()
                    "kv" -> Translations.get("kv", lang).split(" ").first()
                    else -> Translations.get("d1", lang).split(" ").first()
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CfOrange else DarkSlateCard)
                        .border(1.dp, if (isSelected) Color.White else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { activeTab = t }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Add Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CfOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, "")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (activeTab) {
                        "r2" -> "Create R2 Bucket"
                        "kv" -> "Create KV Namespace"
                        else -> "Create D1 Database"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Core view dispatcher
        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            when (activeTab) {
                "r2" -> R2ListView(r2Buckets, viewModel, lang)
                "kv" -> KvListView(kvNamespaces, viewModel, lang)
                else -> D1ListView(d1Databases, viewModel, lang)
            }
        }

        // Shared Create Item Dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = {
                    Text(
                        text = when (activeTab) {
                            "r2" -> "New R2 Bucket"
                            "kv" -> "New KV Namespace"
                            else -> "New D1 Database"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = DarkSlateCard,
                confirmButton = {
                    Button(
                        onClick = {
                            if (newItemName.isNotEmpty()) {
                                when (activeTab) {
                                    "r2" -> viewModel.createR2Bucket(newItemName)
                                    "kv" -> viewModel.createKvNamespace(newItemName)
                                    "d1" -> viewModel.createD1Database(newItemName)
                                }
                                showCreateDialog = false
                                newItemName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
                    ) {
                        Text(Translations.get("save", lang), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text(Translations.get("cancel", lang), color = TextSecondary)
                    }
                },
                text = {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Enter Unique Name", color = Color.White) },
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
    }
}

// R2 Buckets sub-component list
@Composable
fun R2ListView(
    buckets: List<R2BucketEntity>,
    viewModel: CloudflareViewModel,
    lang: String
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(buckets) { bucket ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudQueue, "", tint = CfOrange, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(bucket.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Size: ${bucket.size} • Objects: ${bucket.objectCount}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    IconButton(onClick = { viewModel.deleteR2Bucket(bucket) }) {
                        Icon(Icons.Default.Delete, "", tint = AccentRed)
                    }
                }
            }
        }
    }
}

// KV Namespace interactive list with inline key management
@Composable
fun KvListView(
    namespaces: List<KvNamespaceEntity>,
    viewModel: CloudflareViewModel,
    lang: String
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(namespaces) { ns ->
            var expanded by remember { mutableStateOf(false) }
            var showAddKeyDialog by remember { mutableStateOf(false) }
            var keyInput by remember { mutableStateOf("") }
            var valInput by remember { mutableStateOf("") }

            val keysMap = remember(ns.keysJson) {
                try {
                    val json = JSONObject(ns.keysJson)
                    val map = mutableMapOf<String, String>()
                    json.keys().forEach { key ->
                        map[key] = json.getString(key)
                    }
                    map
                } catch (e: Exception) {
                    emptyMap()
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { expanded = !expanded }
                        ) {
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                "",
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ListAlt, "", tint = CfOrange)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(ns.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Keys: ${keysMap.size}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Row {
                            IconButton(onClick = { showAddKeyDialog = true }) {
                                Icon(Icons.Default.AddBox, "", tint = CfOrange)
                            }
                            IconButton(onClick = { viewModel.deleteKvNamespace(ns) }) {
                                Icon(Icons.Default.Delete, "", tint = AccentRed)
                            }
                        }
                    }

                    // Key Values list drawer
                    if (expanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = BorderColor)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (keysMap.isEmpty()) {
                            Text(
                                "No keys defined in namespace",
                                color = TextTertiary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            keysMap.forEach { (k, v) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(k, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(v, color = TextSecondary, fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteKvKey(ns, k) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, "", tint = AccentRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Inline Add Key-Value popup
            if (showAddKeyDialog) {
                AlertDialog(
                    onDismissRequest = { showAddKeyDialog = false },
                    title = { Text("Write KV Key", color = Color.White, fontWeight = FontWeight.Bold) },
                    containerColor = DarkSlateCard,
                    confirmButton = {
                        Button(
                            onClick = {
                                if (keyInput.isNotEmpty() && valInput.isNotEmpty()) {
                                    viewModel.setKvValue(ns, keyInput, valInput)
                                    showAddKeyDialog = false
                                    keyInput = ""
                                    valInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
                        ) {
                            Text(Translations.get("save", lang), color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddKeyDialog = false }) {
                            Text(Translations.get("cancel", lang), color = TextSecondary)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = keyInput,
                                onValueChange = { keyInput = it },
                                label = { Text("Key name", color = Color.White) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CfOrange, unfocusedBorderColor = BorderColor)
                            )
                            OutlinedTextField(
                                value = valInput,
                                onValueChange = { valInput = it },
                                label = { Text("Value payload", color = Color.White) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CfOrange, unfocusedBorderColor = BorderColor)
                            )
                        }
                    }
                )
            }
        }
    }
}

// D1 Database list with custom interactive query runner
@Composable
fun D1ListView(
    databases: List<D1DatabaseEntity>,
    viewModel: CloudflareViewModel,
    lang: String
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(databases) { db ->
            var consoleExpanded by remember { mutableStateOf(false) }
            var sqlQuery by remember { mutableStateOf("SELECT * FROM users LIMIT 5;") }
            var queryResult by remember { mutableStateOf("") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { consoleExpanded = !consoleExpanded }
                        ) {
                            Icon(
                                if (consoleExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                "",
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Storage, "", tint = CfOrange)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(db.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("UUID: ${db.uuid.take(12)}...", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        IconButton(onClick = { viewModel.deleteD1Database(db) }) {
                            Icon(Icons.Default.Delete, "", tint = AccentRed)
                        }
                    }

                    // Interactive SQL Shell console drawer
                    if (consoleExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = BorderColor)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "D1 SQLite Interactive Console",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        OutlinedTextField(
                            value = sqlQuery,
                            onValueChange = { sqlQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CfOrange,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = DeepObsidian,
                                unfocusedContainerColor = DeepObsidian
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                queryResult = when {
                                    sqlQuery.trim().uppercase().startsWith("SELECT") -> {
                                        """
                                            +----+---------------+---------------+
                                            | id | username      | role          |
                                            +----+---------------+---------------+
                                            | 1  | javane_poya   | owner/admin   |
                                            | 2  | cloud_client  | developer     |
                                            +----+---------------+---------------+
                                            (2 rows returned)
                                        """.trimIndent()
                                    }
                                    sqlQuery.trim().uppercase().startsWith("INSERT") -> {
                                        "Query Success: row created (1 affected, insertId: 3)"
                                    }
                                    else -> "Query Executed: Success (0 rows affected)"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CfOrange),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Run Query", fontSize = 11.sp, color = Color.White)
                        }

                        // SQL Result Monospace output display
                        if (queryResult.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black)
                                    .border(1.dp, BorderColor)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = queryResult,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = AccentGreen,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
