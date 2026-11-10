package com.example.lab4storage

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var etNote: EditText
    private lateinit var tvResult: TextView
    private lateinit var btnSaveSP: Button
    private lateinit var btnLoadSP: Button
    private lateinit var btnClearSP: Button
    private lateinit var btnSaveFile: Button
    private lateinit var btnLoadFile: Button

    private lateinit var prefs: SharedPreferences
    private val PREF_KEY_NOTE = "saved_note"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etNote = findViewById(R.id.etNote)
        tvResult = findViewById(R.id.tvResult)
        btnSaveSP = findViewById(R.id.btnSaveSP)
        btnLoadSP = findViewById(R.id.btnLoadSP)
        btnClearSP = findViewById(R.id.btnClearSP)
        btnSaveFile = findViewById(R.id.btnSaveFile)
        btnLoadFile = findViewById(R.id.btnLoadFile)

        prefs = getSharedPreferences("NotesPrefs", MODE_PRIVATE)

        // --- SharedPreferences ---

        btnSaveSP.setOnClickListener {
            val note = etNote.text.toString().trim()
            if (note.isEmpty()) {
                Toast.makeText(this, "Введите текст заметки", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            with(prefs.edit()) {
                putString(PREF_KEY_NOTE, note)
                apply()
            }
            Toast.makeText(this, "Сохранено в SharedPreferences", Toast.LENGTH_SHORT).show()
        }

        btnLoadSP.setOnClickListener {
            val savedNote = prefs.getString(PREF_KEY_NOTE, null)
            if (savedNote.isNullOrEmpty()) {
                tvResult.text = "Нет сохранённой заметки (SP)"
                Toast.makeText(this, "Данных нет", Toast.LENGTH_SHORT).show()
            } else {
                tvResult.text = savedNote
            }
        }

        btnClearSP.setOnClickListener {
            with(prefs.edit()) {
                remove(PREF_KEY_NOTE)
                apply()
            }
            tvResult.text = "Данные (SP) очищены"
            etNote.text.clear()
            Toast.makeText(this, "Очищено", Toast.LENGTH_SHORT).show()
        }

        // --- Файлы ---

        val file = File(filesDir, "my_note.txt")

        btnSaveFile.setOnClickListener {
            val note = etNote.text.toString().trim()
            if (note.isEmpty()) {
                Toast.makeText(this, "Сначала введите текст", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                file.writeText(note)
                Toast.makeText(this, "Сохранено в файл my_note.txt", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка записи: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnLoadFile.setOnClickListener {
            try {
                if (!file.exists()) {
                    tvResult.text = "Файл не найден"
                    Toast.makeText(this, "Файла нет", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val content = file.readText()
                tvResult.text = content
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка чтения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
