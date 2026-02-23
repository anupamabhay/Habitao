package com.habitao.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.habitao.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskById(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE dueDate = :dateMillis ORDER BY sortOrder ASC")
    suspend fun getTasksForDate(dateMillis: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE dueDate = :dateMillis ORDER BY sortOrder ASC")
    fun observeTasksForDate(dateMillis: Long): Flow<List<TaskEntity>>
}
