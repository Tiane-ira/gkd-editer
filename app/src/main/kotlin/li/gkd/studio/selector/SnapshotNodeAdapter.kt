package li.gkd.studio.selector

import li.gkd.selector.NodeAdapter
import li.gkd.studio.data.model.SnapshotNode

object SnapshotNodeAdapter : NodeAdapter<SnapshotNode>() {
    override fun getAttr(target: Any, name: String): Any? {
        if (target !is SnapshotNode) return null
        return when (name) {
            "id" -> target.attr.id
            "vid" -> target.attr.vid
            "name" -> target.attr.name
            "text" -> target.attr.text
            "desc" -> target.attr.desc
            "clickable" -> target.attr.clickable
            "focusable" -> target.attr.focusable
            "checkable" -> target.attr.checkable
            "checked" -> target.attr.checked
            "editable" -> target.attr.editable
            "longClickable" -> target.attr.longClickable
            "visibleToUser" -> target.attr.visibleToUser
            "left" -> target.attr.left
            "top" -> target.attr.top
            "right" -> target.attr.right
            "bottom" -> target.attr.bottom
            "width" -> target.attr.width
            "height" -> target.attr.height
            "index" -> target.attr.index
            "depth" -> target.attr.depth
            "childCount" -> target.attr.childCount
            "parent" -> target.parent
            "_id" -> target.id
            "_pid" -> target.pid
            else -> null
        }
    }

    override fun getName(node: SnapshotNode): String? = node.attr.name

    override fun getChildCount(node: SnapshotNode): Int = node.children.size

    override fun getChild(node: SnapshotNode, index: Int): SnapshotNode? = node.children.getOrNull(index)

    override fun getParent(node: SnapshotNode): SnapshotNode? = node.parent

    override fun getNodeKey(node: SnapshotNode): Any = node.id
}
