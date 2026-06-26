package javanepoya.ir.cloudpanel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javanepoya.ir.cloudpanel.ui.theme.*
import javanepoya.ir.cloudpanel.viewmodel.CloudflareViewModel

@Composable
fun AnalyticsScreenView(
    viewModel: CloudflareViewModel,
    lang: String,
    onBack: () -> Unit
) {
    // Dynamic simulated analytics data points
    val trafficPoints = listOf(12f, 24f, 18f, 32f, 45f, 38f, 52f)
    val trafficLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val requestsPoints = listOf(140f, 280f, 190f, 350f, 520f, 440f, 610f)
    val requestsLabels = listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00")

    val workerPoints = listOf(5f, 15f, 8f, 20f, 35f, 28f, 42f)
    val workerLabels = listOf("api-router", "sec-gateway", "cors-handler", "geo-router")

    Column(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        AppHeader(
            title = Translations.get("analytics", lang),
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
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                CustomAnalyticsChart(
                    title = Translations.get("traffic", lang) + " (GB)",
                    points = trafficPoints,
                    labels = trafficLabels,
                    color = CfOrange
                )
            }

            item {
                CustomAnalyticsChart(
                    title = Translations.get("requests", lang) + " (k)",
                    points = requestsPoints,
                    labels = requestsLabels,
                    color = AccentBlue
                )
            }

            item {
                CustomAnalyticsChart(
                    title = Translations.get("worker_usage", lang) + " (M)",
                    points = workerPoints,
                    labels = workerLabels,
                    color = AccentGreen
                )
            }
        }
    }
}
