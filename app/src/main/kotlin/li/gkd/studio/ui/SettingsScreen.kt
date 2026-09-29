package li.gkd.studio.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import li.gkd.studio.App
import li.gkd.studio.service.StudioAccessibilityService
import li.gkd.studio.service.VolumeTriggerHelper
import li.gkd.studio.ui.theme.AmberWarning
import li.gkd.studio.ui.theme.EmeraldSuccess
import li.gkd.studio.ui.theme.IndigoPrimary

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val isRunning by StudioAccessibilityService.isServiceRunning.collectAsState()
    val currentPkg by StudioAccessibilityService.currentPackageName.collectAsState()
    val currentAct by StudioAccessibilityService.currentActivityName.collectAsState()
    val currentApp by StudioAccessibilityService.currentAppName.collectAsState()
    val isVolumeTriggerOn by VolumeTriggerHelper.isVolumeTriggerEnabled.collectAsState()

    LaunchedEffect(Unit) {
        VolumeTriggerHelper.init(context)
    }

    val snapshotCount by remember { App.instance.database.snapshotDao().count() }.collectAsState(initial = 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Service Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRunning) EmeraldSuccess.copy(alpha = 0.12f) else AmberWarning.copy(alpha = 0.12f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isRunning) EmeraldSuccess.copy(alpha = 0.4f) else AmberWarning.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isRunning) EmeraldSuccess else AmberWarning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "无障碍服务运行中" else "无障碍服务未开启",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isRunning) EmeraldSuccess else AmberWarning
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) MaterialTheme.colorScheme.surfaceVariant else IndigoPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isRunning) "设置" else "去开启",
                            color = if (isRunning) MaterialTheme.colorScheme.onSurface else Color.White,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isRunning) {
                        "后台服务运行正常，常驻通知已就绪。在任意前台应用下拉通知栏即可触发快照抓取。"
                    } else {
                        "请在系统无障碍设置中找到「GKD Rule Studio 无障碍快照服务」并开启。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isRunning && !currentPkg.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "当前检测前台应用:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${currentApp ?: currentPkg} ($currentPkg)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (!currentAct.isNullOrBlank()) {
                                Text(
                                    text = "Activity: ${currentAct?.substringAfterLast('.')}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Capture Triggers Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "快照触发方式",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Trigger 1: Notification Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IndigoPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "通知栏快捷抓取",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "常驻通知栏，下拉点击「抓取快照」自动收起并抓取前台",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "默认开启",
                        fontSize = 11.sp,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Medium
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Trigger 2: Volume Button Change
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isVolumeTriggerOn) IndigoPrimary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isVolumeTriggerOn) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "按音量键触发抓取",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "在前台任意应用按音量加/减键自动抓取，无需下拉通知栏",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isVolumeTriggerOn,
                        onCheckedChange = {
                            VolumeTriggerHelper.setEnabled(context, it)
                            val msg = if (it) "已开启音量键抓取快照" else "已关闭音量键抓取快照"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = IndigoPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "提示: 抓取内置 2 秒冷却时间防止重复触发；锁屏状态下自动忽略。",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        // Quick Capture Test Action
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "快照抓取测试",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "点击按钮将在当前页面直接触发一次快照抓取测试（平时推荐使用音量键或通知栏按钮）：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val service = StudioAccessibilityService.instance
                        if (service != null) {
                            service.triggerCapture()
                        } else {
                            Toast.makeText(context, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("立即抓取快照")
                }
            }
        }

        // Usage Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "使用流程指南",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                InstructionStep(1, "开启服务", "启动本应用并授权「无障碍服务」")
                InstructionStep(2, "打开目标 APP", "切换到需要跳过广告或关闭弹窗的页面")
                InstructionStep(3, "触发抓取快照", "按手机音量键，或下拉通知栏点击常驻通知的「抓取快照」")
                InstructionStep(4, "审查与生成规则", "返回 Rule Studio，点击节点即可生成 Selector 并测试匹配，一键生成 GKD 规则")
            }
        }

        // About Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = IndigoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "关于 GKD Rule Studio",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "手机端 GKD 快照抓取、审查和规则生成工具。\n数据结构与选择器引擎严格对齐 GKD 与 GKD Inspect 源码标准。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "已存储快照: $snapshotCount 个",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IndigoPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InstructionStep(step: Int, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IndigoPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = IndigoPrimary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
