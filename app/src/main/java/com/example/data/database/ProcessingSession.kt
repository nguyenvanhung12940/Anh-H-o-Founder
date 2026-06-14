package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processing_sessions")
data class ProcessingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val rosterText: String,
    val logText: String,
    val parsedResultsJson: String,
    val aiSummary: String? = null
)
