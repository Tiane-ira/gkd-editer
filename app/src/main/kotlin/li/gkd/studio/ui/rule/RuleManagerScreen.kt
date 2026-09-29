package li.gkd.studio.ui.rule

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import li.gkd.studio.data.model.GkdRule
import li.gkd.studio.rule.OutputScope
import li.gkd.studio.rule.RuleGenerator
import li.gkd.studio.ui.theme.IndigoPrimary
import li.gkd.studio.ui.theme.RoseError

@Composable
fun RuleManagerScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val ruleDao = remember { App.instance.database.ruleDao() }

    var searchQuery by remember { mutableStateOf("") }
    val rulesFlow = remember(searchQuery) {
        if (searchQuery.isBlank()) ruleDao.getAllRules() else ruleDao.searchRules(searchQuery)
    }
    val rules by rulesFlow.collectAsState(initial = emptyList())

    // Group rules by app
    val appGroups = remember(rules) {
        rules.groupBy { it.appId to it.appName }
    }

    val expandedApps = remember { mutableStateMapOf<String, Boolean>() }

    // Batch Selection State
    var isBatchMode by remember { mutableStateOf(false) }
    val selectedRuleIds = remember { mutableStateListOf<Long>() }

    var ruleToEdit by remember { mutableStateOf<GkdRule?>(null) }
    var ruleToDelete by remember { mutableStateOf<GkdRule?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchExportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (isBatchMode) {
            // Batch Mode Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        isBatchMode = false
                        selectedRuleIds.clear()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "退出多选")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "已选择 ${selectedRuleIds.size} 条规则",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val isAllSelected = rules.isNotEmpty() && selectedRuleIds.size == rules.size
                TextButton(onClick = {
                    if (isAllSelected) {
                        selectedRuleIds.clear()
                    } else {
                        selectedRuleIds.clear()
                        selectedRuleIds.addAll(rules.map { it.id })
                    }
                }) {
                    Text(
                        text = if (isAllSelected) "取消全选" else "全选",
                        fontWeight = FontWeight.SemiBold,
                        color = IndigoPrimary
                    )
                }
            }
        } else {
            // Normal Search & Batch Entry Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索规则或应用...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (rules.isNotEmpty()) {
                    Button(
                        onClick = { isBatchMode = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Text("批量", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (rules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Rule,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "暂无规则" else "未找到匹配规则",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "审查快照时点击「生成 GKD 规则」并保存，规则将显示在此处",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (isBatchMode) 76.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(appGroups.keys.toList()) { (appId, appName) ->
                        val appRules = appGroups[appId to appName] ?: emptyList()
                        val isExpanded = expandedApps[appId] ?: true

                        val allAppRulesSelected = appRules.isNotEmpty() && appRules.all { selectedRuleIds.contains(it.id) }

                        AppRuleGroupCard(
                            appId = appId,
                            appName = appName,
                            rules = appRules,
                            isExpanded = isExpanded,
                            isBatchMode = isBatchMode,
                            allAppRulesSelected = allAppRulesSelected,
                            selectedRuleIds = selectedRuleIds,
                            onToggleAppSelect = {
                                if (allAppRulesSelected) {
                                    appRules.forEach { selectedRuleIds.remove(it.id) }
                                } else {
                                    appRules.forEach {
                                        if (!selectedRuleIds.contains(it.id)) selectedRuleIds.add(it.id)
                                    }
                                }
                            },
                            onToggleRuleSelect = { ruleId ->
                                if (selectedRuleIds.contains(ruleId)) {
                                    selectedRuleIds.remove(ruleId)
                                } else {
                                    selectedRuleIds.add(ruleId)
                                }
                            },
                            onLongClickRule = { ruleId ->
                                if (!isBatchMode) {
                                    isBatchMode = true
                                    selectedRuleIds.add(ruleId)
                                }
                            },
                            onToggleExpand = { expandedApps[appId] = !isExpanded },
                            onEditRule = { ruleToEdit = it },
                            onCopyRule = { rule ->
                                clipboardManager.setText(AnnotatedString(rule.ruleJson5))
                                Toast.makeText(context, "已复制规则 JSON5", Toast.LENGTH_SHORT).show()
                            },
                            onShareRule = { rule ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, rule.ruleJson5)
                                    putExtra(Intent.EXTRA_TITLE, "${rule.appName} - ${rule.groupName}")
                                }
                                context.startActivity(Intent.createChooser(intent, "分享规则"))
                            },
                            onDeleteRule = { ruleToDelete = it }
                        )
                    }
                }

                // Batch Mode Bottom Floating Action Bar
                if (isBatchMode) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { showBatchExportDialog = true },
                                enabled = selectedRuleIds.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("批量导出 (${selectedRuleIds.size})")
                            }

                            OutlinedButton(
                                onClick = { showBatchDeleteDialog = true },
                                enabled = selectedRuleIds.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedRuleIds.isNotEmpty()) RoseError else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = if (selectedRuleIds.isNotEmpty()) RoseError else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("批量删除 (${selectedRuleIds.size})", color = if (selectedRuleIds.isNotEmpty()) RoseError else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Batch Export Dialog
    if (showBatchExportDialog) {
        val selectedRules = rules.filter { it.id in selectedRuleIds }
        BatchExportRuleDialog(
            selectedRules = selectedRules,
            onDismiss = { showBatchExportDialog = false }
        )
    }

    // Batch Delete Dialog
    if (showBatchDeleteDialog) {
        val count = selectedRuleIds.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("确认批量删除规则") },
            text = { Text("是否删除选中的 $count 条规则？删除后不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                ruleDao.deleteRulesByIds(selectedRuleIds.toList())
                            }
                            selectedRuleIds.clear()
                            isBatchMode = false
                            showBatchDeleteDialog = false
                            Toast.makeText(context, "已成功删除 $count 条规则", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("删除全部", color = RoseError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Edit Rule Dialog
    if (ruleToEdit != null) {
        val target = ruleToEdit!!
        var editedName by remember { mutableStateOf(target.groupName) }
        var editedJson by remember { mutableStateOf(target.ruleJson5) }

        AlertDialog(
            onDismissRequest = { ruleToEdit = null },
            title = { Text("编辑规则") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("规则名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedJson,
                        onValueChange = { editedJson = it },
                        label = { Text("GKD JSON5") },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            ruleDao.updateRule(
                                target.copy(
                                    groupName = editedName,
                                    ruleJson5 = editedJson,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                        ruleToEdit = null
                        Toast.makeText(context, "规则已更新", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToEdit = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Single Delete Confirmation Dialog
    if (ruleToDelete != null) {
        val target = ruleToDelete!!
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text("确认删除规则") },
            text = { Text("是否删除规则「${target.groupName}」？") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            ruleDao.deleteRule(target)
                        }
                        ruleToDelete = null
                        Toast.makeText(context, "规则已删除", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("删除", color = RoseError)
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun BatchExportRuleDialog(
    selectedRules: List<GkdRule>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uniqueApps = remember(selectedRules) { selectedRules.map { it.appId }.distinct() }
    val isSingleApp = uniqueApps.size == 1

    var exportScope by remember {
        mutableStateOf(if (isSingleApp) OutputScope.APP else OutputScope.SUBSCRIPTION)
    }

    val generatedCode = remember(selectedRules, exportScope) {
        if (exportScope == OutputScope.APP && isSingleApp) {
            val first = selectedRules.first()
            RuleGenerator.generateBatchAppRulesJson5(first.appId, first.appName, selectedRules)
        } else {
            RuleGenerator.generateBatchRulesJson5(selectedRules)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("批量导出规则 (${selectedRules.size} 条)")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isSingleApp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = exportScope == OutputScope.APP,
                            onClick = { exportScope = OutputScope.APP },
                            label = { Text("App 规则", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = exportScope == OutputScope.SUBSCRIPTION,
                            onClick = { exportScope = OutputScope.SUBSCRIPTION },
                            label = { Text("完整订阅", fontSize = 12.sp) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = if (exportScope == OutputScope.APP) "可直接复制并覆盖/粘贴到 GKD 该应用的本地规则中"
                    else "可直接在 GKD 设置中作为自定义订阅导入",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = generatedCode,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, generatedCode)
                        putExtra(Intent.EXTRA_TITLE, "GKD 规则批量导出")
                    }
                    context.startActivity(Intent.createChooser(intent, "分享导出的规则"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("分享")
                }

                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(generatedCode))
                        Toast.makeText(context, "已复制批量规则 JSON5", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun AppRuleGroupCard(
    appId: String,
    appName: String,
    rules: List<GkdRule>,
    isExpanded: Boolean,
    isBatchMode: Boolean,
    allAppRulesSelected: Boolean,
    selectedRuleIds: List<Long>,
    onToggleAppSelect: () -> Unit,
    onToggleRuleSelect: (Long) -> Unit,
    onLongClickRule: (Long) -> Unit,
    onToggleExpand: () -> Unit,
    onEditRule: (GkdRule) -> Unit,
    onCopyRule: (GkdRule) -> Unit,
    onShareRule: (GkdRule) -> Unit,
    onDeleteRule: (GkdRule) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // App Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isBatchMode) {
                        Checkbox(
                            checked = allAppRulesSelected,
                            onCheckedChange = { onToggleAppSelect() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = IndigoPrimary,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = appName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(IndigoPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${rules.size} 条规则",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IndigoPrimary
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (rule in rules) {
                        val isSelected = selectedRuleIds.contains(rule.id)
                        RuleItemCard(
                            rule = rule,
                            isBatchMode = isBatchMode,
                            isSelected = isSelected,
                            onToggleSelect = { onToggleRuleSelect(rule.id) },
                            onLongClick = { onLongClickRule(rule.id) },
                            onEdit = { onEditRule(rule) },
                            onCopy = { onCopyRule(rule) },
                            onShare = { onShareRule(rule) },
                            onDelete = { onDeleteRule(rule) }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RuleItemCard(
    rule: GkdRule,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isBatchMode) onToggleSelect()
                },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) IndigoPrimary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = if (isSelected) BorderStroke(1.5.dp, IndigoPrimary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isBatchMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = IndigoPrimary,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = "└─ ${rule.groupName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!isBatchMode) {
                    Row {
                        IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = rule.selector,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2
            )

            if (!rule.activityIds.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Activity: ${rule.activityIds}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
