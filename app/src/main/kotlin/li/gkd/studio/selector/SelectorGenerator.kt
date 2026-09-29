package li.gkd.studio.selector

import li.gkd.studio.data.model.SnapshotNode

data class SelectorCandidate(
    val title: String,
    val selector: String,
    val matchCount: Int,
    val isUnique: Boolean = matchCount == 1,
    val description: String = ""
)

object SelectorGenerator {

    private fun escapeStr(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun getShortName(fullName: String?): String {
        if (fullName == null) return "View"
        val lstIndex = fullName.lastIndexOf('.')
        return if (lstIndex == -1) fullName else fullName.substring(lstIndex + 1)
    }

    private fun getConnectOperator(operator: String, index: Int): String {
        return operator + if (index == 1) "" else index.toString()
    }

    fun getInspectHierarchicalSelector(
        curNode: SnapshotNode,
        isFirst: Boolean = true,
        lastIndex: Int = 1
    ): String {
        val parent = curNode.parent
        if (parent == null) {
            return if (isFirst) "[parent=null]" else "${getConnectOperator("<", lastIndex)} [parent=null]"
        }
        if (curNode.idQf == true) {
            val key = if (curNode.attr.vid != null) "vid" else "id"
            val value = curNode.attr.vid ?: curNode.attr.id ?: ""
            return if (isFirst) {
                "[$key=\"${escapeStr(value)}\"]"
            } else {
                "${getConnectOperator("<", lastIndex)} [$key=\"${escapeStr(value)}\"]"
            }
        }
        val shortName = getShortName(curNode.attr.name)
        return if (isFirst) {
            "@$shortName ${getInspectHierarchicalSelector(parent, false, curNode.attr.index + 1)}"
        } else {
            "${getConnectOperator("<", lastIndex)} $shortName ${getInspectHierarchicalSelector(parent, false, curNode.attr.index + 1)}"
        }
    }

    fun generateCandidates(node: SnapshotNode, allNodes: List<SnapshotNode>): List<SelectorCandidate> {
        val rawCandidates = mutableListOf<Pair<String, String>>() // (Title, Selector)

        val text = node.attr.text?.trim()
        val desc = node.attr.desc?.trim()
        val vid = node.attr.vid
        val fullId = node.attr.id
        val shortClass = getShortName(node.attr.name)

        // 1. Text exact and Splash skip pattern
        if (!text.isNullOrEmpty()) {
            val escapedText = escapeStr(text)
            if (text.contains("跳过") || text.contains("skip", ignoreCase = true)) {
                rawCandidates.add("开屏跳过 (GKD规范)" to "[text*=\"跳过\"][text.length<=10]")
                rawCandidates.add("开屏跳过+可见" to "[text*=\"跳过\"][text.length<=10][visibleToUser=true]")
            }
            rawCandidates.add("文本匹配" to "[text=\"$escapedText\"]")
            if (text.length >= 4) {
                val subText = escapeStr(text.take(4))
                rawCandidates.add("文本包含" to "[text*=\"$subText\"]")
            }
        }

        // 2. Desc exact and close pattern
        if (!desc.isNullOrEmpty() && desc != text) {
            val escapedDesc = escapeStr(desc)
            if (desc.contains("跳过") || desc.contains("skip", ignoreCase = true)) {
                rawCandidates.add("开屏跳过 (描述)" to "[desc*=\"跳过\"][desc.length<=10]")
            }
            if (desc.contains("关闭") || desc.contains("close", ignoreCase = true)) {
                rawCandidates.add("关闭按钮 (描述)" to "[desc*=\"关闭\"][clickable=true]")
            }
            rawCandidates.add("描述匹配" to "[desc=\"$escapedDesc\"]")
            if (desc.length >= 4) {
                val subDesc = escapeStr(desc.take(4))
                rawCandidates.add("描述包含" to "[desc*=\"$subDesc\"]")
            }
        }

        // 3. Text or Desc
        if (!text.isNullOrEmpty() && text == desc) {
            val escapedText = escapeStr(text)
            rawCandidates.add("文本或描述" to "[text=\"$escapedText\" || desc=\"$escapedText\"]")
        }

        // 4. View ID (short)
        if (!vid.isNullOrEmpty()) {
            if (vid.contains("skip", ignoreCase = true)) {
                rawCandidates.add("跳过ID匹配" to "[vid*=\"skip\"]")
            }
            if (vid.contains("close", ignoreCase = true)) {
                rawCandidates.add("关闭ID匹配" to "[vid*=\"close\"]")
            }
            rawCandidates.add("View ID" to "[vid=\"${escapeStr(vid)}\"]")
        }

        // 5. Full Resource ID
        if (!fullId.isNullOrEmpty() && fullId != vid) {
            rawCandidates.add("完整 Resource ID" to "[id=\"${escapeStr(fullId)}\"]")
        }

        // 6. VID + Text
        if (!vid.isNullOrEmpty() && !text.isNullOrEmpty()) {
            rawCandidates.add("ID + 文本" to "[vid=\"${escapeStr(vid)}\"][text=\"${escapeStr(text)}\"]")
        }

        // 7. Full ID + Text
        if (!fullId.isNullOrEmpty() && !text.isNullOrEmpty()) {
            rawCandidates.add("Resource ID + 文本" to "[id=\"${escapeStr(fullId)}\"][text=\"${escapeStr(text)}\"]")
        }

        // 8. Class + Text
        if (!text.isNullOrEmpty() && shortClass != "View") {
            rawCandidates.add("类名 + 文本" to "${shortClass}[text=\"${escapeStr(text)}\"]")
        }

        // 9. Class + VID
        if (!vid.isNullOrEmpty() && shortClass != "View") {
            rawCandidates.add("类名 + ID" to "${shortClass}[vid=\"${escapeStr(vid)}\"]")
        }

        // 10. Clickable + Text
        if (node.attr.clickable && !text.isNullOrEmpty()) {
            rawCandidates.add("可点击 + 文本" to "[clickable=true][text=\"${escapeStr(text)}\"]")
        }

        // 11. Parent clickable container target
        val parent = node.parent
        if (parent != null && parent.attr.clickable && !node.attr.clickable && !text.isNullOrEmpty()) {
            val parentClass = getShortName(parent.attr.name)
            rawCandidates.add("点击父容器" to "@$parentClass[clickable=true] > [text=\"${escapeStr(text)}\"]")
        }

        // 12. Inspect Hierarchical Selector
        runCatching {
            val inspectSel = getInspectHierarchicalSelector(node).trim()
            if (inspectSel.isNotBlank()) {
                rawCandidates.add("层次关系 (Inspect)" to inspectSel)
            }
        }

        // Test each candidate against allNodes
        val results = mutableListOf<SelectorCandidate>()
        val seenSelectors = mutableSetOf<String>()

        for ((title, sel) in rawCandidates) {
            if (!seenSelectors.add(sel)) continue

            val testRes = SelectorTester.test(sel, allNodes)
            val matchCount = if (testRes is SelectorTestResult.Success) testRes.matchingNodes.size else 0

            results.add(
                SelectorCandidate(
                    title = title,
                    selector = sel,
                    matchCount = matchCount,
                    isUnique = matchCount == 1,
                    description = if (matchCount == 1) "唯一匹配 (推荐)" else if (matchCount == 0) "无匹配" else "匹配 $matchCount 个节点"
                )
            )
        }

        // Sort: unique matches first, then by matchCount ascending, then by selector length
        return results.sortedWith(
            compareByDescending<SelectorCandidate> { it.isUnique }
                .thenBy { if (it.matchCount == 0) 9999 else it.matchCount }
                .thenBy { it.selector.length }
        )
    }
}
