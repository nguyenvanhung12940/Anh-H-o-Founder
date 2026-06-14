package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM processing_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ProcessingSession>>

    @Query("SELECT * FROM processing_sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): ProcessingSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ProcessingSession): Long

    @Delete
    suspend fun deleteSession(session: ProcessingSession)

    @Query("DELETE FROM processing_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)
}
