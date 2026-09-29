package li.gkd.studio.rule

import li.gkd.studio.data.model.GkdRule

enum class RuleCategory(
    val key: Int,
    val title: String,
    val isSplash: Boolean
) {
    SPLASH(0, "开屏广告", true),
    UPDATE(2, "更新提示", false),
    YOUTH(1, "青少年模式", false),
    PARTIAL_AD(6, "局部广告", false),
    FULLSCREEN_AD(7, "全屏广告", false),
    CUSTOM(10, "自定义", false);

    companion object {
        fun fromTitle(title: String): RuleCategory {
            return entries.firstOrNull { it.title == title } ?: CUSTOM
        }
    }
}

enum class OutputScope {
    APP,
    GROUP,
    RULE,
    SUBSCRIPTION
}

data class RuleOptions(
    val appId: String,
    val appName: String,
    val activityId: String? = null,
    val selector: String,
    val category: RuleCategory = RuleCategory.SPLASH,
    val groupKey: Int = category.key,
    val groupName: String = category.title,
    val groupDesc: String = "",
    val action: String = "clickCenter",
    val fastQuery: Boolean = true,
    val matchRoot: Boolean = false,
    val matchTime: Long? = if (category.isSplash || category == RuleCategory.UPDATE || category == RuleCategory.YOUTH) 10000L else null,
    val actionMaximum: Int? = if (category.isSplash || category == RuleCategory.UPDATE || category == RuleCategory.YOUTH) 1 else null,
    val resetMatch: String? = if (category.isSplash || category == RuleCategory.UPDATE || category == RuleCategory.YOUTH) "app" else null,
    val includeActivity: Boolean = !category.isSplash,
    val outputScope: OutputScope = OutputScope.APP
)

object RuleGenerator {

