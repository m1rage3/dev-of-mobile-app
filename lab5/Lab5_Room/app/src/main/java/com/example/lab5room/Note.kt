package com.example.lab5room

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity = таблица notes в SQLite, Room создаёт её сам
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
