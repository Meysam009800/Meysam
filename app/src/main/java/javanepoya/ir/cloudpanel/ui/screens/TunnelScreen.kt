package javanepoya.ir.cloudpanel.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javanepoya.ir.cloudpanel.data.TunnelEntity
import javanepoya.ir.cloudpanel.ui.theme.*
import javanepoya.ir.cloudpanel.viewmodel.CloudflareViewModel

@Composable
fun TunnelScreenView(
    viewModel: CloudflareViewModel,
    lang: String,
    onBack: () -> Unit
) {
    val tunnels by viewModel.currentTunnels.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newTunnelName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = Translations.get("tunnels", lang),
                lang = lang,
                onBack = onBack,
                onSearchClicked = {},
                onAddAccountClicked = {}
            )

            // Add tunnel action button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CfOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, "")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Cloudflare Tunnel", fontWeight = FontWeight.Bold)
                }
            }

            // Scrollable Tunnels List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (tunnels.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CompareArrows, "", tint = TextTertiary, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Cloudflare Tunnels Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }

                items(tunnels) { tunnel ->
                    TunnelItemCard(
                        tunnel = tunnel,
                        onToggle = { viewModel.toggleTunnelStatus(tunnel) },
                        onDelete = { viewModel.deleteTunnel(tunnel) }
                    )
                }
            }
        }

        // Create tunnel popup form
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create Tunnel", color = Color.White, fontWeight = FontWeight.Bold) },
                containerColor = DarkSlateCard,
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTunnelName.isNotEmpty()) {
                                viewModel.createTunnel(newTunnelName)
                                showCreateDialog = false
                                newTunnelName = ""
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
                        value = newTunnelName,
                        onValueChange = { newTunnelName = it },
                        label = { Text("Tunnel name (e.g. dev-laptop)", color = Color.White) },
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

@Composable
fun TunnelItemCard(
    tunnel: TunnelEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isHealthy = tunnel.status == "Healthy"

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
                    Icon(Icons.Default.CompareArrows, "", tint = CfOrange, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tunnel.name, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isHealthy) AccentGreen else AccentRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Status: ${tunnel.status}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Connections: ${tunnel.connections}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isHealthy) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = "Toggle status",
                        tint = if (isHealthy) CfOrange else AccentGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "", tint = AccentRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
