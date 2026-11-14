package com.example.lab5room

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val viewModel: NoteViewModel by viewModels()

    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var adapter: NotesAdapter

    // текущая выбранная заметка (для «Обновить» и «Удалить»)
    private var selectedNote: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etTitle = findViewById(R.id.etTitle)
        etContent = findViewById(R.id.etContent)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        adapter = NotesAdapter { note -> selectNote(note) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // при любом изменении таблицы список обновится сам
        viewModel.allNotes.observe(this) { notes ->
            adapter.submitList(notes)
        }

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                toast("Заголовок не может быть пустым")
                return@setOnClickListener
            }
            viewModel.insert(Note(title = title, content = etContent.text.toString().trim()))
            toast("Заметка добавлена")
            clearForm()
        }

        findViewById<Button>(R.id.btnUpdate).setOnClickListener {
            val note = selectedNote
            if (note == null) {
                toast("Сначала выберите заметку в списке")
                return@setOnClickListener
            }
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                toast("Заголовок не может быть пустым")
                return@setOnClickListener
            }
            // id тот же — Room обновит именно эту строку
            viewModel.update(
                note.copy(
                    title = title,
                    content = etContent.text.toString().trim(),
                    timestamp = System.currentTimeMillis()
                )
            )
            toast("Заметка обновлена")
            clearForm()
        }

        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            val note = selectedNote
            if (note == null) {
                toast("Сначала выберите заметку в списке")
                return@setOnClickListener
            }
            viewModel.delete(note)
            toast("Заметка удалена")
            clearForm()
        }
    }

    // клик по элементу: подставляем данные в поля для редактирования
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
}
