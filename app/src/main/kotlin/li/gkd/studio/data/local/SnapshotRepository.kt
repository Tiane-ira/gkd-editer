package li.gkd.studio.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import li.gkd.studio.data.model.AppInfo
import li.gkd.studio.data.model.ComplexSnapshot
import li.gkd.studio.data.model.DeviceInfo
import li.gkd.studio.data.model.Snapshot
import li.gkd.studio.data.model.SnapshotNode
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SnapshotRepository {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private fun getSnapshotsDir(context: Context): File {
        val dir = File(context.filesDir, "snapshots")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSnapshotDir(context: Context, id: String): File {
        val dir = File(getSnapshotsDir(context), id)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun saveSnapshot(
        context: Context,
        id: String,
        packageName: String,
        appName: String?,
        activity: String?,
        width: Int,
        height: Int,
        isLandscape: Boolean,
        bitmap: Bitmap?,
        nodes: List<SnapshotNode>
    ): Snapshot = withContext(Dispatchers.IO) {
        val dir = getSnapshotDir(context, id)
        val imageFile = File(dir, "$id.webp")
        val jsonFile = File(dir, "$id.json")

        // 1. Save screenshot image
        if (bitmap != null) {
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
            }
        }

        // 2. Build ComplexSnapshot compatible with GKD
        val complex = ComplexSnapshot(
            id = id.toLongOrNull() ?: System.currentTimeMillis(),
            appId = packageName,
            activityId = activity,
            screenHeight = height,
            screenWidth = width,
            isLandscape = isLandscape,
            appInfo = appName?.let {
                AppInfo(id = packageName, name = it)
            },
            device = DeviceInfo(),
            nodes = nodes
        )

        val jsonStr = json.encodeToString(complex)
        jsonFile.writeText(jsonStr)

        val snapshot = Snapshot(
            id = id,
            packageName = packageName,
            appName = appName,
            activity = activity,
            timestamp = id.toLongOrNull() ?: System.currentTimeMillis(),
            width = width,
            height = height,
            isLandscape = isLandscape,
            screenshotPath = imageFile.absolutePath,
            treeJson = jsonFile.absolutePath,
            nodeCount = nodes.size
        )

        val db = StudioDatabase.getDatabase(context)
        db.snapshotDao().insertSnapshot(snapshot)

        snapshot
    }

    suspend fun loadSnapshotNodes(snapshot: Snapshot): List<SnapshotNode> = withContext(Dispatchers.IO) {
        val file = File(snapshot.treeJson)
        if (!file.exists()) return@withContext emptyList()
        val text = file.readText()
        val complex = runCatching { json.decodeFromString<ComplexSnapshot>(text) }.getOrNull()
        val rawNodes = complex?.nodes ?: emptyList()
        reconstructTree(rawNodes)
    }

    fun reconstructTree(nodes: List<SnapshotNode>): List<SnapshotNode> {
        val nodeMap = nodes.associateBy { it.id }
        val roots = mutableListOf<SnapshotNode>()

        // Link parent and children
        for (node in nodes) {
            val parent = nodeMap[node.pid]
            if (parent != null && parent !== node) {
                node.parent = parent
            } else if (node.pid == -1 || node.pid == node.id || parent == null) {
                roots.add(node)
            }
        }

        // Reconstruct children list for each node
        val childrenMap = nodes.groupBy { it.pid }
        for (node in nodes) {
            node.children = childrenMap[node.id] ?: emptyList()
        }

        return nodes
    }

    suspend fun deleteSnapshot(context: Context, snapshot: Snapshot) = withContext(Dispatchers.IO) {
        val dir = getSnapshotDir(context, snapshot.id)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        val db = StudioDatabase.getDatabase(context)
        db.snapshotDao().deleteSnapshot(snapshot)
    }

    suspend fun deleteSnapshots(context: Context, snapshots: List<Snapshot>) = withContext(Dispatchers.IO) {
        for (snapshot in snapshots) {
            val dir = getSnapshotDir(context, snapshot.id)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
        val db = StudioDatabase.getDatabase(context)
        db.snapshotDao().deleteByIds(snapshots.map { it.id })
    }

    suspend fun createExportZip(context: Context, snapshot: Snapshot): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "export")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val zipFile = File(cacheDir, "snapshot-${snapshot.packageName}-${snapshot.id}.zip")
        if (zipFile.exists()) zipFile.delete()

        val snapDir = getSnapshotDir(context, snapshot.id)
        val files = snapDir.listFiles() ?: emptyArray()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            for (file in files) {
                if (!file.isFile) continue
                FileInputStream(file).use { fis ->
                    val entry = ZipEntry(file.name)
                    zos.putNextEntry(entry)
                    fis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }

        zipFile
    }

    suspend fun createBatchExportZip(context: Context, snapshots: List<Snapshot>): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "export")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val zipFile = File(cacheDir, "gkd_snapshots_$timestamp.zip")
        if (zipFile.exists()) zipFile.delete()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            for (snapshot in snapshots) {
                val snapDir = getSnapshotDir(context, snapshot.id)
                val files = snapDir.listFiles() ?: emptyArray()
                val folderName = "${snapshot.appName ?: snapshot.packageName}_${snapshot.id}"
                    .replace(Regex("[\\\\/:*?\"<>|]"), "_")

                for (file in files) {
                    if (!file.isFile) continue
                    FileInputStream(file).use { fis ->
                        val entry = ZipEntry("$folderName/${file.name}")
                        zos.putNextEntry(entry)
                        fis.copyTo(zos)
                        zos.closeEntry()
                    }
                }
            }
        }

        zipFile
    }
}
