package li.gkd.studio.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "snapshots")
@Serializable
data class Snapshot(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "app_name")
    val appName: String? = null,
    @ColumnInfo(name = "activity")
    val activity: String? = null,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "width")
    val width: Int = 0,
    @ColumnInfo(name = "height")
    val height: Int = 0,
    @ColumnInfo(name = "is_landscape")
    val isLandscape: Boolean = false,
    @ColumnInfo(name = "screenshot_path")
    val screenshotPath: String,
    @ColumnInfo(name = "tree_json")
    val treeJson: String = "",
    @ColumnInfo(name = "node_count")
    val nodeCount: Int = 0
)
