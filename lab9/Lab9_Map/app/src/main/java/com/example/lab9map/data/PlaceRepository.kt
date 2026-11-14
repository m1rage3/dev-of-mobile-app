package com.example.lab9map.data

import android.content.Context
import com.example.lab9map.model.Place

// сохранение отделено от UI: Activity работает только с Place
class PlaceRepository(context: Context) {

    private val dao = PlaceDatabase.get(context).placeDao()

    suspend fun getUserPlaces(): List<Place> =
        dao.getAll().map {
            Place(it.id, it.name, it.latitude, it.longitude, it.description, isUserPlace = true)
        }

    // возвращает место с id, который выдала база
    suspend fun addUserPlace(place: Place): Place {
        val id = dao.insert(
            PlaceEntity(
                name = place.name,
                latitude = place.latitude,
                longitude = place.longitude,
                description = place.description
            )
        )
        return place.copy(id = id, isUserPlace = true)
    }

    suspend fun deleteUserPlace(place: Place) {
        dao.deleteById(place.id)
    }
}
