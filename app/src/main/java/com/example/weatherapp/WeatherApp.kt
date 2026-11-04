package com.example.weatherapp

import android.app.Application
import android.util.Log
import timber.log.Timber

class WeatherApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return "WeatherLog[(${element.fileName}:${element.lineNumber})#${element.methodName}]"
                }
            })
            Timber.d("Application initialized in DEBUG mode")
        } else {
            Timber.plant(ReleaseTree())
        }
    }

    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
                return
            }
            Log.println(priority, tag ?: "WeatherRelease", message)
        }
    }
}
