package com.example.lab5recycler

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var etNewNote: EditText
    private lateinit var btnAdd: Button
    private lateinit var rvNotes: RecyclerView
    private lateinit var adapter: NotesAdapter

    private val notesList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etNewNote = findViewById(R.id.etNewNote)
        btnAdd = findViewById(R.id.btnAdd)
        rvNotes = findViewById(R.id.rvNotes)

        adapter = NotesAdapter(notesList) { position ->
            adapter.removeNote(position)
        }

        rvNotes.layoutManager = LinearLayoutManager(this)
        rvNotes.adapter = adapter

        btnAdd.setOnClickListener {
            val text = etNewNote.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Введите текст заметки", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            notesList.add(text)
            val lastPosition = notesList.size - 1
            adapter.notifyItemInserted(lastPosition)
            rvNotes.scrollToPosition(lastPosition)
            etNewNote.text.clear()
        }
    }
}
