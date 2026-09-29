package li.gkd.studio.ui.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import li.gkd.studio.data.model.SnapshotNode
import li.gkd.studio.selector.SelectorCandidate
import li.gkd.studio.selector.SelectorGenerator
import li.gkd.studio.selector.SelectorTestResult
import li.gkd.studio.selector.SelectorTester
import li.gkd.studio.ui.theme.AmberWarning
import li.gkd.studio.ui.theme.EmeraldSuccess
import li.gkd.studio.ui.theme.IndigoPrimary
import li.gkd.studio.ui.theme.RoseError

@Composable
fun SelectorTestSheet(
    node: SnapshotNode,
    allNodes: List<SnapshotNode>,
    onMatchingNodesChanged: (List<SnapshotNode>) -> Unit,
    onGenerateRule: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val candidates = remember(node) { SelectorGenerator.generateCandidates(node, allNodes) }

    var currentSelector by remember {
        mutableStateOf(candidates.firstOrNull()?.selector ?: "")
    }

    var testResult by remember {
        mutableStateOf<SelectorTestResult>(SelectorTestResult.Success(emptyList()))
    }

    fun runTest(sel: String) {
        val res = SelectorTester.test(sel, allNodes)
        testResult = res
        if (res is SelectorTestResult.Success) {
            onMatchingNodesChanged(res.matchingNodes)
        } else {
            onMatchingNodesChanged(emptyList())
        }
    }

    LaunchedEffect(currentSelector) {
        runTest(currentSelector)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selector 生成与测试",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selector Editor
            OutlinedTextField(
                value = currentSelector,
                onValueChange = { currentSelector = it },
                label = { Text("GKD Selector") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(currentSelector))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Live Test Status Card
            when (val res = testResult) {
                is SelectorTestResult.Success -> {
                    val count = res.matchingNodes.size
                    val (bgColor, textColor, icon) = when {
                        count == 1 -> Triple(EmeraldSuccess.copy(alpha = 0.15f), EmeraldSuccess, Icons.Default.CheckCircle)
                        count > 1 -> Triple(AmberWarning.copy(alpha = 0.15f), AmberWarning, Icons.Default.Warning)
                        else -> Triple(Color(0xFF334155).copy(alpha = 0.3f), Color.Gray, Icons.Default.PlayArrow)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                count == 1 -> "匹配 1 个节点 (唯一目标，完美适用)"
                                count > 1 -> "⚠ 匹配 $count 个节点 (建议添加属性以唯一匹配)"
                                else -> "未匹配到任何节点"
                            },
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                is SelectorTestResult.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(RoseError.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = RoseError, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = res.message,
                            color = RoseError,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action button: Generate GKD Rule
            Button(
                onClick = { onGenerateRule(currentSelector) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                enabled = testResult is SelectorTestResult.Success && (testResult as SelectorTestResult.Success).matchingNodes.isNotEmpty()
            ) {
                Text("生成 GKD 规则", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "候选 Selector (点击应用并测试)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Candidate List
            LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                items(candidates) { candidate ->
                    CandidateCard(
                        candidate = candidate,
                        isSelected = candidate.selector == currentSelector,
                        onClick = {
                            currentSelector = candidate.selector
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun CandidateCard(
    candidate: SelectorCandidate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) IndigoPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = candidate.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = candidate.description,
                        fontSize = 11.sp,
                        color = if (candidate.isUnique) EmeraldSuccess else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = candidate.selector,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (candidate.isUnique) EmeraldSuccess.copy(alpha = 0.2f)
                        else if (candidate.matchCount > 0) AmberWarning.copy(alpha = 0.2f)
                        else Color.Gray.copy(alpha = 0.2f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (candidate.matchCount == 1) "1 个节点" else "${candidate.matchCount} 个",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (candidate.isUnique) EmeraldSuccess else if (candidate.matchCount > 0) AmberWarning else Color.Gray
                )
            }
        }
    }
}
