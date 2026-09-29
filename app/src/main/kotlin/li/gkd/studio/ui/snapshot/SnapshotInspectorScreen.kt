package li.gkd.studio.ui.snapshot

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.gkd.studio.App
import li.gkd.studio.data.local.SnapshotRepository
import li.gkd.studio.data.model.Snapshot
import li.gkd.studio.data.model.SnapshotNode
import li.gkd.studio.ui.rule.RulePreviewDialog
import li.gkd.studio.ui.selector.SelectorTestSheet
import li.gkd.studio.ui.theme.EmeraldSuccess
import li.gkd.studio.ui.theme.IndigoPrimary
import java.io.File

enum class InspectorViewMode {
    SPLIT,
    CANVAS_ONLY,
    TREE_ONLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapshotInspectorScreen(
    snapshot: Snapshot,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var viewMode by remember { mutableStateOf(InspectorViewMode.SPLIT) }
    var allNodes by remember { mutableStateOf<List<SnapshotNode>>(emptyList()) }
    var selectedNode by remember { mutableStateOf<SnapshotNode?>(null) }
    var matchingNodes by remember { mutableStateOf<List<SnapshotNode>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    var showSelectorSheet by remember { mutableStateOf(false) }
    var showRuleDialog by remember { mutableStateOf(false) }
    var selectedSelectorForRule by remember { mutableStateOf("") }

    // Load screenshot bitmap and reconstructed tree
    val screenshotBitmap = remember(snapshot.screenshotPath) {
        val file = File(snapshot.screenshotPath)
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    LaunchedEffect(snapshot) {
        val nodes = SnapshotRepository.loadSnapshotNodes(snapshot)
        allNodes = nodes
        selectedNode = nodes.firstOrNull()
    }

    val filteredNodes = remember(allNodes, searchQuery) {
        if (searchQuery.isBlank()) {
            allNodes
        } else {
            val q = searchQuery.lowercase()
            allNodes.filter { node ->
                (node.attr.text?.lowercase()?.contains(q) == true) ||
                        (node.attr.desc?.lowercase()?.contains(q) == true) ||
                        (node.attr.vid?.lowercase()?.contains(q) == true) ||
                        (node.attr.id?.lowercase()?.contains(q) == true) ||
                        (node.attr.name?.lowercase()?.contains(q) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = snapshot.appName ?: snapshot.packageName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = snapshot.activity?.substringAfterLast('.') ?: snapshot.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewMode = when (viewMode) {
                            InspectorViewMode.SPLIT -> InspectorViewMode.CANVAS_ONLY
                            InspectorViewMode.CANVAS_ONLY -> InspectorViewMode.TREE_ONLY
                            InspectorViewMode.TREE_ONLY -> InspectorViewMode.SPLIT
                        }
                    }) {
                        Icon(
                            imageVector = when (viewMode) {
                                InspectorViewMode.SPLIT -> Icons.Default.ViewAgenda
                                InspectorViewMode.CANVAS_ONLY -> Icons.Default.ViewStream
                                InspectorViewMode.TREE_ONLY -> Icons.Default.ViewCarousel
                            },
                            contentDescription = "Toggle view"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Selected Node Action Bar
            if (selectedNode != null) {
                NodeBottomBar(
                    node = selectedNode!!,
                    onOpenSelector = { showSelectorSheet = true },
                    onCopyAttr = { attrName, value ->
                        clipboardManager.setText(AnnotatedString(value))
                        Toast.makeText(context, "已复制 $attrName", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (viewMode) {
                InspectorViewMode.CANVAS_ONLY -> {
                    InteractiveCanvas(
                        bitmap = screenshotBitmap,
                        nodes = allNodes,
                        selectedNode = selectedNode,
                        matchingNodes = matchingNodes,
                        onNodeSelected = { selectedNode = it }
                    )
                }
                InspectorViewMode.TREE_ONLY -> {
                    TreeSection(
                        nodes = filteredNodes,
                        selectedNode = selectedNode,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onNodeSelected = { selectedNode = it }
                    )
                }
                InspectorViewMode.SPLIT -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Canvas takes top 50%
                        Box(modifier = Modifier.fillMaxWidth().weight(1.1f)) {
                            InteractiveCanvas(
                                bitmap = screenshotBitmap,
                                nodes = allNodes,
                                selectedNode = selectedNode,
                                matchingNodes = matchingNodes,
                                onNodeSelected = { selectedNode = it }
                            )
                        }

                        // UI Tree takes bottom
                        Box(modifier = Modifier.fillMaxWidth().weight(0.9f)) {
                            TreeSection(
                                nodes = filteredNodes,
                                selectedNode = selectedNode,
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                onNodeSelected = { selectedNode = it }
                            )
                        }
                    }
                }
            }

            // Selector Testing BottomSheet
            if (showSelectorSheet && selectedNode != null) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = {
                        showSelectorSheet = false
                        matchingNodes = emptyList()
                    },
                    sheetState = sheetState
                ) {
                    SelectorTestSheet(
                        node = selectedNode!!,
                        allNodes = allNodes,
                        onMatchingNodesChanged = { matchingNodes = it },
                        onGenerateRule = { sel ->
                            selectedSelectorForRule = sel
                            showSelectorSheet = false
                            showRuleDialog = true
                        },
                        onDismiss = {
                            showSelectorSheet = false
                            matchingNodes = emptyList()
                        }
                    )
                }
            }

            // Rule Preview Dialog
            if (showRuleDialog) {
                RulePreviewDialog(
                    appId = snapshot.packageName,
                    appName = snapshot.appName ?: snapshot.packageName,
                    activityId = snapshot.activity,
                    selector = selectedSelectorForRule,
                    onSaveRule = { rule ->
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                App.instance.database.ruleDao().insertRule(rule)
                            }
                            showRuleDialog = false
                            Toast.makeText(context, "已保存到「规则管理」", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { showRuleDialog = false }
                )
            }
        }
    }
}

@Composable
private fun NodeBottomBar(
    node: SnapshotNode,
    onOpenSelector: () -> Unit,
    onCopyAttr: (String, String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.attr.name?.substringAfterLast('.') ?: "View",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = IndigoPrimary
                    )

                    val textVal = node.attr.text ?: node.attr.desc ?: node.attr.vid ?: node.attr.id
                    if (!textVal.isNullOrBlank()) {
                        Text(
                            text = textVal,
                            fontSize = 12.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Bounds: [${node.attr.left}, ${node.attr.top}, ${node.attr.right}, ${node.attr.bottom}] · ${node.attr.width}x${node.attr.height}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onOpenSelector,
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("生成 Selector", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick attribute chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!node.attr.text.isNullOrEmpty()) {
                    AttrChip("text: ${node.attr.text}") { onCopyAttr("text", node.attr.text!!) }
                }
                if (!node.attr.vid.isNullOrEmpty()) {
                    AttrChip("vid: ${node.attr.vid}") { onCopyAttr("vid", node.attr.vid!!) }
                } else if (!node.attr.id.isNullOrEmpty()) {
                    AttrChip("id: ${node.attr.id}") { onCopyAttr("id", node.attr.id!!) }
                }
                if (node.attr.clickable) {
                    AttrChip("clickable=true") { onCopyAttr("clickable", "true") }
                }
            }
        }
    }
}

@Composable
private fun AttrChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun TreeSection(
    nodes: List<SnapshotNode>,
    selectedNode: SnapshotNode?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNodeSelected: (SnapshotNode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("搜索节点 (text, id, class)...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp)
        ) {
            items(nodes, key = { it.id }) { node ->
                val isSelected = node.id == selectedNode?.id
                val indent = (node.attr.depth * 14).dp

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) IndigoPrimary.copy(alpha = 0.2f) else Color.Transparent
                        )
                        .clickable { onNodeSelected(node) }
                        .padding(start = indent, top = 4.dp, bottom = 4.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (node.children.isNotEmpty()) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = if (node.children.isNotEmpty()) IndigoPrimary else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = node.label,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
