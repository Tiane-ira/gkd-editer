package li.gkd.studio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import li.gkd.studio.MainActivity
import li.gkd.studio.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID = "gkd_studio_capture_channel"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GKD 快照服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "后台运行常驻通知与抓取操作"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun getCapturePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, StudioActionReceiver::class.java).apply {
            action = StudioActionReceiver.ACTION_CAPTURE_SNAPSHOT
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 100, intent, flags)
    }

    private fun getContentPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 101, intent, flags)
    }

    fun buildRunningNotification(context: Context): Notification {
        createNotificationChannel(context)
        val captureAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            "抓取快照",
            getCapturePendingIntent(context)
        ).build()

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("GKD Rule Studio")
            .setContentText("状态：服务运行中")
            .setContentIntent(getContentPendingIntent(context))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(captureAction)
            .build()
    }

    fun buildSavedNotification(
        context: Context,
        appName: String,
        packageName: String,
        time: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    ): Notification {
        createNotificationChannel(context)
        val captureAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            "抓取快照",
            getCapturePendingIntent(context)
        ).build()

        val text = "$appName ($packageName) · $time"
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("快照已保存")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("快照已保存\n$appName\n$packageName\n$time"))
            .setContentIntent(getContentPendingIntent(context))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(captureAction)
            .build()
    }
}
