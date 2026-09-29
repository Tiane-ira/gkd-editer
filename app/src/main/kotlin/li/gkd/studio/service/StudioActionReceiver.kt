package li.gkd.studio.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class StudioActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CAPTURE_SNAPSHOT = "li.gkd.studio.ACTION_CAPTURE_SNAPSHOT"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_CAPTURE_SNAPSHOT) {
            val service = StudioAccessibilityService.instance
            if (service != null && StudioAccessibilityService.isServiceRunning.value) {
                service.triggerCapture()
            } else {
                Toast.makeText(context, "请先在系统设置中开启 GKD Rule Studio 无障碍服务", Toast.LENGTH_LONG).show()
            }
        }
    }
}
