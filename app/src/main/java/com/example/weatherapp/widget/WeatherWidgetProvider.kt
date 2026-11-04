package com.example.weatherapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.weatherapp.R
import com.example.weatherapp.data.LocationPrefs
import com.example.weatherapp.data.WeatherApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class WeatherWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.d("Updating ${appWidgetIds.size} home screen widget(s)")
        val pendingResult = goAsync()

        scope.launch {
            try {
                val saved = LocationPrefs.getLocation(context)
                val api = WeatherApi.create()
                val weather = api.getCurrentWeather(lat = saved.lat, lon = saved.lon).currentWeather

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.weather_widget)
                    views.setTextViewText(R.id.tvWidgetCity, saved.name)
                    views.setTextViewText(
                        R.id.tvWidgetTemp,
                        context.getString(R.string.temperature_format, weather.temperature)
                    )

                    val intent = Intent(context, WeatherWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        appWidgetId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                Timber.i("Widget successfully updated for ${saved.name}: ${weather.temperature}°C")
            } catch (e: Exception) {
                Timber.e(e, "Error updating weather widget")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
