package com.example.lab8weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab8weather.data.InvalidDataException
import com.example.lab8weather.data.WeatherApi
import com.example.lab8weather.data.WeatherRepository
import com.example.lab8weather.mapping.DailyUiModel
import com.example.lab8weather.mapping.WeatherMapper
import com.example.lab8weather.mapping.WeatherUiModel
import com.google.gson.JsonParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class City(val name: String, val lat: Double, val lon: Double)

val cities = listOf(
    City("Новосибирск", 55.03, 82.92),
    City("Москва", 55.75, 37.62),
    City("Санкт-Петербург", 59.94, 30.31),
    City("Красноярск", 56.01, 92.87)
)

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(
        val cityName: String,
        val model: WeatherUiModel,          // уже промапленная модель, не сырой ответ
        val days: List<DailyUiModel>
    ) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class WeatherViewModel : ViewModel() {

    private val repository = WeatherRepository(WeatherApi.service)

    private val _state = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    private var currentCity: City? = null

    // force = true — кнопка «Повторить»; иначе тот же город не грузим повторно
    // (например, после поворота экрана Spinner снова сообщает о выборе)
    fun loadCity(city: City, force: Boolean = false) {
        if (!force && city == currentCity && _state.value !is WeatherUiState.Error) return
        currentCity = city

        _state.value = WeatherUiState.Loading
        viewModelScope.launch {
            val result = repository.getForecast(city.lat, city.lon)
            _state.value = result.fold(
                onSuccess = { raw ->
                    WeatherUiState.Success(
                        cityName = city.name,
                        model = WeatherMapper.toUiModel(raw.current_weather!!),
                        days = WeatherMapper.toDailyList(raw.daily)
                    )
                },
                onFailure = { e -> WeatherUiState.Error(errorMessage(e)) }
            )
        }
    }

    // отличаем «нет сети» от «сервер вернул что-то не то»
    private fun errorMessage(e: Throwable): String = when (e) {
        is IOException -> "Нет подключения к интернету. Проверьте соединение."
        is HttpException -> "Сервер вернул ошибку ${e.code()}"
        is InvalidDataException, is JsonParseException, is IllegalStateException ->
            "Сервер вернул некорректные данные"
        else -> "Не удалось загрузить погоду"
    }
}
