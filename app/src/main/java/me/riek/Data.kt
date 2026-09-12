package me.riek

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playedAt: Long,   // epoch millis
    val score: Int,       // net score (right − wrong)
    val right: Int = 0,   // passed
    val wrong: Int = 0,   // failed
    val skipped: Int = 0,
)

@Dao
interface GameDao {
    @Insert
    suspend fun insert(game: Game): Long

    @Query("SELECT * FROM games ORDER BY playedAt ASC")
    fun all(): Flow<List<Game>>

    @Query("SELECT * FROM games ORDER BY playedAt DESC LIMIT 3")
    fun recent(): Flow<List<Game>>

    @Query("SELECT AVG(score) FROM games")
    fun avgScore(): Flow<Double?>

    @Query("SELECT MAX(score) FROM games")
    fun maxScore(): Flow<Int?>
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN right INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE games ADD COLUMN wrong INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE games ADD COLUMN skipped INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(entities = [Game::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "80in8.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
