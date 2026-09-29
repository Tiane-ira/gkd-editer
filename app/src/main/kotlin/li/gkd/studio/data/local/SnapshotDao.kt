package li.gkd.studio.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import li.gkd.studio.data.model.Snapshot

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<Snapshot>>

    @Query("SELECT * FROM snapshots WHERE id = :id LIMIT 1")
    suspend fun getSnapshotById(id: String): Snapshot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: Snapshot)

    @Delete
    suspend fun deleteSnapshot(snapshot: Snapshot)

    @Query("DELETE FROM snapshots WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM snapshots WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM snapshots WHERE package_name LIKE '%' || :keyword || '%' OR app_name LIKE '%' || :keyword || '%' ORDER BY timestamp DESC")
    fun searchSnapshots(keyword: String): Flow<List<Snapshot>>

    @Query("SELECT COUNT(*) FROM snapshots")
    fun count(): Flow<Int>
}
