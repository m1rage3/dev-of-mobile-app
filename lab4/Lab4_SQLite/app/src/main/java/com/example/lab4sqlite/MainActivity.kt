package com.example.lab4sqlite

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnAdd: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnDelete: Button
    private lateinit var recyclerView: RecyclerView

    private lateinit var dbHelper: NotesDbHelper
    private lateinit var adapter: NotesAdapter

    // заметка, выбранная в списке (нужна для «Обновить» и «Удалить»)
    private var selectedNote: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etTitle = findViewById(R.id.etTitle)
        etContent = findViewById(R.id.etContent)
        btnAdd = findViewById(R.id.btnAdd)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnDelete = findViewById(R.id.btnDelete)
        recyclerView = findViewById(R.id.recyclerView)

        dbHelper = NotesDbHelper(this)

        adapter = NotesAdapter { note -> selectNote(note) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnAdd.setOnClickListener { addNote() }
        btnUpdate.setOnClickListener { updateNote() }
        btnDelete.setOnClickListener { deleteNote() }

        loadNotes()
    }

    // все запросы к базе — в фоновом потоке (Dispatchers.IO),
    // обновление списка — обратно в главном потоке
    private fun loadNotes() {
        lifecycleScope.launch {
            val notes = withContext(Dispatchers.IO) { dbHelper.getAllNotes() }
            adapter.submit(notes)
        }
    }

    private fun addNote() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()
        if (title.isEmpty()) {
            toast("Заголовок не может быть пустым")
            return
        }
        lifecycleScope.launch {
            val id = withContext(Dispatchers.IO) { dbHelper.insertNote(title, content) }
            if (id == -1L) {
                toast("Ошибка: заметка не сохранена")
            } else {
                toast("Заметка добавлена (id = $id)")
                clearForm()
                loadNotes()
            }
        }
    }

    private fun updateNote() {
        val note = selectedNote
        if (note == null) {
            toast("Сначала выберите заметку в списке")
            return
        }
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()
        if (title.isEmpty()) {
            toast("Заголовок не может быть пустым")
            return
        }
        lifecycleScope.launch {
            val rows = withContext(Dispatchers.IO) { dbHelper.updateNote(note.id, title, content) }
            if (rows > 0) {
                toast("Заметка обновлена")
                clearForm()
                loadNotes()
            } else {
                toast("Заметка не найдена")
            }
        }
    }

    private fun deleteNote() {
        val note = selectedNote
        if (note == null) {
            toast("Сначала выберите заметку в списке")
            return
        }
        lifecycleScope.launch {
            val rows = withContext(Dispatchers.IO) { dbHelper.deleteNote(note.id) }
            if (rows > 0) {
                toast("Удалено строк: $rows")
                clearForm()
                loadNotes()
            } else {
                toast("Заметка не найдена")
            }
        }
    }

    private fun selectNote(note: Note) {
        selectedNote = note
        etTitle.setText(note.title)
        etContent.setText(note.content)
        adapter.setSelected(note.id)
    }

    private fun clearForm() {
        selectedNote = null
        etTitle.text.clear()
        etContent.text.clear()
        adapter.setSelected(null)
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // закрываем соединение с базой
        dbHelper.close()
    }
}
