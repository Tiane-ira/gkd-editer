package li.gkd.studio

import android.app.Application
import li.gkd.studio.data.local.StudioDatabase
import li.gkd.studio.service.NotificationHelper

class App : Application() {

    lateinit var database: StudioDatabase
        private set

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = StudioDatabase.getDatabase(this)
        NotificationHelper.createNotificationChannel(this)
    }
}
