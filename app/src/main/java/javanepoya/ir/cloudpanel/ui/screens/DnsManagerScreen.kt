package javanepoya.ir.cloudpanel.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
import javanepoya.ir.cloudpanel.data.DnsRecordEntity
import javanepoya.ir.cloudpanel.ui.theme.*
import javanepoya.ir.cloudpanel.viewmodel.CloudflareViewModel

@Composable
fun DnsManagerScreenView(
    viewModel: CloudflareViewModel,
    lang: String,
    onBack: () -> Unit
) {
    val currentAccount by viewModel.currentAccount.collectAsState()
    val dnsRecords by viewModel.currentDnsRecords.collectAsState()
 
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<DnsRecordEntity?>(null) }
 
    val filteredRecords = dnsRecords.filter {
        searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true) || it.type.contains(searchQuery, ignoreCase = true)
    }
 
    Box(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = Translations.get("dns_manager", lang),
                lang = lang,
                onBack = onBack,
                onSearchClicked = {},
                onAddAccountClicked = {}
            )
 
            // DNS search bar and Add Record action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (lang == "fa") "جستجوی رکوردهای DNS" else "Search records...", color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CfOrange,
                        unfocusedBorderColor = BorderColor
                    )
                )
 
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CfOrange),
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, "")
                }
            }
 
            // Lazy column of DNS items
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredRecords.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.HelpOutline, "", tint = TextTertiary, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (lang == "fa") "هیچ رکوردی یافت نشد" else "No DNS Records Found",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
 
                items(filteredRecords) { record ->
                    DnsRecordItemCard(
                        record = record,
                        lang = lang,
                        onToggleProxy = {
                            viewModel.updateDnsRecord(record.copy(proxied = !record.proxied))
                        },
                        onEdit = {
                            recordToEdit = record
                        },
                        onDelete = {
                            viewModel.deleteDnsRecord(record)
                        }
                    )
                }
            }
        }
 
        // Add DNS record popup form
        if (showAddDialog) {
            AddDnsRecordDialog(
                lang = lang,
                onDismiss = { showAddDialog = false },
                onAdd = { type, name, content, ttl, proxied ->
                    viewModel.addDnsRecord(type, name, content, ttl, proxied)
                    showAddDialog = false
                }
            )
        }

        // Edit DNS record popup form
        if (recordToEdit != null) {
            EditDnsRecordDialog(
                record = recordToEdit!!,
                lang = lang,
                onDismiss = { recordToEdit = null },
                onSave = { type, name, content, ttl, proxied ->
                    viewModel.updateDnsRecord(recordToEdit!!.copy(
                        type = type,
                        name = name,
                        content = content,
                        ttl = ttl,
                        proxied = proxied
                    ))
                    recordToEdit = null
                }
            )
        }
    }
}
 
@Composable
fun DnsRecordItemCard(
    record: DnsRecordEntity,
    lang: String,
    onToggleProxy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Type Badge (A, CNAME, etc.)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CfOrange.copy(0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = record.type,
                            color = CfOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = record.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TTL: ${if (record.ttl == 1) "Auto" else "${record.ttl}s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
 
            // Cloudflare proxy status toggle, edit, & delete
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Cloud icon indicating proxy state
                IconButton(onClick = onToggleProxy) {
                    Icon(
                        imageVector = if (record.proxied) Icons.Default.Cloud else Icons.Default.CloudQueue,
                        contentDescription = "Proxy",
                        tint = if (record.proxied) CfOrange else TextTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddDnsRecordDialog(
    lang: String,
    onDismiss: () -> Unit,
    onAdd: (type: String, name: String, content: String, ttl: Int, proxied: Boolean) -> Unit
) {
    val types = listOf("A", "AAAA", "CNAME", "TXT", "MX", "NS", "SRV")
    var selectedType by remember { mutableStateOf("A") }
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var ttl by remember { mutableStateOf("300") }
    var proxied by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Translations.get("add_account", lang), color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = DarkSlateCard,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && content.isNotEmpty()) {
                        val ttlVal = ttl.toIntOrNull() ?: 300
                        onAdd(selectedType, name, content, ttlVal, proxied)
                    }
                },
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
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // DNS Type picker
                Text("Record Type", color = Color.White, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { t ->
                        val isSelected = selectedType == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CfOrange else DeepObsidian)
                                .border(1.dp, if (isSelected) Color.White else BorderColor, RoundedCornerShape(6.dp))
                                .clickable { selectedType = t }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(t, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Record Name (e.g. api, @)", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CfOrange,
                        unfocusedBorderColor = BorderColor
                    )
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("IP address / Target destination", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CfOrange,
                        unfocusedBorderColor = BorderColor
                    )
                )

                OutlinedTextField(
                    value = ttl,
                    onValueChange = { ttl = it },
                    label = { Text("TTL (seconds)", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CfOrange,
                        unfocusedBorderColor = BorderColor
                    )
                )

                // Proxied checkbox
                if (selectedType in listOf("A", "AAAA", "CNAME")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = proxied,
                            onCheckedChange = { proxied = it },
                            colors = CheckboxDefaults.colors(checkedColor = CfOrange)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translations.get("proxy", lang), color = Color.White)
                    }
                }
            }
        }
    )
}

@Composable
fun EditDnsRecordDialog(
    record: DnsRecordEntity,
    lang: String,
    onDismiss: () -> Unit,
    onSave: (type: String, name: String, content: String, ttl: Int, proxied: Boolean) -> Unit
) {
    val types = listOf("A", "AAAA", "CNAME", "TXT", "MX", "NS", "SRV")
    var selectedType by remember { mutableStateOf(record.type) }
    var name by remember { mutableStateOf(record.name) }
    var content by remember { mutableStateOf(record.content) }
    var ttl by remember { mutableStateOf(record.ttl.toString()) }
    var proxied by remember { mutableStateOf(record.proxied) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (lang == "fa") "ویرایش رکورد DNS" else "Edit DNS Record", color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = DarkSlateCard,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && content.isNotEmpty()) {
                        val ttlVal = ttl.toIntOrNull() ?: 300
                        onSave(selectedType, name, content, ttlVal, proxied)
                    }
                },
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
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // DNS Type picker
                Text("Record Type", color = Color.White, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { t ->
                        val isSelected = selectedType == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CfOrange else DeepObsidian)
                                .border(1.dp, if (isSelected) Color.White else BorderColor, RoundedCornerShape(6.dp))
                                .clickable { selectedType = t }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(t, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Record Name (e.g. api, @)", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CfOrange,
                        unfocusedBorderColor = BorderColor
                    )
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("IP address / Target destination", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CfOrange,
                        unfocusedBorderColor = BorderColor
                    )
                )

                OutlinedTextField(
                    value = ttl,
                    onValueChange = { ttl = it },
                    label = { Text("TTL (seconds)", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CfOrange,
                        unfocusedBorderColor = BorderColor
                    )
                )

                // Proxied checkbox
                if (selectedType in listOf("A", "AAAA", "CNAME")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = proxied,
                            onCheckedChange = { proxied = it },
                            colors = CheckboxDefaults.colors(checkedColor = CfOrange)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translations.get("proxy", lang), color = Color.White)
                    }
                }
            }
        }
    )
}
