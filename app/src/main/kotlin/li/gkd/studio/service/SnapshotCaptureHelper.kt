package li.gkd.studio.service

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import li.gkd.studio.data.model.SnapshotAttr
import li.gkd.studio.data.model.SnapshotNode

object SnapshotCaptureHelper {

    private const val MAX_CHILD_SIZE = 100
    private const val MAX_KEEP_SIZE = 4000

    private class TempNodeData(
        val node: AccessibilityNodeInfo,
        val parent: TempNodeData?,
        val index: Int,
        val depth: Int
    ) {
        var id: Int = 0
        var children: List<TempNodeData> = emptyList()
        var idQfInit = false
        var idQf: Boolean? = null
            set(value) {
                field = value
                idQfInit = true
            }
        var textQfInit = false
        var textQf: Boolean? = null
            set(value) {
                field = value
                textQfInit = true
            }

        fun toAttr(appId: String): SnapshotAttr {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            val fullId = node.viewIdResourceName
            val idPrefix = "$appId:id/"
            val vid = if (fullId != null && fullId.startsWith(idPrefix)) {
                fullId.substring(idPrefix.length)
            } else {
                null
            }

            return SnapshotAttr(
                id = fullId,
                vid = vid,
                name = node.className?.toString(),
                text = node.text?.toString(),
                desc = node.contentDescription?.toString(),
                clickable = node.isClickable,
                focusable = node.isFocusable,
                checkable = node.isCheckable,
                checked = node.isChecked,
                editable = node.isEditable,
                longClickable = node.isLongClickable,
                visibleToUser = node.isVisibleToUser,
                left = rect.left,
                top = rect.top,
                right = rect.right,
                bottom = rect.bottom,
                width = rect.width(),
                height = rect.height(),
                childCount = node.childCount,
                index = index,
                depth = depth
            )
        }
    }

    fun info2nodeList(root: AccessibilityNodeInfo?): List<SnapshotNode> {
        if (root == null) return emptyList()
        val appId = root.packageName?.toString() ?: ""

        val nodes = mutableListOf<TempNodeData>()
        val stack = mutableListOf<TempNodeData>()
        var times = 0
        stack.add(TempNodeData(root, null, 0, 0))

        while (stack.isNotEmpty()) {
            times++
            val current = stack.removeAt(stack.lastIndex)
            current.id = times - 1

            val childCount = current.node.childCount.coerceAtMost(MAX_CHILD_SIZE)
            val children = mutableListOf<TempNodeData>()
            for (i in 0 until childCount) {
                val child = current.node.getChild(i) ?: continue
                children.add(TempNodeData(child, current, i, current.depth + 1))
            }
            current.children = children
            nodes.add(current)

            for (i in children.indices.reversed()) {
                stack.add(children[i])
            }

            if (times > MAX_KEEP_SIZE) {
                break
            }
        }

        // Fast query checks (idQf / textQf) as per GKD algorithm
        val idQfCache = mutableMapOf<String, List<AccessibilityNodeInfo>>()
        val textQfCache = mutableMapOf<String, List<AccessibilityNodeInfo>>()

        fun updateQf(n: TempNodeData) {
            val fullId = n.node.viewIdResourceName
            if (!n.idQfInit && !fullId.isNullOrEmpty()) {
                val matches = idQfCache.getOrPut(fullId) {
                    root.findAccessibilityNodeInfosByViewId(fullId) ?: emptyList()
                }
                n.idQf = matches.any { it == n.node }
            }

            val text = n.node.text?.toString()
            if (!n.textQfInit && !text.isNullOrEmpty()) {
                val matches = textQfCache.getOrPut(text) {
                    root.findAccessibilityNodeInfosByText(text) ?: emptyList()
                }
                n.textQf = matches.any { it == n.node }
            }

            n.idQfInit = true
            n.textQfInit = true
        }

        for (i in nodes.indices.reversed()) {
            val n = nodes[i]
            if (n.children.isEmpty()) {
                updateQf(n)
            }
        }
        for (i in nodes.indices.reversed()) {
            val n = nodes[i]
            if (n.children.isNotEmpty()) {
                updateQf(n)
            }
        }

        return nodes.map { n ->
            SnapshotNode(
                id = n.id,
                pid = n.parent?.id ?: -1,
                idQf = n.idQf,
                textQf = n.textQf,
                attr = n.toAttr(appId)
            )
        }
    }
}
