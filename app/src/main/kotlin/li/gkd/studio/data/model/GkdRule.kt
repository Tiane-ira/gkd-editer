package li.gkd.studio.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "gkd_rules")
@Serializable
data class GkdRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "app_id")
    val appId: String,
    @ColumnInfo(name = "app_name")
    val appName: String,
    @ColumnInfo(name = "group_key")
    val groupKey: Int = 1,
    @ColumnInfo(name = "group_name")
    val groupName: String,
    @ColumnInfo(name = "group_desc")
    val groupDesc: String = "",
    @ColumnInfo(name = "activity_ids")
    val activityIds: String? = null,
    @ColumnInfo(name = "action")
    val action: String = "clickCenter",
    @ColumnInfo(name = "selector")
    val selector: String,
    @ColumnInfo(name = "fast_query")
    val fastQuery: Boolean = false,
    @ColumnInfo(name = "match_root")
    val matchRoot: Boolean = false,
    @ColumnInfo(name = "rule_json5")
    val ruleJson5: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
