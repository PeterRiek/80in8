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

    @Query("DELETE FROM games")
    suspend fun deleteAll()
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN right INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE games ADD COLUMN wrong INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE games ADD COLUMN skipped INTEGER NOT NULL DEFAULT 0")
    }
}

/** Seeds 40 demo games (upward trend with occasional Cracked peaks) on first DB creation. */
private val SEED_CALLBACK = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val rnd = kotlin.random.Random(42)
        for (i in 0 until 40) {
            // ~18% of games spike into the Cracked band (74–80), rest follow the trend
            val target = if (rnd.nextInt(100) < 18) 74 + rnd.nextInt(7)
                         else (18 + i + rnd.nextInt(-7, 8)).coerceIn(4, 68)
            val maxWrong = ((80 - target) / 2).coerceIn(0, 13)
            val wrong = if (maxWrong == 0) 0 else rnd.nextInt(maxWrong + 1)
            val right = (target + wrong).coerceAtMost(80)
            val skipped = 80 - right - wrong
            val score = right - wrong
            val playedAt = now - (40 - i) * day - rnd.nextInt(0, 12 * 60 * 60 * 1000)
            db.execSQL(
                "INSERT INTO games (playedAt, score, right, wrong, skipped) VALUES ($playedAt, $score, $right, $wrong, $skipped)"
            )
        }
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
                ).addMigrations(MIGRATION_1_2).addCallback(SEED_CALLBACK).build().also { instance = it }
            }
    }
}
