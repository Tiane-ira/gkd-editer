package li.gkd.studio.ui.snapshot

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import li.gkd.studio.App
import li.gkd.studio.data.local.SnapshotRepository
import li.gkd.studio.data.model.Snapshot
import li.gkd.studio.ui.theme.IndigoPrimary
import li.gkd.studio.ui.theme.RoseError
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SnapshotManagerScreen(
    onOpenSnapshot: (Snapshot) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { App.instance.database.snapshotDao() }

    var searchQuery by remember { mutableStateOf("") }
    val snapshotsFlow = remember(searchQuery) {
        if (searchQuery.isBlank()) dao.getAllSnapshots() else dao.searchSnapshots(searchQuery)
    }
    val snapshots by snapshotsFlow.collectAsState(initial = emptyList())

    // Batch Management State
    var isBatchMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }

    var snapshotToDelete by remember { mutableStateOf<Snapshot?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

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
                        selectedIds.clear()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "退出多选")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "已选择 ${selectedIds.size} 项",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isAllSelected = snapshots.isNotEmpty() && selectedIds.size == snapshots.size
                    TextButton(onClick = {
                        if (isAllSelected) {
                            selectedIds.clear()
                        } else {
                            selectedIds.clear()
                            selectedIds.addAll(snapshots.map { it.id })
                        }
                    }) {
                        Text(
                            text = if (isAllSelected) "取消全选" else "全选",
                            fontWeight = FontWeight.SemiBold,
                            color = IndigoPrimary
                        )
                    }
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
                    placeholder = { Text("搜索应用名称或包名...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (snapshots.isNotEmpty()) {
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

        if (snapshots.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "暂无快照" else "未找到匹配的快照",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "按音量键或下拉通知栏点击「抓取快照」即可捕获",
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
                    items(snapshots, key = { it.id }) { snapshot ->
                        val isSelected = selectedIds.contains(snapshot.id)
                        SnapshotCard(
                            snapshot = snapshot,
                            isBatchMode = isBatchMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                if (isSelected) selectedIds.remove(snapshot.id) else selectedIds.add(snapshot.id)
                            },
                            onLongClick = {
                                if (!isBatchMode) {
                                    isBatchMode = true
                                    selectedIds.add(snapshot.id)
                                }
                            },
                            onClick = {
                                if (isBatchMode) {
                                    if (isSelected) selectedIds.remove(snapshot.id) else selectedIds.add(snapshot.id)
                                } else {
                                    onOpenSnapshot(snapshot)
                                }
                            },
                            onShare = {
                                coroutineScope.launch {
                                    try {
                                        val zipFile = SnapshotRepository.createExportZip(context, snapshot)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            zipFile
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/zip"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "分享快照压缩包"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDelete = { snapshotToDelete = snapshot }
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
                                onClick = {
                                    val targets = snapshots.filter { it.id in selectedIds }
                                    if (targets.isEmpty()) return@Button
                                    coroutineScope.launch {
                                        try {
                                            Toast.makeText(context, "正在打包 ${targets.size} 个快照...", Toast.LENGTH_SHORT).show()
                                            val zipFile = SnapshotRepository.createBatchExportZip(context, targets)
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                zipFile
                                            )
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/zip"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "批量导出快照压缩包"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("批量导出 (${selectedIds.size})")
                            }

                            OutlinedButton(
                                onClick = { showBatchDeleteDialog = true },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedIds.isNotEmpty()) RoseError else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = if (selectedIds.isNotEmpty()) RoseError else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("批量删除 (${selectedIds.size})", color = if (selectedIds.isNotEmpty()) RoseError else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Single Delete Confirmation Dialog
    if (snapshotToDelete != null) {
        val target = snapshotToDelete!!
        AlertDialog(
            onDismissRequest = { snapshotToDelete = null },
            title = { Text("确认删除快照") },
            text = { Text("是否删除 ${target.appName ?: target.packageName} 的快照？相关的截图与数据文件也将被移除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            SnapshotRepository.deleteSnapshot(context, target)
                            snapshotToDelete = null
                            Toast.makeText(context, "快照已删除", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("删除", color = RoseError)
                }
            },
            dismissButton = {
                TextButton(onClick = { snapshotToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Batch Delete Confirmation Dialog
    if (showBatchDeleteDialog) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("确认批量删除快照") },
            text = { Text("是否删除选中的 $count 个快照？相关的截图与数据文件将被永久移除，此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val targets = snapshots.filter { it.id in selectedIds }
                            SnapshotRepository.deleteSnapshots(context, targets)
                            selectedIds.clear()
                            isBatchMode = false
                            showBatchDeleteDialog = false
                            Toast.makeText(context, "已成功删除 $count 个快照", Toast.LENGTH_SHORT).show()
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SnapshotCard(
    snapshot: Snapshot,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val formattedTime = remember(snapshot.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(snapshot.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) IndigoPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(1.5.dp, IndigoPrimary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Batch Mode Checkbox
            if (isBatchMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = IndigoPrimary,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Screenshot Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 90.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = File(snapshot.screenshotPath),
                    contentDescription = "Snapshot Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snapshot.appName ?: snapshot.packageName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = snapshot.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                if (!snapshot.activity.isNullOrBlank()) {
                    Text(
                        text = snapshot.activity.substringAfterLast('.'),
                        style = MaterialTheme.typography.bodySmall,
                        color = IndigoPrimary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${snapshot.nodeCount} 节点",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // More Menu (Only in non-batch mode)
            if (!isBatchMode) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("审查快照") },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导出 / 分享") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = RoseError) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RoseError) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
