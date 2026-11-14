package com.example.lab4sqlite

// Просто контейнер для данных, с базой напрямую не связан
data class Note(
    val id: Int,
    val title: String,
    val content: String,
    val timestamp: Long
)
