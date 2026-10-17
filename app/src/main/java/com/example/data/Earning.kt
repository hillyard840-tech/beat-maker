package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Money the user actually made, logged by them (or by Kora after they confirm). */
@Entity(tableName = "earnings")
data class Earning(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Where it came from, e.g. "Fiverr logo gigs". */
    val source: String,
    /** In minor units (cents) of the tracker's currency. */
    val amountCents: Long,
    /** Time spent, if logged, for the real hourly rate. */
    val minutes: Int? = null,
    val note: String = "",
    val earnedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
