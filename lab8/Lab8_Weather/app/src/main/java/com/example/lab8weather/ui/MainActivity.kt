package com.example.lab8weather.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab8weather.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Locale

// Activity только показывает готовое состояние — никакой логики кодов погоды здесь нет
class MainActivity : AppCompatActivity() {

    private val viewModel: WeatherViewModel by viewModels()

    private lateinit var rootView: View
    private lateinit var spinner: Spinner
    private lateinit var progressBar: ProgressBar
    private lateinit var weatherCard: View
    private lateinit var tvCity: TextView
    private lateinit var tvEmoji: TextView
    private lateinit var tvCondition: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvForecastTitle: TextView
    private lateinit var tvError: TextView
    private val dailyAdapter = DailyAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootView = findViewById(R.id.root)
        spinner = findViewById(R.id.citySpinner)
        progressBar = findViewById(R.id.progressBar)
        weatherCard = findViewById(R.id.weatherCard)
        tvCity = findViewById(R.id.tvCity)
        tvEmoji = findViewById(R.id.tvEmoji)
        tvCondition = findViewById(R.id.tvCondition)
        tvTemp = findViewById(R.id.tvTemp)
        tvWind = findViewById(R.id.tvWind)
        tvForecastTitle = findViewById(R.id.tvForecastTitle)
        tvError = findViewById(R.id.tvError)

        findViewById<RecyclerView>(R.id.rvDaily).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = dailyAdapter
        }

        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, cities.map { it.name }
        )
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.loadCity(cities[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // подписка на состояние только пока экран виден
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: WeatherUiState) {
        when (state) {
            is WeatherUiState.Loading -> {
                progressBar.visibility = View.VISIBLE
                weatherCard.visibility = View.GONE
                tvForecastTitle.visibility = View.GONE
                tvError.visibility = View.GONE
                dailyAdapter.submit(emptyList())
            }
            is WeatherUiState.Success -> {
                progressBar.visibility = View.GONE
                tvError.visibility = View.GONE
                weatherCard.visibility = View.VISIBLE
                tvCity.text = state.cityName
                tvEmoji.text = state.model.emoji
                tvCondition.text = state.model.condition
                tvTemp.text = String.format(Locale.getDefault(), "%.1f°C", state.model.temperature)
                tvWind.text = String.format(Locale.getDefault(), "Ветер: %.1f км/ч", state.model.windSpeed)
                tvForecastTitle.visibility = if (state.days.isEmpty()) View.GONE else View.VISIBLE
                dailyAdapter.submit(state.days)
            }
            is WeatherUiState.Error -> {
                progressBar.visibility = View.GONE
                weatherCard.visibility = View.GONE
                tvForecastTitle.visibility = View.GONE
                tvError.visibility = View.VISIBLE
                tvError.text = state.message
                dailyAdapter.submit(emptyList())
                Snackbar.make(rootView, state.message, Snackbar.LENGTH_LONG)
                    .setAction("Повторить") {
                        viewModel.loadCity(cities[spinner.selectedItemPosition], force = true)
                    }
                    .show()
            }
        }
    }
}
