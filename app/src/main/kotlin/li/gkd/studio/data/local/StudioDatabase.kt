package li.gkd.studio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import li.gkd.studio.data.model.GkdRule
import li.gkd.studio.data.model.Snapshot

@Database(entities = [Snapshot::class, GkdRule::class], version = 1, exportSchema = false)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
    abstract fun ruleDao(): RuleDao

    companion object {
        @Volatile
        private var INSTANCE: StudioDatabase? = null

        fun getDatabase(context: Context): StudioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "gkd_studio_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
