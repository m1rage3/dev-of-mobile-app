package com.example.lab4sqlite

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(
    private val onClick: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private var notes: List<Note> = emptyList()
    private var selectedId: Int? = null
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvItemTitle)
        val tvContent: TextView = itemView.findViewById(R.id.tvItemContent)
        val tvDate: TextView = itemView.findViewById(R.id.tvItemDate)
    }

    fun submit(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    fun setSelected(id: Int?) {
        selectedId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.tvTitle.text = note.title
        holder.tvContent.text = note.content
        holder.tvContent.visibility = if (note.content.isEmpty()) View.GONE else View.VISIBLE
        holder.tvDate.text = dateFormat.format(Date(note.timestamp))

        // выбранную заметку подсвечиваем
        holder.itemView.setBackgroundColor(
            if (note.id == selectedId) Color.parseColor("#332196F3") else Color.TRANSPARENT
        )
        holder.itemView.setOnClickListener { onClick(note) }
    }

    override fun getItemCount() = notes.size
}
