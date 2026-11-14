package com.example.lab8weather.data

// Модели повторяют JSON от Open-Meteo, поэтому имена полей как в ответе сервера.
// Поля-объекты nullable: если сервер пришлёт неполный ответ, проверим это в репозитории.

data class WeatherResponse(
    val current_weather: CurrentWeather?,
    val daily: Daily?
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int
)

data class Daily(
    val time: List<String>?,
    val weathercode: List<Int>?,
    val temperature_2m_max: List<Double>?,
    val temperature_2m_min: List<Double>?
)
