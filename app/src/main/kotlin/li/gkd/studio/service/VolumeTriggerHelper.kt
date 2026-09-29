package li.gkd.studio.service

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow

object VolumeTriggerHelper {

    private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    private const val PREFS_NAME = "gkd_studio_settings"
    private const val KEY_VOLUME_TRIGGER = "volume_trigger_enabled"

    val isVolumeTriggerEnabled = MutableStateFlow(true)

    private var isRegistered = false
    private var lastTriggerTime = 0L

    private var volumeReceiver: BroadcastReceiver? = null
    private var volumeObserver: ContentObserver? = null

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isVolumeTriggerEnabled.value = sp.getBoolean(KEY_VOLUME_TRIGGER, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_VOLUME_TRIGGER, enabled).apply()
        isVolumeTriggerEnabled.value = enabled
    }

    fun register(context: Context, onVolumeTriggered: () -> Unit) {
        if (isRegistered) return
        init(context)

        volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == VOLUME_CHANGED_ACTION) {
                    handleVolumeEvent(context, onVolumeTriggered)
                }
            }
        }

        try {
            ContextCompat.registerReceiver(
                context,
                volumeReceiver!!,
                IntentFilter(VOLUME_CHANGED_ACTION),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val handler = Handler(Looper.getMainLooper())
            volumeObserver = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    handleVolumeEvent(context, onVolumeTriggered)
                }
            }
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeObserver!!
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        isRegistered = true
    }

    fun unregister(context: Context) {
        if (!isRegistered) return
        volumeReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
        }
        volumeReceiver = null

        volumeObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
        }
        volumeObserver = null

        isRegistered = false
    }

    private fun handleVolumeEvent(context: Context, onVolumeTriggered: () -> Unit) {
        if (!isVolumeTriggerEnabled.value) return

        // Skip if screen is locked
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km?.isKeyguardLocked == true) return

        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < 2000) return
        lastTriggerTime = now

        onVolumeTriggered()
    }
}
