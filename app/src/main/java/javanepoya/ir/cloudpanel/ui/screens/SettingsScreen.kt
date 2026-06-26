package javanepoya.ir.cloudpanel.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@Composable
fun SettingsScreenView(
    viewModel: CloudflareViewModel,
    lang: String,
    onBack: () -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isBiometricEnabled by viewModel.biometricEnabled.collectAsState()

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var pinSetupInput by remember { mutableStateOf("") }
    var pinSetupConfirmInput by remember { mutableStateOf("") }
    var setupErrorMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        AppHeader(
            title = Translations.get("advanced_settings", lang),
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Localization Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, "", tint = CfOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                Translations.get("select_lang", lang),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // English Selection
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (appLanguage == "en") CfOrange else DeepObsidian)
                                    .border(1.dp, if (appLanguage == "en") Color.White else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setLanguage("en") }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(Translations.get("english", lang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // Persian Selection
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (appLanguage == "fa") CfOrange else DeepObsidian)
                                    .border(1.dp, if (appLanguage == "fa") Color.White else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setLanguage("fa") }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(Translations.get("persian", lang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Security Controls Card (PIN Lock, Biometrics)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            Translations.get("security", lang),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        // 1. PIN Lock action row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, "", tint = CfOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Translations.get("pin_lock", lang), color = Color.White)
                            }
                            Button(
                                onClick = { showPinSetupDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CfOrange),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(Translations.get("save", lang), color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Divider(color = BorderColor)

                        // 2. Fingerprint status toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, "", tint = CfOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Translations.get("fingerprint", lang), color = Color.White)
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { viewModel.toggleBiometric(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CfOrange)
                            )
                        }
                    }
                }
            }

            // Developer profile card as requested
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeveloperMode, "", tint = CfOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "System Information",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            Translations.get("developer", lang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Package: javanepoya.ir.cloudpanel",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Architecture: MVVM Model View ViewModel",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Security: AES-GCM Encrypted SharedPreferences & Keystore Container",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // PIN creation Dialog overlay
        if (showPinSetupDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPinSetupDialog = false
                    pinSetupInput = ""
                    pinSetupConfirmInput = ""
                    setupErrorMessage = ""
                },
                title = { Text(Translations.get("set_pin", lang), color = Color.White, fontWeight = FontWeight.Bold) },
                containerColor = DarkSlateCard,
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinSetupInput.length == 4 && pinSetupInput == pinSetupConfirmInput) {
                                viewModel.configurePin(pinSetupInput)
                                showPinSetupDialog = false
                                pinSetupInput = ""
                                pinSetupConfirmInput = ""
                                setupErrorMessage = ""
                            } else if (pinSetupInput != pinSetupConfirmInput) {
                                setupErrorMessage = Translations.get("pin_mismatch", lang)
                            } else {
                                setupErrorMessage = "PIN must be exactly 4 digits"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
                    ) {
                        Text(Translations.get("save", lang), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.disablePin()
                            showPinSetupDialog = false
                        }
                    ) {
                        Text("Disable PIN", color = AccentRed)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = pinSetupInput,
                            onValueChange = { if (it.length <= 4) pinSetupInput = it },
                            label = { Text("New 4-Digit PIN", color = Color.White) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CfOrange, unfocusedBorderColor = BorderColor)
                        )

                        OutlinedTextField(
                            value = pinSetupConfirmInput,
                            onValueChange = { if (it.length <= 4) pinSetupConfirmInput = it },
                            label = { Text("Confirm PIN", color = Color.White) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CfOrange, unfocusedBorderColor = BorderColor)
                        )

                        if (setupErrorMessage.isNotEmpty()) {
                            Text(setupErrorMessage, color = AccentRed, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            )
        }
    }
}
