package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String,
    val fact: String,
    val category: String = "general", // "preference", "project", "personal", "general"
    val timestamp: Long = System.currentTimeMillis()
)
