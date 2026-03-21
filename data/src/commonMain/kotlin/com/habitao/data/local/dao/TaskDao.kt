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

    @Query(
        """
        SELECT * FROM tasks
        WHERE parentTaskId IS NULL
        AND (
            (dueDate IS NOT NULL AND dueDate >= :startMillis AND dueDate < :endMillis)
            OR
            (completedAt IS NOT NULL AND completedAt >= :startMillis AND completedAt < :endMillis)
        )
        ORDER BY sortOrder ASC
        """,
    )
    fun observeTasksForDateRange(
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId ORDER BY sortOrder ASC")
    suspend fun getSubtasksByParentId(parentId: String): List<TaskEntity>

    @Query("DELETE FROM tasks WHERE parentTaskId = :parentId")
    suspend fun deleteSubtasksByParentId(parentId: String)

    @Query(
        "SELECT COUNT(*) FROM tasks WHERE parentTaskId IS NULL AND isCompleted = 1 " +
            "AND completedAt >= :startMillis AND completedAt < :endMillis",
    )
    suspend fun getCompletedTaskCountInRange(
        startMillis: Long,
        endMillis: Long,
    ): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE parentTaskId IS NULL")
    suspend fun getTotalTopLevelTaskCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
