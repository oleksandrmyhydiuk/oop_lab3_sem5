package com.example.weatherapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.example.weatherapp.R
import com.example.weatherapp.data.LocationPrefs
import com.example.weatherapp.data.WeatherApi
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.widget.WeatherWidgetProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val weatherApi by lazy { WeatherApi.create() }
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            Timber.i("Location permission granted by user")
            fetchGpsLocation()
        } else {
            Timber.w("Location permission denied by user")
            Toast.makeText(this, R.string.error_location_permission, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.i("MainActivity initialized. Active locale: ${getActiveLang()}")

        binding.btnSearchCity.setOnClickListener {
            val query = binding.etCitySearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchCityByName(query)
            }
        }

        binding.btnGpsLocation.setOnClickListener {
            checkPermissionsAndFetchLocation()
        }

        binding.btnRefresh.setOnClickListener {
            loadWeatherForSavedLocation()
        }

        binding.btnSwitchLang.setOnClickListener {
            toggleLanguage()
        }

        loadWeatherForSavedLocation()
    }

    private fun searchCityByName(query: String) {
        val langCode = if (getActiveLang().contains("uk")) "uk" else "en"
        Timber.d("Searching coordinates for city: '$query' (lang=$langCode)")

        lifecycleScope.launch {
            try {
                val response = weatherApi.searchCity(query = query, language = langCode)
                val match = response.results?.firstOrNull()

                if (match != null) {
                    val displayName = if (!match.country.isNullOrBlank()) {
                        "${match.name}, ${match.country}"
                    } else {
                        match.name
                    }
                    Timber.i("City resolved: $displayName (${match.latitude}, ${match.longitude})")
                    LocationPrefs.saveLocation(this@MainActivity, displayName, match.latitude, match.longitude)
                    loadWeatherForSavedLocation()
                } else {
                    Timber.w("No geocoding results found for query: '$query'")
                    Toast.makeText(this@MainActivity, R.string.error_city_not_found, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Geocoding API error while searching for '$query'")
                Toast.makeText(this@MainActivity, R.string.status_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionsAndFetchLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fetchGpsLocation()
        } else {
            Timber.d("Requesting location permissions from user")
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchGpsLocation() {
        binding.tvTemperature.text = getString(R.string.status_loading)
        lifecycleScope.launch {
            try {
                Timber.d("Requesting current GPS coordinates via FusedLocationProviderClient...")
                val tokenSource = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    tokenSource.token
                ).await()

                if (location != null) {
                    val cityName = resolveCityNameFromCoordinates(location.latitude, location.longitude)
                    Timber.i("GPS location obtained: $cityName (${location.latitude}, ${location.longitude})")
                    LocationPrefs.saveLocation(this@MainActivity, cityName, location.latitude, location.longitude)
                    loadWeatherForSavedLocation()
                } else {
                    Timber.w("FusedLocationProvider returned null location")
                    Toast.makeText(this@MainActivity, R.string.status_error, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to obtain GPS location")
                Toast.makeText(this@MainActivity, R.string.status_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun resolveCityNameFromCoordinates(lat: Double, lon: Double): String =
        withContext(Dispatchers.IO) {
            try {
                val locale = if (getActiveLang().contains("uk")) Locale("uk") else Locale.ENGLISH
                val geocoder = Geocoder(this@MainActivity, locale)
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                addresses?.firstOrNull()?.locality ?: getString(R.string.current_gps_location)
            } catch (e: Exception) {
                Timber.w(e, "Reverse geocoding failed, falling back to default GPS label")
                getString(R.string.current_gps_location)
            }
        }

    private fun loadWeatherForSavedLocation() {
        val saved = LocationPrefs.getLocation(this)
        binding.tvCity.text = saved.name
        binding.tvTemperature.text = getString(R.string.status_loading)

        lifecycleScope.launch {
            try {
                Timber.d("Fetching weather for ${saved.name} [lat=${saved.lat}, lon=${saved.lon}]")
                val response = weatherApi.getCurrentWeather(lat = saved.lat, lon = saved.lon)
                val current = response.currentWeather

                Timber.i("Weather updated for ${saved.name}: ${current.temperature}°C, wind=${current.windSpeed}km/h")

                binding.tvTemperature.text = getString(R.string.temperature_format, current.temperature)
                binding.tvWind.text = getString(R.string.wind_format, current.windSpeed)

                updateHomeWidget()
            } catch (e: Exception) {
                Timber.e(e, "Failed to load weather for ${saved.name}")
                binding.tvTemperature.text = getString(R.string.status_error)
                binding.tvWind.text = ""
            }
        }
    }

    private fun getActiveLang(): String {
        val appLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return appLocale.ifEmpty { Locale.getDefault().language }
    }

    private fun toggleLanguage() {
        val nextLocale = if (getActiveLang().contains("uk")) "en" else "uk"
        Timber.i("Switching language to '$nextLocale'")
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLocale))
    }

    private fun updateHomeWidget() {
        val intent = Intent(this, WeatherWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(application)
                .getAppWidgetIds(ComponentName(application, WeatherWidgetProvider::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(intent)
    }
}
