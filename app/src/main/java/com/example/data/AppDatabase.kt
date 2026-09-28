package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdTime DESC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdTime DESC")
    fun getTasksByStatus(status: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE status IN ('RUNNING', 'WAITING') ORDER BY createdTime DESC")
    fun getActiveTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskItem?

    @Query("SELECT * FROM tasks WHERE projectName = :projectName ORDER BY createdTime DESC LIMIT 1")
    suspend fun getLatestTaskForProject(projectName: String): TaskItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTask(task: TaskItem)

    @Update
    suspend fun updateTask(task: TaskItem)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("SELECT * FROM task_logs WHERE taskId = :taskId ORDER BY timestamp ASC")
    fun getLogsForTask(taskId: String): Flow<List<TaskLog>>

    @Insert
    suspend fun insertLog(log: TaskLog)

    @Query("SELECT COUNT(*) FROM tasks WHERE status IN ('RUNNING', 'WAITING')")
    fun getActiveTaskCount(): Flow<Int>
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}

@Database(
    entities = [TaskItem::class, TaskLog::class, ChatMessage::class],
    version = 1,
    exportSchema = false
)
abstract class ArushiDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: ArushiDatabase? = null

        fun getDatabase(context: Context): ArushiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArushiDatabase::class.java,
                    "arushi_tasks.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
