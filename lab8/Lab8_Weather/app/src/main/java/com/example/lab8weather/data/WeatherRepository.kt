package com.example.lab8weather.data

import kotlinx.coroutines.CancellationException

// ответ пришёл, но в нём нет нужных данных
class InvalidDataException(message: String) : Exception(message)

// единая точка доступа к данным: наружу отдаём Result, а не исключения
class WeatherRepository(private val api: WeatherApiService) {

    suspend fun getForecast(lat: Double, lon: Double): Result<WeatherResponse> = try {
        val response = api.getForecast(lat, lon)
        if (response.current_weather == null) {
            Result.failure(InvalidDataException("В ответе нет current_weather"))
        } else {
            Result.success(response)
        }
    } catch (e: CancellationException) {
        throw e // отмену корутины не глушим
    } catch (e: Exception) {
        Result.failure(e)
    }
}
