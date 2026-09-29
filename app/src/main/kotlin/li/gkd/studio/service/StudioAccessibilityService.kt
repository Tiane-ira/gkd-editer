package li.gkd.studio.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import li.gkd.studio.data.local.SnapshotRepository
import li.gkd.studio.rule.RuleGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

class StudioAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val captureMutex = Mutex()

    companion object {
        const val ACTION_CAPTURE_SNAPSHOT = "li.gkd.studio.ACTION_CAPTURE_SNAPSHOT"

        @Volatile
        var instance: StudioAccessibilityService? = null
            private set

        val isServiceRunning = MutableStateFlow(false)
        val currentPackageName = MutableStateFlow<String?>(null)
        val currentActivityName = MutableStateFlow<String?>(null)
        val currentAppName = MutableStateFlow<String?>(null)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceRunning.value = true

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NotificationHelper.NOTIFICATION_ID, NotificationHelper.buildRunningNotification(this))

        VolumeTriggerHelper.register(this) {
            triggerCapture()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CAPTURE_SNAPSHOT) {
            triggerCapture()
        }
        return START_STICKY
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            val cls = event.className?.toString()
            if (!pkg.isNullOrBlank() && pkg != packageName && pkg != "com.android.systemui") {
                currentPackageName.value = pkg
                if (RuleGenerator.isPotentialActivityName(cls)) {
                    currentActivityName.value = cls
                }
                resolveAppName(pkg)?.let {
                    currentAppName.value = it
                }
            }
        }
    }

    override fun onInterrupt() {
        // Required callback
    }

    override fun onDestroy() {
        super.onDestroy()
        VolumeTriggerHelper.unregister(this)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NotificationHelper.NOTIFICATION_ID)
        instance = null
        isServiceRunning.value = false
        serviceScope.cancel()
    }

    fun triggerCapture() {
        serviceScope.launch {
            if (!captureMutex.tryLock()) {
                Toast.makeText(this@StudioAccessibilityService, "快照抓取中，请稍候...", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                captureSnapshotInternal()
            } finally {
                captureMutex.unlock()
            }
        }
    }

    private suspend fun captureSnapshotInternal() {
        // 1. Collapse notification bar / shade so foreground app is revealed
        collapseStatusBarAndShade()

        // 2. Poll for foreground app root (waiting for notification shade to collapse)
        val rootNode = findForegroundApplicationRoot()
        if (rootNode == null) {
            Toast.makeText(this, "未找到活动应用窗口，请确认前台有运行的应用", Toast.LENGTH_SHORT).show()
            return
        }

        val pkgName = rootNode.packageName?.toString() ?: currentPackageName.value ?: "unknown.package"
        if (pkgName == "com.android.systemui") {
            Toast.makeText(this, "正在返回前台应用，请重试抓取", Toast.LENGTH_SHORT).show()
            return
        }

        val appLabel = resolveAppName(pkgName) ?: pkgName
        val actName = if (currentPackageName.value == pkgName && RuleGenerator.isPotentialActivityName(currentActivityName.value)) {
            currentActivityName.value
        } else {
            null
        }

        // 3. Take screenshot AFTER the shade has collapsed
        val screenshotBitmap = captureScreenshotBitmap()

        // 4. Extract UI Tree from foreground app
        val nodes = SnapshotCaptureHelper.info2nodeList(rootNode)

        // 5. Screen Dimensions
        val displayMetrics = resources.displayMetrics
        val width = screenshotBitmap?.width ?: displayMetrics.widthPixels
        val height = screenshotBitmap?.height ?: displayMetrics.heightPixels
        val isLandscape = width > height

        val snapshotId = System.currentTimeMillis().toString()

        // 6. Save to Repository and Room
        SnapshotRepository.saveSnapshot(
            context = this,
            id = snapshotId,
            packageName = pkgName,
            appName = appLabel,
            activity = actName,
            width = width,
            height = height,
            isLandscape = isLandscape,
            bitmap = screenshotBitmap,
            nodes = nodes
        )

        // 7. Update Notification
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.buildSavedNotification(this, appLabel, pkgName, timeStr)
        )

        // 8. Haptic feedback
        vibrateFeedback()
        Toast.makeText(this, "快照已抓取: $appLabel", Toast.LENGTH_SHORT).show()
    }

    private fun collapseStatusBarAndShade() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        } else {
            @Suppress("DEPRECATION")
            try {
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            } catch (_: Exception) {}
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private suspend fun findForegroundApplicationRoot(): AccessibilityNodeInfo? {
        val startTime = System.currentTimeMillis()
        var targetRoot: AccessibilityNodeInfo? = null

        // Check active window immediately (fast path for volume trigger when app is already in foreground)
        val initialActive = rootInActiveWindow
        val initialPkg = initialActive?.packageName?.toString()
        if (initialActive != null && initialPkg != null && initialPkg != "com.android.systemui" && initialPkg != packageName) {
            return initialActive
        }

        // Give the notification shade up to 1200ms to close and reveal the underlying application
        while (System.currentTimeMillis() - startTime < 1200) {
            kotlinx.coroutines.delay(200)

            // Check active window
            val active = rootInActiveWindow
            val activePkg = active?.packageName?.toString()
            if (active != null && activePkg != null && activePkg != "com.android.systemui" && activePkg != packageName) {
                targetRoot = active
                break
            }

            // Check windows list
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val winList = try { windows } catch (_: Throwable) { null }
                if (winList != null) {
                    val appWin = winList.firstOrNull { win ->
                        if (win.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION) {
                            val pkg = win.root?.packageName?.toString()
                            pkg != null && pkg != "com.android.systemui" && pkg != packageName
                        } else {
                            false
                        }
                    }
                    if (appWin != null && appWin.root != null) {
                        targetRoot = appWin.root
                        break
                    }
                }
            }
        }

        if (targetRoot == null) {
            targetRoot = rootInActiveWindow ?: findRootFromWindows()
        }

        return targetRoot
    }

    private fun findRootFromWindows(): AccessibilityNodeInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val winList = try { windows } catch (_: Throwable) { null } ?: return null
            for (win in winList) {
                if (win.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION) {
                    val root = win.root ?: continue
                    val pkg = root.packageName?.toString()
                    if (pkg != null && pkg != "com.android.systemui" && pkg != packageName) {
                        return root
                    }
                }
            }
            for (win in winList) {
                val root = win.root ?: continue
                val pkg = root.packageName?.toString()
                if (pkg != null && pkg != "com.android.systemui" && pkg != packageName) {
                    return root
                }
            }
        }
        return null
    }

    private suspend fun captureScreenshotBitmap(): Bitmap? = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        try {
                            val buffer = screenshotResult.hardwareBuffer
                            val colorSpace = screenshotResult.colorSpace
                            val hwBitmap = Bitmap.wrapHardwareBuffer(buffer, colorSpace)
                            val copy = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                            hwBitmap?.recycle()
                            buffer.close()
                            if (cont.isActive) cont.resume(copy)
                        } catch (e: Exception) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            )
        } else {
            cont.resume(null)
        }
    }

    private fun resolveAppName(packageName: String): String? {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun vibrateFeedback() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (_: Exception) {
            // Ignore if vibration permission not available
        }
    }
}
