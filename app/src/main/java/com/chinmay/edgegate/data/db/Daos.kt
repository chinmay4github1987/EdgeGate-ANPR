package com.chinmay.edgegate.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY blacklisted DESC, createdAt DESC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE plate = :plate")
    suspend fun get(plate: String): VehicleEntity?

    @Upsert
    suspend fun upsert(vehicle: VehicleEntity)

    @Query("UPDATE vehicles SET blacklisted = :blacklisted WHERE plate = :plate")
    suspend fun setBlacklisted(plate: String, blacklisted: Boolean)

    @Query("DELETE FROM vehicles WHERE plate = :plate")
    suspend fun delete(plate: String)
}

@Dao
interface GateEventDao {
    @Insert
    suspend fun insert(event: GateEventEntity): Long

    @Query("SELECT * FROM gate_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<GateEventEntity>>

    @Query("SELECT * FROM gate_events WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun since(since: Long): List<GateEventEntity>

    /** Last direction this plate actually moved (denied attempts don't change state). */
    @Query(
        """SELECT direction FROM gate_events
           WHERE plate = :plate AND action != 'DENY_BLACKLISTED'
           ORDER BY timestamp DESC LIMIT 1"""
    )
    suspend fun lastDirection(plate: String): String?

    @Query("SELECT COUNT(*) FROM gate_events WHERE timestamp >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Query("SELECT * FROM gate_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun observeSince(since: Long): Flow<List<GateEventEntity>>

    /** Retention policy: drop rows older than the configured number of days. */
    @Query("DELETE FROM gate_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long): Int

    /** Vehicles whose most recent movement was an ENTRY, i.e. currently inside. */
    @Query(
        """SELECT COUNT(*) FROM gate_events e
           WHERE e.action != 'DENY_BLACKLISTED' AND e.direction = 'ENTRY'
             AND e.timestamp = (SELECT MAX(x.timestamp) FROM gate_events x
                                WHERE x.plate = e.plate AND x.action != 'DENY_BLACKLISTED')"""
    )
    fun observeInsideCount(): Flow<Int>
}
