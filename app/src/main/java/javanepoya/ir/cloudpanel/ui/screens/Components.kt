package javanepoya.ir.cloudpanel.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javanepoya.ir.cloudpanel.ui.theme.*

// Live JS/TS Syntax Highlighter VisualTransformation
class JsSyntaxHighlighter(val searchQuery: String = "") : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val code = text.text
        val builder = AnnotatedString.Builder()
        builder.append(code)
        
        // Key JS/TS Cloudflare Worker keywords
        val keywords = setOf(
            "const", "let", "var", "function", "async", "await", "return", 
            "import", "export", "default", "from", "fetch", "Response", 
            "Headers", "if", "else", "try", "catch", "throw", "new", "class", 
            "extends", "super", "event", "addEventListener"
        )
        
        val wordRegex = "\\b(\\w+)\\b".toRegex()
        wordRegex.findAll(code).forEach { matchResult ->
            val word = matchResult.value
            if (keywords.contains(word)) {
                builder.addStyle(
                    SpanStyle(color = CfOrange, fontWeight = FontWeight.Bold),
                    matchResult.range.first,
                    matchResult.range.last + 1
                )
            }
        }
        
        // Highlight strings ("..." or '...')
        val stringRegex = "\"[^\"]*\"|'[^']*'".toRegex()
        stringRegex.findAll(code).forEach { matchResult ->
            builder.addStyle(
                SpanStyle(color = AccentGreen),
                matchResult.range.first,
                matchResult.range.last + 1
            )
        }

        // Highlight comments (// ...)
        val commentRegex = "//.*".toRegex()
        commentRegex.findAll(code).forEach { matchResult ->
            builder.addStyle(
                SpanStyle(color = TextTertiary, fontStyle = FontStyle.Italic),
                matchResult.range.first,
                matchResult.range.last + 1
            )
        }

        // Highlight search queries with a distinct style
        if (searchQuery.isNotEmpty()) {
            var index = code.indexOf(searchQuery, ignoreCase = true)
            while (index != -1) {
                builder.addStyle(
                    SpanStyle(background = Color(0xFFFFCC00), color = Color.Black, fontWeight = FontWeight.Bold),
                    index,
                    index + searchQuery.length
                )
                index = code.indexOf(searchQuery, index + searchQuery.length, ignoreCase = true)
            }
        }
        
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

