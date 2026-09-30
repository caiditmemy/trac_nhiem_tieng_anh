package com.example.android_student.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "exam_history")
data class ExamHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectName: String,
    val score: Double,
    val correctCount: Int,
    val totalCount: Int,
    val date: String,
    val isPassed: Boolean,
    val mode: String // "Luyện tập" hoặc "Thi thử"
)

@Dao
interface ExamHistoryDao {
    @Insert
    suspend fun insertHistory(item: ExamHistoryEntity)

    @Query("SELECT * FROM exam_history ORDER BY id DESC")
    fun getAllHistories(): Flow<List<ExamHistoryEntity>>
}

@Database(entities = [ExamHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examHistoryDao(): ExamHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_exam_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}