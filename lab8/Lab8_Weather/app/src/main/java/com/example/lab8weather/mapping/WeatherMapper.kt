package com.example.lab8weather.mapping

import android.util.Log
import com.example.lab8weather.data.CurrentWeather
import com.example.lab8weather.data.Daily
import java.text.SimpleDateFormat
import java.util.Locale

// Модели для экрана: тут уже нет «сырых» кодов, только текст и эмодзи
data class WeatherUiModel(
    val temperature: Double,
    val windSpeed: Double,
    val condition: String,   // «Ясно», «Дождь»
    val emoji: String,       // «☀️», «🌧»
    val code: Int
)

data class DailyUiModel(
    val date: String,        // «пн, 17 нояб.»
    val emoji: String,
    val condition: String,
    val tempMin: Double,
    val tempMax: Double
)

// Слой маппинга: код погоды WMO -> описание и эмодзи
object WeatherMapper {

    private const val TAG = "WeatherMapper"

    private val codeToInfo = mapOf(
        0 to ("Ясно" to "☀️"),
        1 to ("Преимущественно ясно" to "🌤"),
        2 to ("Переменная облачность" to "⛅"),
        3 to ("Пасмурно" to "☁️"),
        45 to ("Туман" to "🌫"),
        48 to ("Туман с изморозью" to "🌫"),
        51 to ("Слабая морось" to "🌦"),
        53 to ("Морось" to "🌦"),
        55 to ("Сильная морось" to "🌧"),
        56 to ("Ледяная морось" to "🌧"),
        57 to ("Сильная ледяная морось" to "🌧"),
        61 to ("Слабый дождь" to "🌦"),
        63 to ("Дождь" to "🌧"),
        65 to ("Сильный дождь" to "🌧"),
        66 to ("Ледяной дождь" to "🌧"),
        67 to ("Сильный ледяной дождь" to "🌧"),
        71 to ("Слабый снег" to "🌨"),
        73 to ("Снег" to "🌨"),
        75 to ("Сильный снег" to "❄️"),
        77 to ("Снежная крупа" to "🌨"),
        80 to ("Слабый ливень" to "🌦"),
        81 to ("Ливень" to "🌧"),
        82 to ("Сильный ливень" to "⛈"),
        85 to ("Снегопад" to "🌨"),
        86 to ("Сильный снегопад" to "❄️"),
        95 to ("Гроза" to "⛈"),
        96 to ("Гроза с градом" to "⛈"),
        99 to ("Сильная гроза с градом" to "⛈")
    )

    // ветка по умолчанию: незнакомый код не роняет приложение
    private fun info(code: Int): Pair<String, String> =
        codeToInfo[code] ?: ("Неизвестно" to "🌌")

    fun toUiModel(raw: CurrentWeather): WeatherUiModel {
        val (condition, emoji) = info(raw.weathercode)
        Log.d(TAG, "weathercode=${raw.weathercode} -> $emoji $condition")
        return WeatherUiModel(
            temperature = raw.temperature,
            windSpeed = raw.windspeed,
            condition = condition,
            emoji = emoji,
            code = raw.weathercode
        )
    }

    fun toDailyList(daily: Daily?): List<DailyUiModel> {
        val dates = daily?.time ?: return emptyList()
        val max = daily.temperature_2m_max ?: return emptyList()
        val min = daily.temperature_2m_min ?: return emptyList()
        val codes = daily.weathercode ?: emptyList()

        val count = minOf(dates.size, max.size, min.size)
        return (0 until count).map { i ->
            val (condition, emoji) = info(codes.getOrElse(i) { -1 })
            DailyUiModel(
                date = formatDate(dates[i]),
                emoji = emoji,
                condition = condition,
                tempMin = min[i],
                tempMax = max[i]
            )
        }
    }

    private val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val outputFormat = SimpleDateFormat("EE, d MMM", Locale("ru"))

    private fun formatDate(raw: String): String = try {
        inputFormat.parse(raw)?.let { outputFormat.format(it) } ?: raw
    } catch (e: Exception) {
        raw
    }
}
