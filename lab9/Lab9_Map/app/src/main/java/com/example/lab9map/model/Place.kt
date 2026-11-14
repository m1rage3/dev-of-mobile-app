package com.example.lab9map.model

// В модели храним простые latitude/longitude, а Point создаём только в слое карты
data class Place(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val isUserPlace: Boolean = false
)
