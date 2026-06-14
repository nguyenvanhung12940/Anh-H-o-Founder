package com.example.data.repository

import com.example.data.database.ProcessingSession
import com.example.data.database.SessionDao
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {
    val allSessions: Flow<List<ProcessingSession>> = sessionDao.getAllSessions()

    suspend fun getSessionById(id: Int): ProcessingSession? {
        return sessionDao.getSessionById(id)
    }

    suspend fun saveSession(session: ProcessingSession): Long {
        return sessionDao.insertSession(session)
    }

    suspend fun deleteSessionById(id: Int) {
        sessionDao.deleteSessionById(id)
    }
}
