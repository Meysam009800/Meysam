package javanepoya.ir.cloudpanel.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javanepoya.ir.cloudpanel.data.WorkerEntity
import javanepoya.ir.cloudpanel.data.WorkerTemplate
import javanepoya.ir.cloudpanel.ui.theme.*
import javanepoya.ir.cloudpanel.viewmodel.CloudflareViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkerStudioView(
    viewModel: CloudflareViewModel,
    lang: String,
    onBack: () -> Unit
) {
    val currentAccount by viewModel.currentAccount.collectAsState()
    val workers by viewModel.currentWorkers.collectAsState()
    val editorCode by viewModel.editorCode.collectAsState()
    val editorWorker by viewModel.editorWorker.collectAsState()
    val isProfessionalEditor by viewModel.editorIsProfessional.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var selectedTemplateCode by remember { mutableStateOf("") }
    var newWorkerName by remember { mutableStateOf("") }

    // Cloudflare standard template definitions
    val templates = WorkerTemplate.templates

    Box(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        // If actively editing a worker, show the CodeEditor view (covers whole screen)
        if (editorWorker != null) {
            CodeEditorView(
                code = editorCode,
                onCodeChange = { viewModel.setEditorCode(it) },
                lang = lang,
                onSave = {
                    viewModel.saveAndDeployWorker(editorWorker!!.name, editorCode)
                },
                onCancel = {
                    viewModel.selectWorkerForEditing(editorWorker!!, isProfessionalEditor)
                    // Trigger cancel to close editor
                    viewModel.saveAndDeployWorker(editorWorker!!.name, editorWorker!!.script)
                },
                viewModel = viewModel
            )
        } else {
            // Main Worker list & templates view
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeader(
                    title = Translations.get("worker_studio", lang),
                    lang = lang,
                    onBack = onBack,
                    onSearchClicked = {},
                    onAddAccountClicked = {}
                )

                // Actions header bar (Create worker, Templates)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CfOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, "")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translations.get("create_worker", lang), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showTemplatesDialog = true },
                        border = BorderStroke(1.dp, CfOrange),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CfOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Dashboard, "")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translations.get("worker_templates", lang), fontWeight = FontWeight.Bold)
                    }
                }

                // Lazy list of configured Workers
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (workers.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CodeOff, "", tint = TextTertiary, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (lang == "fa") "هیچ ورکری برای این حساب ثبت نشده است" else "No Workers active",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    items(workers) { worker ->
                        WorkerItemCard(
                            worker = worker,
                            lang = lang,
                            onEditSimple = {
                                viewModel.selectWorkerForEditing(worker, isProfessional = false)
                            },
                            onEditProfessional = {
                                viewModel.selectWorkerForEditing(worker, isProfessional = true)
                            },
                            onDuplicate = { viewModel.duplicateWorker(worker) },
                            onDelete = { viewModel.deleteWorker(worker) }
                        )
                    }
                }
            }
        }

        // Dialog to create a new Worker name input
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text(Translations.get("create_worker", lang), color = Color.White) },
                containerColor = DarkSlateCard,
                confirmButton = {
                    Button(
                        onClick = {
                            if (newWorkerName.isNotEmpty()) {
                                val templateScript = if (selectedTemplateCode.isNotEmpty()) selectedTemplateCode else """
                                    export default {
                                      async fetch(request, env, ctx) {
                                        return new Response('Hello from CF Switcher!');
                                      },
                                    };
                                """.trimIndent()
                                
                                val newW = WorkerEntity(
                                    accountId = currentAccount?.id ?: 0,
                                    name = newWorkerName,
                                    script = templateScript
                                )
                                viewModel.saveAndDeployWorker(newW.name, newW.script)
                                showCreateDialog = false
                                newWorkerName = ""
                                selectedTemplateCode = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CfOrange)
                    ) {
                        Text(Translations.get("deploy", lang), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text(Translations.get("cancel", lang), color = TextSecondary)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newWorkerName,
                            onValueChange = { newWorkerName = it },
                            label = { Text("Worker Name", color = Color.White) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CfOrange,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                    }
                }
            )
        }

        // Dialog for pre-built templates
        if (showTemplatesDialog) {
            AlertDialog(
                onDismissRequest = { showTemplatesDialog = false },
                title = { Text(Translations.get("worker_templates", lang), color = Color.White, fontWeight = FontWeight.Bold) },
                containerColor = DarkSlateCard,
                confirmButton = {
                    TextButton(onClick = { showTemplatesDialog = false }) {
                        Text(Translations.get("cancel", lang), color = CfOrange)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        templates.forEach { template ->
                            val displayName = if (lang == "fa") template.nameFa else template.name
                            val displayDesc = if (lang == "fa") template.descriptionFa else template.description
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTemplateCode = template.code
                                        newWorkerName = template.name.lowercase().replace(" ", "-")
                                        showTemplatesDialog = false
                                        showCreateDialog = true
                                    },
                                colors = CardDefaults.cardColors(containerColor = DeepObsidian),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.FeaturedPlayList, "", tint = CfOrange)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(displayDesc, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun WorkerItemCard(
    worker: WorkerEntity,
    lang: String,
    onEditSimple: () -> Unit,
    onEditProfessional: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(worker.updatedAt) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(worker.updatedAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, "", tint = CfOrange, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = worker.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Active status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentGreen.copy(0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(worker.status, style = MaterialTheme.typography.bodySmall, color = AccentGreen)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${Translations.get("last_active", lang)}: $dateString",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BorderColor)
            Spacer(modifier = Modifier.height(10.dp))

            // Simple & Professional Code Editor buttons + actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onDuplicate) {
                        Icon(Icons.Default.ContentCopy, "Duplicate", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = AccentRed, modifier = Modifier.size(18.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEditSimple,
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(Translations.get("simple_editor", lang).split(" ").first(), fontSize = 11.sp)
                    }

                    Button(
                        onClick = onEditProfessional,
                        colors = ButtonDefaults.buttonColors(containerColor = CfOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(Translations.get("professional_editor", lang).split(" ").first(), fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
