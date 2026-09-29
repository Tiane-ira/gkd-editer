package li.gkd.studio.ui.rule

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import li.gkd.studio.data.model.GkdRule
import li.gkd.studio.rule.OutputScope
import li.gkd.studio.rule.RuleCategory
import li.gkd.studio.rule.RuleGenerator
import li.gkd.studio.rule.RuleOptions
import li.gkd.studio.ui.theme.EmeraldSuccess
import li.gkd.studio.ui.theme.IndigoPrimary

@Composable
fun RulePreviewDialog(
    appId: String,
    appName: String,
    activityId: String?,
    selector: String,
    onSaveRule: (GkdRule) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val validActivity = remember(appId, activityId) {
        RuleGenerator.formatActivityId(appId, activityId)
    }

    var category by remember { mutableStateOf(RuleCategory.SPLASH) }
    var groupKey by remember { mutableIntStateOf(RuleCategory.SPLASH.key) }
    var groupName by remember { mutableStateOf(RuleCategory.SPLASH.title) }
    var groupDesc by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("clickCenter") }
    var includeActivity by remember { mutableStateOf(false) } // Splash ads should NOT lock activity by default
    var fastQuery by remember { mutableStateOf(true) }
    var outputScope by remember { mutableStateOf(OutputScope.APP) }

    var json5Code by remember { mutableStateOf("") }
    var isEditingCode by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }

    fun refreshCode() {
        if (!isEditingCode) {
            val options = RuleOptions(
                appId = appId,
                appName = appName,
                activityId = activityId,
                selector = selector,
                category = category,
                groupKey = groupKey,
                groupName = groupName,
                groupDesc = groupDesc,
                action = action,
                fastQuery = fastQuery,
                includeActivity = includeActivity && validActivity != null,
                outputScope = outputScope
            )
            json5Code = RuleGenerator.generateJson5(options)
        }
    }

    LaunchedEffect(category, groupKey, groupName, groupDesc, action, includeActivity, fastQuery, outputScope) {
        refreshCode()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "生成 GKD 规则",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$appName · $appId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Category Presets (开屏广告, 更新提示, 青少年模式, 局部广告...)
                Text(
                    text = "规则类型预设 (标准 GKD 规范)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RuleCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                groupKey = cat.key
                                groupName = cat.title
                                includeActivity = !cat.isSplash && validActivity != null
                                isEditingCode = false
                            },
                            label = { Text(cat.title, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Output Scope (App, Group, Rule, 订阅)
                Text(
                    text = "导出格式",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = outputScope == OutputScope.APP,
                        onClick = {
                            outputScope = OutputScope.APP
                            isEditingCode = false
                        },
                        label = { Text("App 对象", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = outputScope == OutputScope.GROUP,
                        onClick = {
                            outputScope = OutputScope.GROUP
                            isEditingCode = false
                        },
                        label = { Text("Group 分组", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = outputScope == OutputScope.RULE,
                        onClick = {
                            outputScope = OutputScope.RULE
                            isEditingCode = false
                        },
                        label = { Text("单条 Rule", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = outputScope == OutputScope.SUBSCRIPTION,
                        onClick = {
                            outputScope = OutputScope.SUBSCRIPTION
                            isEditingCode = false
                        },
                        label = { Text("完整订阅", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Name & Desc inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = {
                            groupName = it
                            isEditingCode = false
                        },
                        label = { Text("分组名称") },
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = groupKey.toString(),
                        onValueChange = {
                            groupKey = it.toIntOrNull() ?: 0
                            isEditingCode = false
                        },
                        label = { Text("Key") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4. Options Checkboxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = fastQuery,
                            onCheckedChange = {
                                fastQuery = it
                                isEditingCode = false
                            }
                        )
                        Text("fastQuery 快速查询", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includeActivity && validActivity != null,
                            enabled = validActivity != null,
                            onCheckedChange = {
                                includeActivity = it
                                isEditingCode = false
                            }
                        )
                        Text(
                            text = if (validActivity != null) "限定 Activity" else "无有效 Activity",
                            fontSize = 12.sp,
                            color = if (validActivity != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Activity notice
                if (validActivity != null) {
                    Text(
                        text = "活动页面: $validActivity" + if (category.isSplash) " (开屏广告建议不限制以适配启动动画)" else "",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else {
                    Text(
                        text = "当前快照未捕获到具体 Activity，默认全局应用范围内生效",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. JSON5 Code Viewer Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GKD JSON5 代码",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(json5Code))
                            isCopied = true
                            Toast.makeText(context, "规则代码已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = if (isCopied) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, json5Code)
                                putExtra(Intent.EXTRA_TITLE, "GKD Rule - $appName")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "分享 GKD 规则"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }

                // 6. JSON5 Code Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D1117))
                        .padding(8.dp)
                ) {
                    OutlinedTextField(
                        value = json5Code,
                        onValueChange = {
                            json5Code = it
                            isEditingCode = true
                        },
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF58A6FF),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .horizontalScroll(rememberScrollState())
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 7. GKD Usage Guide
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Tips",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GKD 导入说明：\n1. 点击上方复制按钮\n2. 打开 GKD ->【规则】->【本地规则/自定义规则】直接粘贴保存即可生效！",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rule = GkdRule(
                        appId = appId,
                        appName = appName,
                        groupKey = groupKey,
                        groupName = groupName,
                        groupDesc = groupDesc,
                        activityIds = if (includeActivity && validActivity != null) activityId else null,
                        action = action,
                        selector = selector,
                        fastQuery = fastQuery,
                        matchRoot = false,
                        ruleJson5 = json5Code
                    )
                    onSaveRule(rule)
                    Toast.makeText(context, "规则已保存到本地管理", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("保存到规则管理")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
