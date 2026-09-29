package li.gkd.studio.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class SnapshotNode(
    val id: Int,
    val pid: Int,
    val idQf: Boolean? = null,
    val textQf: Boolean? = null,
    val attr: SnapshotAttr,
    var children: List<SnapshotNode> = emptyList()
) {
    @Transient
    var parent: SnapshotNode? = null

    val label: String
        get() {
            val shortClass = attr.name?.substringAfterLast('.') ?: "View"
            val textOrDesc = attr.text ?: attr.desc ?: attr.vid ?: attr.id
            return if (!textOrDesc.isNullOrBlank()) {
                val trimmed = if (textOrDesc.length > 20) textOrDesc.take(20) + "..." else textOrDesc
                "$shortClass : $trimmed"
            } else {
                shortClass
            }
        }
}
