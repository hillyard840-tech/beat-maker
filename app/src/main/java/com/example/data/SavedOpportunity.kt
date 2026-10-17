package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A job, side-income idea or build idea the user kept, with how far they've got. */
@Entity(tableName = "opportunities")
data class SavedOpportunity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** [KIND_JOB], [KIND_INCOME] or [KIND_BUILD]. */
    val kind: String,
    val title: String,
    val subtitle: String = "",
    val url: String? = null,
    /** Markdown with the full details. */
    val details: String = "",
    val status: String = STATUS_SAVED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val KIND_JOB = "job"
        const val KIND_INCOME = "income"
        const val KIND_BUILD = "build"
        const val STATUS_SAVED = "saved"
        const val STATUS_ACTIVE = "active"   // applied / working on it
        const val STATUS_DONE = "done"       // got it / launched
        const val STATUS_DROPPED = "dropped"
    }
}
