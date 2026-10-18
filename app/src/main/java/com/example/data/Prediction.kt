package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Kora's probability for a Polymarket outcome next to the market's, kept to score Kora later. */
@Entity(tableName = "predictions")
data class Prediction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val marketSlug: String,
    val eventSlug: String? = null,
    val question: String,
    val outcome: String,
    val marketProbability: Double,
    val koraProbability: Double,
    val confidence: String,
    val reasoning: String,
    val endDate: Long? = null,
    /** Null until the market resolves. */
    val happened: Boolean? = null,
    val resolvedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
