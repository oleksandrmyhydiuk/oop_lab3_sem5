package com.example.weatherapp.data

import android.content.Context
import com.example.weatherapp.R

data class SavedLocation(
    val name: String,
    val lat: Double,
    val lon: Double
)

object LocationPrefs {
    private const val PREFS_NAME = "weather_location_prefs"
    private const val KEY_NAME = "city_name"
    private const val KEY_LAT = "latitude"
    private const val KEY_LON = "longitude"

    fun saveLocation(context: Context, name: String, lat: Double, lon: Double) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_NAME, name)
            putString(KEY_LAT, lat.toString())
            putString(KEY_LON, lon.toString())
            apply()
        }
    }

    fun getLocation(context: Context): SavedLocation {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultCity = context.getString(R.string.city_kyiv)
        val name = prefs.getString(KEY_NAME, defaultCity) ?: defaultCity
        val lat = prefs.getString(KEY_LAT, "50.4501")?.toDoubleOrNull() ?: 50.4501
        val lon = prefs.getString(KEY_LON, "30.5234")?.toDoubleOrNull() ?: 30.5234
        return SavedLocation(name, lat, lon)
    }
}
