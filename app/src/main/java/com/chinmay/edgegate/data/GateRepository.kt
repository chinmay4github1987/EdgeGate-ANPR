package com.chinmay.edgegate.data

import com.chinmay.edgegate.core.CsvExporter
import com.chinmay.edgegate.core.Direction
import com.chinmay.edgegate.core.GateDecision
import com.chinmay.edgegate.core.GateLogRow
import com.chinmay.edgegate.core.VehicleCategory
import com.chinmay.edgegate.core.VehicleProfile
import com.chinmay.edgegate.data.db.AppDatabase
import com.chinmay.edgegate.data.db.GateEventEntity
import com.chinmay.edgegate.data.db.VehicleEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Single source of truth for the registry and the gate log. Offline-first: Room only. */
class GateRepository(private val db: AppDatabase) {

    val vehicles: Flow<List<VehicleEntity>> = db.vehicles().observeAll()

    fun recentEvents(limit: Int = 300): Flow<List<GateEventEntity>> = db.events().observeRecent(limit)
    fun countSince(since: Long): Flow<Int> = db.events().observeCountSince(since)
    fun eventsSince(since: Long): Flow<List<GateEventEntity>> = db.events().observeSince(since)
    suspend fun purgeOlderThan(before: Long): Int = db.events().deleteOlderThan(before)
    val insideCount: Flow<Int> = db.events().observeInsideCount()

    suspend fun profile(plate: String): VehicleProfile? = db.vehicles().get(plate)?.let {
        VehicleProfile(
            plate = it.plate,
            category = runCatching { VehicleCategory.valueOf(it.category) }.getOrDefault(VehicleCategory.UNKNOWN),
            blacklisted = it.blacklisted,
            ownerName = it.ownerName,
            unit = it.unit,
        )
    }

    suspend fun lastDirection(plate: String): Direction? =
        db.events().lastDirection(plate)?.let { runCatching { Direction.valueOf(it) }.getOrNull() }

    suspend fun record(decision: GateDecision, display: String, confidence: Float, source: String, latencyMs: Double?) {
        db.events().insert(
            GateEventEntity(
                plate = decision.plate,
                display = display,
                direction = decision.direction.name,
                action = decision.action.name,
                category = decision.category.name,
                confidence = confidence,
                source = source,
                latencyMs = latencyMs,
                note = decision.message,
                timestamp = decision.timestampMs,
            )
        )
    }

    suspend fun saveVehicle(vehicle: VehicleEntity) = db.vehicles().upsert(vehicle)
    suspend fun setBlacklisted(plate: String, value: Boolean) = db.vehicles().setBlacklisted(plate, value)
    suspend fun deleteVehicle(plate: String) = db.vehicles().delete(plate)

    suspend fun exportCsv(since: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val rows = db.events().since(since).map {
            GateLogRow(fmt.format(Date(it.timestamp)), it.display, it.direction, it.action, it.category,
                it.confidence, it.source, it.note)
        }
        return CsvExporter.toCsv(rows)
    }
}
