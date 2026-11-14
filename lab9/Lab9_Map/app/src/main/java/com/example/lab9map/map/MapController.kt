package com.example.lab9map.map

import android.content.Context
import android.graphics.PointF
import android.util.Log
import com.example.lab9map.R
import com.example.lab9map.model.Place
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.TextStyle
import com.yandex.runtime.image.ImageProvider

// Вся работа с картой — здесь, Activity только показывает диалоги
class MapController(
    context: Context,
    private val map: Map,
    private val onPlaceTap: (Place) -> Unit,
    private val onLongTap: (latitude: Double, longitude: Double) -> Unit
) {
    companion object {
        private const val TAG = "MapController"
        val NOVOSIBIRSK = Point(55.0084, 82.9357)
        const val DEFAULT_ZOOM = 12f
    }

    private val defaultIcon = ImageProvider.fromResource(context, R.drawable.pin_default)
    private val userIcon = ImageProvider.fromResource(context, R.drawable.pin_user)

    // id пользовательского места -> его метка (чтобы можно было удалить)
    private val userPlacemarks = mutableMapOf<Long, PlacemarkMapObject>()

    // ВАЖНО: MapKit хранит слушателей через WeakReference.
    // Держим их в полях, иначе сборщик мусора их удалит и клики перестанут работать.
    private val placemarkTapListener = MapObjectTapListener { mapObject, _ ->
        val place = mapObject.userData as? Place ?: return@MapObjectTapListener false
        Log.d(TAG, "Клик по метке: ${place.name}")
        onPlaceTap(place)
        true
    }

    private val inputListener = object : InputListener {
        override fun onMapTap(map: Map, point: Point) {
            Log.d(TAG, "onMapTap: ${point.latitude}, ${point.longitude}")
        }

        override fun onMapLongTap(map: Map, point: Point) {
            Log.d(TAG, "onMapLongTap: ${point.latitude}, ${point.longitude}")
            onLongTap(point.latitude, point.longitude)
        }
    }

    init {
        map.addInputListener(inputListener)
    }

    fun moveCamera(target: Point = NOVOSIBIRSK, zoom: Float = DEFAULT_ZOOM, azimuth: Float = 0f, tilt: Float = 0f) {
        map.move(CameraPosition(target, zoom, azimuth, tilt))
    }

    val cameraPosition: CameraPosition get() = map.cameraPosition

    fun addPlace(place: Place) {
        val placemark = map.mapObjects.addPlacemark().apply {
            geometry = Point(place.latitude, place.longitude)
            // предзаданные — синие, пользовательские — оранжевые
            setIcon(
                if (place.isUserPlace) userIcon else defaultIcon,
                IconStyle().apply {
                    anchor = PointF(0.5f, 1.0f)
                    scale = 1f
                }
            )
            setText(
                place.name,
                TextStyle().apply {
                    size = 11f
                    placement = TextStyle.Placement.BOTTOM
                    offset = 2f
                }
            )
            userData = place // в обработчике клика сразу получим Place
            addTapListener(placemarkTapListener)
        }
        if (place.isUserPlace) userPlacemarks[place.id] = placemark
    }

    fun removeUserPlace(place: Place) {
        userPlacemarks.remove(place.id)?.let { map.mapObjects.remove(it) }
    }

    fun clear() {
        map.mapObjects.clear()
        userPlacemarks.clear()
    }

    fun release() {
        map.removeInputListener(inputListener)
    }
}
