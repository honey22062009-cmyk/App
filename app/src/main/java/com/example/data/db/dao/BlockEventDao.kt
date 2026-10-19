package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.db.entity.BlockEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockEventDao {

    @Query("SELECT * FROM block_events ORDER BY timestamp DESC LIMIT 100")
    fun getRecentBlockEventsFlow(): Flow<List<BlockEventEntity>>

    @Query("SELECT COUNT(*) FROM block_events")
    fun getTotalBlocksCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_events WHERE timestamp >= :since")
    fun getBlocksSinceFlow(since: Long): Flow<Int>

    @Insert
    suspend fun insert(event: BlockEventEntity)

    @Query("DELETE FROM block_events WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM block_events")
    suspend fun clearAll()
}
