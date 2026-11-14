package com.example.lab4sqlite

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NotesDbHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "notes.db"
        private const val DB_VERSION = 1

        const val TABLE_NOTES = "notes"
        const val COL_ID = "id"
        const val COL_TITLE = "title"
        const val COL_CONTENT = "content"
        const val COL_TIMESTAMP = "timestamp"
    }

    // вызывается один раз, когда файла базы ещё нет
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NOTES (" +
                "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_TITLE TEXT NOT NULL, " +
                "$COL_CONTENT TEXT, " +
                "$COL_TIMESTAMP INTEGER NOT NULL)"
        )
    }

    // для лабы: при смене версии просто пересоздаём таблицу
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        onCreate(db)
    }

    // возвращает id новой записи или -1, если вставка не прошла
    fun insertNote(title: String, content: String): Long {
        val values = ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_CONTENT, content)
            put(COL_TIMESTAMP, System.currentTimeMillis())
        }
        return writableDatabase.insert(TABLE_NOTES, null, values)
    }

    fun getAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val cursor = readableDatabase.query(
            TABLE_NOTES,
            null,           // все колонки (SELECT *)
            null, null,     // без WHERE
            null, null,     // без GROUP BY / HAVING
            "$COL_TIMESTAMP DESC"
        )
        // use {} сам закроет курсор, даже если внутри будет исключение
        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(COL_ID)
            val titleIndex = c.getColumnIndexOrThrow(COL_TITLE)
            val contentIndex = c.getColumnIndexOrThrow(COL_CONTENT)
            val timeIndex = c.getColumnIndexOrThrow(COL_TIMESTAMP)

            while (c.moveToNext()) {
                notes.add(
                    Note(
                        id = c.getInt(idIndex),
                        title = c.getString(titleIndex),
                        content = c.getString(contentIndex) ?: "",
                        timestamp = c.getLong(timeIndex)
                    )
                )
            }
        }
        return notes
    }

    // возвращает количество обновлённых строк
    fun updateNote(id: Int, title: String, content: String): Int {
        val values = ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_CONTENT, content)
            put(COL_TIMESTAMP, System.currentTimeMillis())
        }
        return writableDatabase.update(
            TABLE_NOTES, values, "$COL_ID = ?", arrayOf(id.toString())
        )
    }

    // возвращает количество удалённых строк
    fun deleteNote(id: Int): Int {
        return writableDatabase.delete(TABLE_NOTES, "$COL_ID = ?", arrayOf(id.toString()))
    }
}
