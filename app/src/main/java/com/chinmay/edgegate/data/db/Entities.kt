package com.chinmay.edgegate.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Registered vehicle (resident, staff, pre-approved visitor) or a blacklisted plate. */
@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val plate: String,   // canonical, e.g. KA01AB1234
    val display: String,             // KA 01 AB 1234
    val category: String,            // VehicleCategory.name
    val blacklisted: Boolean,
    val ownerName: String?,
    val unit: String?,               // flat / office / department
    val phone: String?,
    val createdAt: Long,
)

/** One line of the digital gate register. */
@Entity(
    tableName = "gate_events",
    indices = [Index("plate"), Index("timestamp")],
)
data class GateEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plate: String,
    val display: String,
    val direction: String,           // Direction.name
    val action: String,              // GateAction.name
    val category: String,            // VehicleCategory.name at time of event
    val confidence: Float,
    val source: String,              // "AI" or "MANUAL"
    val latencyMs: Double?,
    val note: String?,
    val timestamp: Long,
)
