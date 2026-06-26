package javanepoya.ir.cloudpanel.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javanepoya.ir.cloudpanel.ui.theme.*

@Composable
fun NotificationScreenView(
    lang: String,
    onBack: () -> Unit
) {
    // Elegant, highly realistic mock notifications indicating typical Cloudflare issues
    val items = listOf(
        Triple(
            Translations.get("down_alerts", lang),
            "javanepoya.ir is down! Code 502 Bad Gateway (Origin error)",
            AccentRed
        ),
        Triple(
            Translations.get("worker_errors", lang),
            "Worker: api-router failed with exception: 'ReferenceError: request is not defined' (Line 14)",
            CfOrange
        ),
        Triple(
            Translations.get("dns_issues", lang),
            "DNS Warning: MX records detected pointing directly to cloud-proxied IP address. Mail delivery might fail.",
            AccentBlue
        ),
        Triple(
            "WAF Protection",
            "Firewall blocks peaked! Prevented 420 SQL injection attempts from IP range 182.16.x.x",
            AccentGreen
        )
    )

    Column(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        AppHeader(
            title = Translations.get("notifications", lang),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(items) { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(alert.third.copy(0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (alert.third) {
                                    AccentRed -> Icons.Default.ErrorOutline
                                    CfOrange -> Icons.Default.Code
                                    AccentBlue -> Icons.Default.Warning
                                    else -> Icons.Default.Shield
                                },
                                contentDescription = "",
                                tint = alert.third
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alert.first,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = alert.second,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
