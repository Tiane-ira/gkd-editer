package li.gkd.studio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SnapshotAttr(
    val id: String? = null,
    val vid: String? = null,
    val name: String? = null,
    val text: String? = null,
    val desc: String? = null,
    val clickable: Boolean = false,
    val focusable: Boolean = false,
    val checkable: Boolean = false,
    val checked: Boolean? = null,
    val editable: Boolean = false,
    val longClickable: Boolean = false,
    val visibleToUser: Boolean = true,
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val childCount: Int = 0,
    val index: Int = 0,
    val depth: Int = 0
)
