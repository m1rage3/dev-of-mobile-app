package com.example.lab9map.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

// маркеры, которые добавил пользователь
@Entity(tableName = "user_places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String
)

@Dao
interface PlaceDao {
    @Query("SELECT * FROM user_places")
    suspend fun getAll(): List<PlaceEntity>

    @Insert
    suspend fun insert(place: PlaceEntity): Long

    @Query("DELETE FROM user_places WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [PlaceEntity::class], version = 1, exportSchema = false)
abstract class PlaceDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao

    companion object {
        @Volatile
        private var INSTANCE: PlaceDatabase? = null

        fun get(context: Context): PlaceDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, PlaceDatabase::class.java, "places.db"
                ).build().also { INSTANCE = it }
            }
    }
}
