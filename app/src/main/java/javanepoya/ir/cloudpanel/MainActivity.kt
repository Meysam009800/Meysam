package javanepoya.ir.cloudpanel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javanepoya.ir.cloudpanel.ui.screens.*
import javanepoya.ir.cloudpanel.ui.theme.*
import javanepoya.ir.cloudpanel.viewmodel.CloudflareViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: CloudflareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val appLanguage by viewModel.appLanguage.collectAsState()
            val isLocked by viewModel.isLocked.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            CfSwitcherTheme {
                // Dynamic Layout Direction (Persian -> RTL, English -> LTR)
                val layoutDirection = if (appLanguage == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr
                
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isLocked) {
                                // Locked State Security Overlay screen
                                SecurityLockScreen(
                                    lang = appLanguage,
                                    onUnlock = { pin ->
                                        viewModel.unlockWithPin(pin)
                                    }
                                )
                            } else {
                                // Fully functional screen router
                                Crossfade(
                                    targetState = currentScreen,
                                    animationSpec = tween(250)
                                ) { screen ->
                                    when (screen) {
                                        "dashboard" -> HomeScreenView(
                                            viewModel = viewModel,
                                            lang = appLanguage,
                                            onNavigate = { viewModel.navigateTo(it) }
                                        )
                                        "workers" -> WorkerStudioView(
                                            viewModel = viewModel,
                                            lang = appLanguage,
                                            onBack = { viewModel.navigateTo("dashboard") }
                                        )
                                        "dns" -> DnsManagerScreenView(
                                            viewModel = viewModel,
                                            lang = appLanguage,
                                            onBack = { viewModel.navigateTo("dashboard") }
                                        )
                                        "zone_manager" -> ZoneManagerScreenView(
                                            viewModel = viewModel,
                                            lang = appLanguage,
                                            onBack = { viewModel.navigateTo("dashboard") }
                                        )
                                        "r2" -> R2KvD1ScreenView(
                                            viewModel = viewModel,
                                            tab = "r2",
                                            lang = appLanguage,
                                            onBack = { viewModel.navigateTo("dashboard") }
                                        )
                                        "kv" -> R2KvD1ScreenView(
                                            viewModel = viewModel,
                                            tab = "kv",
                                            lang = appLanguage,
                                            onBack = { viewModel.navigateTo("dashboard") }
                                        )
                                        "d1" -> R2KvD1ScreenView(
                                            viewModel = viewModel,
                                            tab = "d1",
                                            lang = appLanguage,
                                            onBack = { viewModel.navigateTo("dashboard") }
                                        )
                                        "analytics" -> AnalyticsScreenView(
                                            viewModel = viewModel,
                                            lang = appLanguage,
                                            onBack = { viewModel.navigateTo("dashboard") }
                                        )
                                        "tunnels" -> TunnelScreenView(
                                            viewModel = viewModel,
                                            lang = appLanguage,
                                            onBack = { viewModel.navigateTo("dashboard") }
                                        )
                                        "notifications" -> NotificationScreenView(
                                            lang = appLanguage,
                                            onBack = { viewModel.navigateTo("dashboard") }
                                        )
                                        "settings" -> SettingsScreenView(
                                            viewModel = viewModel,
                                            lang = appLanguage,
                                            onBack = { viewModel.navigateTo("dashboard") }
                                        )
                                        else -> HomeScreenView(
                                            viewModel = viewModel,
                                            lang = appLanguage,
                                            onNavigate = { viewModel.navigateTo(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Save the active Cloudflare session (cookies, local storage, etc.)
        viewModel.saveActiveSession()
        // Implement auto-lock on pause / background
        viewModel.lockApp()
    }
}

@Composable
fun SecurityLockScreen(
    lang: String,
    onUnlock: (String) -> Boolean
) {
    var pinValue by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "App Locked",
            tint = CfOrange,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = Translations.get("enter_pin", lang),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = Translations.get("auth_required", lang),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Bullet dots showing input length
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            for (i in 1..4) {
                val filled = pinValue.length >= i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) CfOrange else Color.Transparent)
                        .border(2.dp, if (pinError) AccentRed else BorderColor, CircleShape)
                )
            }
        }

        if (pinError) {
            Text(
                Translations.get("wrong_pin", lang),
                color = AccentRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Custom Security Numpad Keyboard
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach { char ->
                        if (char == "BIO") {
                            // Biometrics quick-click mockup
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(DarkSlateCard)
                                    .clickable {
                                        // Auto unlock for demo / biometric simulation
                                        onUnlock("1234")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Fingerprint, "Biometric", tint = CfOrange)
                            }
                        } else if (char == "DEL") {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(DarkSlateCard)
                                    .clickable {
                                        if (pinValue.isNotEmpty()) {
                                            pinValue = pinValue.dropLast(1)
                                            pinError = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("←", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(DarkSlateCard)
                                    .clickable {
                                        if (pinValue.length < 4) {
                                            pinValue += char
                                            pinError = false
                                            if (pinValue.length == 4) {
                                                val success = onUnlock(pinValue)
                                                if (!success) {
                                                    pinError = true
                                                    pinValue = ""
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(char, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
