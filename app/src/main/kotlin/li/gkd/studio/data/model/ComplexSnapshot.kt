package li.gkd.studio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppInfo(
    val id: String,
    val name: String,
    val versionCode: Long = 0,
    val versionName: String? = null,
    val isSystem: Boolean = false,
    val mtime: Long = System.currentTimeMillis(),
    val hidden: Boolean = false
)

@Serializable
data class DeviceInfo(
    val device: String = android.os.Build.DEVICE,
    val model: String = android.os.Build.MODEL,
    val manufacturer: String = android.os.Build.MANUFACTURER,
    val brand: String = android.os.Build.BRAND,
    val sdkInt: Int = android.os.Build.VERSION.SDK_INT,
    val release: String = android.os.Build.VERSION.RELEASE,
    val gkdVersionCode: Int = 1000,
    val gkdVersionName: String = "1.0.0"
)

@Serializable
data class ComplexSnapshot(
    val id: Long,
    val appId: String,
    val activityId: String? = null,
    val screenHeight: Int,
    val screenWidth: Int,
    val isLandscape: Boolean = false,
    val appInfo: AppInfo? = null,
    val gkdAppInfo: AppInfo? = null,
    val device: DeviceInfo = DeviceInfo(),
    val nodes: List<SnapshotNode> = emptyList()
)
