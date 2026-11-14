package com.example.lab9map

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lab9map.data.PlaceRepository
import com.example.lab9map.map.MapController
import com.example.lab9map.model.DefaultPlaces
import com.example.lab9map.model.Place
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private var mapController: MapController? = null
    private lateinit var repository: PlaceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // без ключа MapKit не запустится — показываем понятную подсказку вместо краша
        if (BuildConfig.MAPKIT_API_KEY.isBlank()) {
            setContentView(R.layout.activity_no_key)
            return
        }

        // initialize — до setContentView
        MapKitFactory.initialize(this)
        setContentView(R.layout.activity_main)

        repository = PlaceRepository(this)
        val view = findViewById<MapView>(R.id.mapView)
        mapView = view

        val controller = MapController(
            context = this,
            map = view.mapWindow.map,
            onPlaceTap = { place -> showPlaceCard(place) },
            onLongTap = { lat, lon -> showAddPlaceDialog(lat, lon) }
        )
        mapController = controller

        // после поворота возвращаем камеру туда, где она была
        if (savedInstanceState != null) {
            controller.moveCamera(
                target = Point(
                    savedInstanceState.getDouble(KEY_LAT),
                    savedInstanceState.getDouble(KEY_LON)
                ),
                zoom = savedInstanceState.getFloat(KEY_ZOOM),
                azimuth = savedInstanceState.getFloat(KEY_AZIMUTH),
                tilt = savedInstanceState.getFloat(KEY_TILT)
            )
        } else {
            controller.moveCamera() // Новосибирск, зум 12
        }

        DefaultPlaces.list.forEach { controller.addPlace(it) }
        loadUserPlaces()
    }

    // пользовательские метки восстанавливаем из базы (и после перезапуска, и после поворота)
    private fun loadUserPlaces() {
        lifecycleScope.launch {
            val places = repository.getUserPlaces()
            Log.d(TAG, "Восстановлено пользовательских меток: ${places.size}")
            places.forEach { mapController?.addPlace(it) }
        }
    }

    private fun showPlaceCard(place: Place) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_place, null)
        view.findViewById<TextView>(R.id.tvPlaceName).text = place.name
        view.findViewById<TextView>(R.id.tvPlaceDescription).text =
            place.description.ifBlank { "Без описания" }
        view.findViewById<TextView>(R.id.tvPlaceCoords).text =
            String.format(Locale.US, "%.5f, %.5f", place.latitude, place.longitude)
        view.findViewById<TextView>(R.id.tvPlaceType).text =
            if (place.isUserPlace) "Моя метка" else "Предзаданная точка"

        val btnDelete = view.findViewById<Button>(R.id.btnDeletePlace)
        if (place.isUserPlace) {
            btnDelete.setOnClickListener {
                lifecycleScope.launch {
                    repository.deleteUserPlace(place)
                    mapController?.removeUserPlace(place)
                    Log.d(TAG, "Метка удалена: ${place.name}")
                }
                dialog.dismiss()
            }
        } else {
            btnDelete.visibility = android.view.View.GONE
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showAddPlaceDialog(lat: Double, lon: Double) {
        val padding = (16 * resources.displayMetrics.density).toInt()
        val nameInput = EditText(this).apply { hint = "Название" }
        val descriptionInput = EditText(this).apply { hint = "Описание (необязательно)" }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
            addView(nameInput)
            addView(descriptionInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Новая метка")
            .setMessage(String.format(Locale.US, "%.5f, %.5f", lat, lon))
            .setView(container)
            .setPositiveButton("Добавить") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val saved = repository.addUserPlace(
                        Place(
                            name = name,
                            latitude = lat,
                            longitude = lon,
                            description = descriptionInput.text.toString().trim(),
                            isUserPlace = true
                        )
                    )
                    Log.d(TAG, "Метка сохранена: ${saved.name} (id=${saved.id})")
                    mapController?.addPlace(saved)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapController?.cameraPosition?.let { pos ->
            outState.putDouble(KEY_LAT, pos.target.latitude)
            outState.putDouble(KEY_LON, pos.target.longitude)
            outState.putFloat(KEY_ZOOM, pos.zoom)
            outState.putFloat(KEY_AZIMUTH, pos.azimuth)
            outState.putFloat(KEY_TILT, pos.tilt)
        }
    }

    // события жизненного цикла обязательно передаём и в MapKitFactory, и в MapView
    override fun onStart() {
        super.onStart()
        if (mapView != null) {
            MapKitFactory.getInstance().onStart()
            mapView?.onStart()
        }
    }

    override fun onStop() {
        if (mapView != null) {
            mapView?.onStop()
            MapKitFactory.getInstance().onStop()
        }
        super.onStop()
    }

    override fun onDestroy() {
        mapController?.release()
        mapController = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val KEY_LAT = "camera_lat"
        private const val KEY_LON = "camera_lon"
        private const val KEY_ZOOM = "camera_zoom"
        private const val KEY_AZIMUTH = "camera_azimuth"
        private const val KEY_TILT = "camera_tilt"
    }
}
