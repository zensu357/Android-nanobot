package com.example.nanobot.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nanobot.core.database.entity.MemorySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemorySummaryDao {
    @Query("SELECT * FROM memory_summaries ORDER BY updatedAt DESC")
    fun observeSummaries(): Flow<List<MemorySummaryEntity>>

    @Query("SELECT * FROM memory_summaries WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSummaryForSession(sessionId: String): MemorySummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: MemorySummaryEntity)
}
