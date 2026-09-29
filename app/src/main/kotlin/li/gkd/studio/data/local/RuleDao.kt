package li.gkd.studio.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import li.gkd.studio.data.model.GkdRule

@Dao
interface RuleDao {
    @Query("SELECT * FROM gkd_rules ORDER BY updated_at DESC")
    fun getAllRules(): Flow<List<GkdRule>>

    @Query("SELECT * FROM gkd_rules WHERE app_id = :appId ORDER BY updated_at DESC")
    fun getRulesByAppId(appId: String): Flow<List<GkdRule>>

    @Query("SELECT * FROM gkd_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: Long): GkdRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: GkdRule): Long

    @Update
    suspend fun updateRule(rule: GkdRule)

    @Delete
    suspend fun deleteRule(rule: GkdRule)

    @Query("DELETE FROM gkd_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM gkd_rules WHERE id IN (:ids)")
    suspend fun deleteRulesByIds(ids: List<Long>)

    @Query("SELECT * FROM gkd_rules WHERE app_name LIKE '%' || :query || '%' OR app_id LIKE '%' || :query || '%' OR group_name LIKE '%' || :query || '%'")
    fun searchRules(query: String): Flow<List<GkdRule>>
}
