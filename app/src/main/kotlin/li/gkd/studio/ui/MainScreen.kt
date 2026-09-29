package li.gkd.studio.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import li.gkd.studio.data.model.Snapshot
import li.gkd.studio.ui.rule.RuleManagerScreen
import li.gkd.studio.ui.snapshot.SnapshotInspectorScreen
import li.gkd.studio.ui.snapshot.SnapshotManagerScreen
import li.gkd.studio.ui.theme.IndigoPrimary

enum class MainTab(val label: String) {
    SNAPSHOTS("快照管理"),
    RULES("规则管理"),
    SETTINGS("设置")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentTab by remember { mutableStateOf(MainTab.SNAPSHOTS) }
    var inspectingSnapshot by remember { mutableStateOf<Snapshot?>(null) }

    if (inspectingSnapshot != null) {
        SnapshotInspectorScreen(
            snapshot = inspectingSnapshot!!,
            onBack = { inspectingSnapshot = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "GKD Rule Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.SNAPSHOTS,
                        onClick = { currentTab = MainTab.SNAPSHOTS },
                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                        label = { Text(MainTab.SNAPSHOTS.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            indicatorColor = IndigoPrimary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.RULES,
                        onClick = { currentTab = MainTab.RULES },
                        icon = { Icon(Icons.Default.Rule, contentDescription = null) },
                        label = { Text(MainTab.RULES.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            indicatorColor = IndigoPrimary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { currentTab = MainTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(MainTab.SETTINGS.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            indicatorColor = IndigoPrimary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    MainTab.SNAPSHOTS -> SnapshotManagerScreen(
                        onOpenSnapshot = { inspectingSnapshot = it }
                    )
                    MainTab.RULES -> RuleManagerScreen()
                    MainTab.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}