    /**
     * Checks if a class name is a potential Android Activity.
     * Rejects common view, layout, popup, and window classes produced by AccessibilityEvent.
     */
    fun isPotentialActivityName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val lower = name.lowercase()
        if (lower.startsWith("android.widget.") ||
            lower.startsWith("android.view.") ||
            lower.endsWith("layout") ||
            lower.endsWith("view") ||
            lower.endsWith("window") ||
            lower.endsWith("dialog") ||
            lower.endsWith("popupwindow") ||
            lower.contains("toast") ||
            lower.contains("decorview") ||
            name.contains("$")
        ) {
            return false
        }
        return name.contains(".")
    }

    private fun escapeJson5String(s: String): String {
        return s.replace("\\", "\\\\").replace("'", "\\'")
    }

    /**
     * Formats activity ID for GKD.
     * If it starts with appId and has dot, turns into relative dot notation: .MainActivity
     * If it does not start with appId, keeps full package if valid.
     * Returns null if invalid or view class.
     */
    fun formatActivityId(appId: String, activityId: String?): String? {
        if (activityId.isNullOrBlank()) return null
        if (!isPotentialActivityName(activityId)) return null
        val trimmed = activityId.trim()
        return if (trimmed.startsWith(appId) && trimmed.length > appId.length && trimmed[appId.length] == '.') {
            trimmed.substring(appId.length)
        } else {
            trimmed
        }
    }

    fun generateJson5(options: RuleOptions): String {
        val shortAct = if (options.includeActivity) {
            formatActivityId(options.appId, options.activityId)
        } else {
            null
        }

        // 1. Build Rule-level properties
        val rulePropertyLines = mutableListOf<String>()
        if (options.action != "clickCenter" && options.action != "clickNode" && options.action.isNotBlank()) {
            rulePropertyLines.add("action: '${escapeJson5String(options.action)}'")
        }
        if (!shortAct.isNullOrBlank()) {
            rulePropertyLines.add("activityIds: '${escapeJson5String(shortAct)}'")
        }
        rulePropertyLines.add("matches: '${escapeJson5String(options.selector)}'")

        // 2. Build Group-level properties (matching ganlin_gkd.json5)
        val groupPropertyLines = mutableListOf<String>()
        groupPropertyLines.add("key: ${options.groupKey}")
        groupPropertyLines.add("name: '${escapeJson5String(options.groupName)}'")
        if (options.groupDesc.isNotBlank()) {
            groupPropertyLines.add("desc: '${escapeJson5String(options.groupDesc)}'")
        }
        if (options.matchTime != null && options.matchTime > 0) {
            groupPropertyLines.add("matchTime: ${options.matchTime}")
        }
        if (options.actionMaximum != null && options.actionMaximum > 0) {
            groupPropertyLines.add("actionMaximum: ${options.actionMaximum}")
        }
        if (!options.resetMatch.isNullOrBlank()) {
            groupPropertyLines.add("resetMatch: '${escapeJson5String(options.resetMatch)}'")
        }
        if (options.fastQuery) {
            groupPropertyLines.add("fastQuery: true")
        }
        if (options.matchRoot) {
            groupPropertyLines.add("matchRoot: true")
        }

        val cleanAppName = options.appName.ifBlank { options.appId }

        when (options.outputScope) {
            OutputScope.RULE -> {
                val sb = StringBuilder()
                sb.append("{\n")
                if (options.matchTime != null && options.matchTime > 0) {
                    sb.append("  matchTime: ${options.matchTime},\n")
                }
                if (options.actionMaximum != null && options.actionMaximum > 0) {
                    sb.append("  actionMaximum: ${options.actionMaximum},\n")
                }
                if (!options.resetMatch.isNullOrBlank()) {
                    sb.append("  resetMatch: '${escapeJson5String(options.resetMatch)}',\n")
                }
                if (options.fastQuery) {
                    sb.append("  fastQuery: true,\n")
                }
                for (prop in rulePropertyLines) {
                    sb.append("  $prop,\n")
                }
                sb.append("}")
                return sb.toString()
            }

            OutputScope.GROUP -> {
                val sb = StringBuilder()
                sb.append("{\n")
                for (prop in groupPropertyLines) {
                    sb.append("  $prop,\n")
                }
                sb.append("  rules: [\n")
                sb.append("    {\n")
                for (prop in rulePropertyLines) {
                    sb.append("      $prop,\n")
                }
                sb.append("    },\n")
                sb.append("  ],\n")
                sb.append("}")
                return sb.toString()
            }

            OutputScope.APP -> {
                val sb = StringBuilder()
                sb.append("{\n")
                sb.append("  id: '${escapeJson5String(options.appId)}',\n")
                sb.append("  name: '${escapeJson5String(cleanAppName)}',\n")
                sb.append("  groups: [\n")
                sb.append("    {\n")
                for (prop in groupPropertyLines) {
                    sb.append("      $prop,\n")
                }
                sb.append("      rules: [\n")
                sb.append("        {\n")
                for (prop in rulePropertyLines) {
                    sb.append("          $prop,\n")
                }
                sb.append("        },\n")
                sb.append("      ],\n")
                sb.append("    },\n")
                sb.append("  ],\n")
                sb.append("}")
                return sb.toString()
            }

            OutputScope.SUBSCRIPTION -> {
                val sb = StringBuilder()
                sb.append("{\n")
                sb.append("  id: 10001,\n")
                sb.append("  name: 'GKD 本地规则订阅',\n")
                sb.append("  version: 1,\n")
                sb.append("  author: 'GKD Rule Studio',\n")
                sb.append("  apps: [\n")
                sb.append("    {\n")
                sb.append("      id: '${escapeJson5String(options.appId)}',\n")
                sb.append("      name: '${escapeJson5String(cleanAppName)}',\n")
                sb.append("      groups: [\n")
                sb.append("        {\n")
                for (prop in groupPropertyLines) {
                    sb.append("          $prop,\n")
                }
                sb.append("          rules: [\n")
                sb.append("            {\n")
                for (prop in rulePropertyLines) {
                    sb.append("              $prop,\n")
                }
                sb.append("            },\n")
                sb.append("          ],\n")
                sb.append("        },\n")
                sb.append("      ],\n")
                sb.append("    },\n")
                sb.append("  ],\n")
                sb.append("}")
                return sb.toString()
            }
        }
    }

    fun generateBatchRulesJson5(
        rules: List<GkdRule>,
        title: String = "GKD 本地规则订阅"
    ): String {
        val appGroups = rules.groupBy { it.appId to it.appName }
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  id: 10001,\n")
        sb.append("  name: '${escapeJson5String(title)}',\n")
        sb.append("  version: 1,\n")
        sb.append("  author: 'GKD Rule Studio',\n")
        sb.append("  apps: [\n")

        for ((appKey, appRules) in appGroups) {
            val (appId, appName) = appKey
            val cleanName = appName.ifBlank { appId }
            sb.append("    {\n")
            sb.append("      id: '${escapeJson5String(appId)}',\n")
            sb.append("      name: '${escapeJson5String(cleanName)}',\n")
            sb.append("      groups: [\n")

            val ruleGroups = appRules.groupBy { it.groupName to it.groupKey }
            for ((groupKeyInfo, groupRuleList) in ruleGroups) {
                val (groupName, groupKey) = groupKeyInfo
                val firstRule = groupRuleList.first()
                val isSplash = groupName.contains("开屏")
                sb.append("        {\n")
                sb.append("          key: $groupKey,\n")
                sb.append("          name: '${escapeJson5String(groupName)}',\n")
                if (firstRule.groupDesc.isNotBlank()) {
                    sb.append("          desc: '${escapeJson5String(firstRule.groupDesc)}',\n")
                }
                if (isSplash) {
                    sb.append("          matchTime: 10000,\n")
                    sb.append("          actionMaximum: 1,\n")
                    sb.append("          resetMatch: 'app',\n")
                }
                if (firstRule.fastQuery) {
                    sb.append("          fastQuery: true,\n")
                }
                if (firstRule.matchRoot) {
                    sb.append("          matchRoot: true,\n")
                }
                sb.append("          rules: [\n")
                for (r in groupRuleList) {
                    sb.append("            {\n")
                    if (r.action != "clickCenter" && r.action != "clickNode" && r.action.isNotBlank()) {
                        sb.append("              action: '${escapeJson5String(r.action)}',\n")
                    }
                    val shortAct = if (!isSplash) formatActivityId(r.appId, r.activityIds) else null
                    if (!shortAct.isNullOrBlank()) {
                        sb.append("              activityIds: '${escapeJson5String(shortAct)}',\n")
                    }
                    sb.append("              matches: '${escapeJson5String(r.selector)}',\n")
                    sb.append("            },\n")
                }
                sb.append("          ],\n")
                sb.append("        },\n")
            }
            sb.append("      ],\n")
            sb.append("    },\n")
        }
        sb.append("  ],\n")
        sb.append("}")
        return sb.toString()
    }

    fun generateBatchAppRulesJson5(
        appId: String,
        appName: String,
        rules: List<GkdRule>
    ): String {
        val cleanName = appName.ifBlank { appId }
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  id: '${escapeJson5String(appId)}',\n")
        sb.append("  name: '${escapeJson5String(cleanName)}',\n")
        sb.append("  groups: [\n")
        val ruleGroups = rules.groupBy { it.groupName to it.groupKey }
        for ((groupKeyInfo, groupRuleList) in ruleGroups) {
            val (groupName, groupKey) = groupKeyInfo
            val firstRule = groupRuleList.first()
            val isSplash = groupName.contains("开屏")
            sb.append("    {\n")
            sb.append("      key: $groupKey,\n")
            sb.append("      name: '${escapeJson5String(groupName)}',\n")
            if (firstRule.groupDesc.isNotBlank()) {
                sb.append("      desc: '${escapeJson5String(firstRule.groupDesc)}',\n")
            }
            if (isSplash) {
                sb.append("      matchTime: 10000,\n")
                sb.append("      actionMaximum: 1,\n")
                sb.append("      resetMatch: 'app',\n")
            }
            if (firstRule.fastQuery) {
                sb.append("      fastQuery: true,\n")
            }
            if (firstRule.matchRoot) {
                sb.append("      matchRoot: true,\n")
            }
            sb.append("      rules: [\n")
            for (r in groupRuleList) {
                sb.append("        {\n")
                if (r.action != "clickCenter" && r.action != "clickNode" && r.action.isNotBlank()) {
                    sb.append("          action: '${escapeJson5String(r.action)}',\n")
                }
                val shortAct = if (!isSplash) formatActivityId(r.appId, r.activityIds) else null
                if (!shortAct.isNullOrBlank()) {
                    sb.append("          activityIds: '${escapeJson5String(shortAct)}',\n")
                }
                sb.append("          matches: '${escapeJson5String(r.selector)}',\n")
                sb.append("        },\n")
            }
            sb.append("      ],\n")
            sb.append("    },\n")
        }
        sb.append("  ],\n")
        sb.append("}")
        return sb.toString()
    }
}
