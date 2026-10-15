package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class Skill(
    @PrimaryKey val topic: String,
    val progress: Int = 0, // 0 to 100
    val level: String = "Beginner", // Beginner, Intermediate, Advanced, Expert
    val lastInsight: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