// Sleek Custom Chart Component using local Canvas
@Composable
fun CustomAnalyticsChart(
    title: String,
    points: List<Float>,
    labels: List<String>,
    color: Color = CfOrange
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val maxPoint = points.maxOrNull() ?: 1f
                    val barWidth = width / (points.size * 1.5f)
                    val spacing = width / (points.size * 3f)

                    for (i in points.indices) {
                        val point = points[i]
                        val barHeight = (point / maxPoint) * (height - 30.dp.toPx())
                        val x = i * (barWidth + spacing) + spacing / 2
                        val y = height - barHeight - 20.dp.toPx()

                        // Draw background track
                        drawRoundRect(
                            color = color.copy(alpha = 0.1f),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, height - 20.dp.toPx()),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        // Draw dynamic bar
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.7f), color)
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Dynamic Header with fast account switching
@Composable
fun AppHeader(
    title: String,
    lang: String,
    onBack: (() -> Unit)? = null,
    onSearchClicked: () -> Unit,
    onAddAccountClicked: () -> Unit
) {
    // Top Bar Container seamlessly blending into DeepObsidian
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepObsidian)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (onBack == null) {
            // Immersive Dashboard Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile/Initials Avatar with elevation shadow styling
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentBlue)
                            .clickable { onAddAccountClicked() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JP",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Personal Prod",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "javanepoya.ir.cloudpanel",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search icon
                    IconButton(onClick = onSearchClicked) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                    
                    // Pro Mode Badge with Pulsing Orange Dot
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CfOrange.copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = "PRO MODE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        } else {
            // General Screen Header with Back Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = CfOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSearchClicked) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// Workspace Selector
@Composable
fun WorkspaceSelector(
    selected: String,
    lang: String,
    onSelect: (String) -> Unit
) {
    val items = listOf("All", "Personal", "Client", "Company")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val label = when (item) {
                "Personal" -> Translations.get("personal", lang).split(" ").first()
                "Client" -> Translations.get("client", lang).split(" ").first()
                "Company" -> Translations.get("company", lang).split(" ").first()
                else -> if (lang == "fa") "همه" else "All"
            }
            val isSelected = selected == item
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(item) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CfOrange,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

// Professional Code Editor Layout
@Composable
fun CodeEditorView(
    code: String,
    onCodeChange: (String) -> Unit,
    lang: String,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    viewModel: javanepoya.ir.cloudpanel.viewmodel.CloudflareViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var showSearchTools by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val lineCount = code.split("\n").size
    val numbersText = (1..lineCount).joinToString("\n")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
    ) {
        // Control Bar (Undo/Redo/Save)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { viewModel.editorUndo() }) {
                    Icon(Icons.Default.Undo, "Undo", tint = Color.White)
                }
                IconButton(onClick = { viewModel.editorRedo() }) {
                    Icon(Icons.Default.Redo, "Redo", tint = Color.White)
                }
                IconButton(onClick = { showSearchTools = !showSearchTools }) {
                    Icon(Icons.Default.FindReplace, "Search Replace", tint = Color.White)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
                ) {
                    Text(Translations.get("cancel", lang))
                }
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = CfOrange, contentColor = Color.White)
                ) {
                    Text(Translations.get("deploy_worker", lang))
                }
            }
        }

        // Search & Replace Tools (Dynamic Code Action)
        if (showSearchTools) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .background(DarkSlateCard, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val searchMatchCount = remember(code, searchQuery) {
                        if (searchQuery.isEmpty()) 0
                        else {
                            var count = 0
                            var index = code.indexOf(searchQuery, ignoreCase = true)
                            while (index != -1) {
                                count++
                                index = code.indexOf(searchQuery, index + searchQuery.length, ignoreCase = true)
                            }
                            count
                        }
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(Translations.get("search", lang), color = Color.White) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CfOrange,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = if (lang == "fa") "$searchMatchCount مورد" else "$searchMatchCount matches",
                            color = if (searchMatchCount > 0) AccentGreen else AccentRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = replaceQuery,
                        onValueChange = { replaceQuery = it },
                        label = { Text(Translations.get("replace", lang), color = Color.White) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CfOrange,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                    Button(
                        onClick = {
                            if (searchQuery.isNotEmpty()) {
                                val replaced = code.replace(searchQuery, replaceQuery)
                                onCodeChange(replaced)
                                keyboardController?.hide()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CfOrange),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(Translations.get("replace", lang))
                    }
                }
            }
        }

        // Editor Body (Double pane: line numbers & highlited editor text area)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF070B11))
                .padding(vertical = 8.dp)
        ) {
            // Line numbers column
            Text(
                text = numbersText,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = TextTertiary,
                lineHeight = 20.sp,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .width(42.dp)
                    .padding(end = 8.dp)
            )

            // Live-typing Syntax Highlighted Editor
            BasicTextField(
                value = code,
                onValueChange = {
                    // Check for auto-indent upon new line injection
                    if (it.length > code.length && it.endsWith("\n")) {
                        val lines = code.split("\n")
                        if (lines.isNotEmpty()) {
                            val lastLine = lines.last()
                            val spaces = lastLine.takeWhile { char -> char == ' ' }.length
                            onCodeChange(it + " ".repeat(spaces))
                        } else {
                            onCodeChange(it)
                        }
                    } else {
                        onCodeChange(it)
                    }
                },
                textStyle = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(CfOrange),
                visualTransformation = JsSyntaxHighlighter(searchQuery),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Default,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false
                )
            )
        }
    }
}
