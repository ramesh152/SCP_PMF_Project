package com.ramesh.scp_project.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MediaEntity)

    @Query("SELECT * FROM media ORDER BY timestamp DESC")
    fun getAll(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getByTimeRange(start: Long, end: Long): Flow<List<MediaEntity>>

    @Query("DELETE FROM media")
    suspend fun deleteAll()
}
