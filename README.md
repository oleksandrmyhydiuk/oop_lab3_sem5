# Weather Tracker — Android Application

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-blue)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-blue)](https://developer.android.com/about/versions/14)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Weather Tracker** is a native Android application written in **Kotlin** that provides real-time weather forecasts, city search via geocoding, device GPS location detection, runtime multi-language support (**English** and **Ukrainian**), structured logging with **Timber**, and an interactive **Home Screen App Widget**.

---

## Table of Contents

- [Key Features](#key-features)
- [Tech Stack & Libraries](#tech-stack--libraries)
- [Architecture & Project Structure](#architecture--project-structure)
- [API Integration](#api-integration)
- [Structured Logging Strategy](#structured-logging-strategy)
- [Internationalization (i18n)](#internationalization-i18n)
- [Home Screen Widget](#home-screen-widget)
- [Getting Started](#getting-started)
- [Permissions](#permissions)
- [Security & License](#security--license)

---

## Key Features

- **Real-Time Weather Forecast**: Displays current temperature (°C) and wind speed (km/h) using the Open-Meteo Forecast API.
- **Global City Search (Geocoding)**: Search for any city worldwide by name in either English or Ukrainian via the Open-Meteo Geocoding API.
- **GPS Location Detection**: Automatically resolves current device coordinates using Google Play Services `FusedLocationProviderClient` and performs reverse geocoding via `android.location.Geocoder`.
- **Persistent Location Storage**: Saves the last selected city and coordinates in `SharedPreferences` (`LocationPrefs`) so the main screen and home widget stay synchronized across app restarts.
- **Dynamic Multi-Language Support (i18n)**: Full localization in **English (`en`)** and **Ukrainian (`uk`)** with instant in-app switching powered by `AppCompatDelegate.setApplicationLocales` and Android 13+ per-app language preferences (`locale_config.xml`).
- **Home Screen Weather Widget**: Resizable Android `AppWidgetProvider` (`WeatherWidgetProvider`) with background coroutine updates (`goAsync()`), automatic periodic refresh, tap-to-refresh support, and instant sync when the location changes inside the app.
- **Build-Aware Structured Logging**: Configured with **Timber** (`DebugTree` with file/line/method tags in debug builds, and `ReleaseTree` filtering out verbose/debug/info logs in release builds) plus **OkHttp `HttpLoggingInterceptor`** for network diagnostics.

---

## Tech Stack & Libraries

| Category | Technology / Library | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | `1.9.22` | Primary development language (JVM Target 17) |
| **UI Layer** | AndroidX AppCompat, Material 3, ConstraintLayout, ViewBinding | `1.6.1` / `1.11.0` | Type-safe view binding and Material Design components |
| **Concurrency** | Kotlin Coroutines (`lifecycle-runtime-ktx`, `kotlinx-coroutines-play-services`) | `2.7.0` / `1.7.3` | Asynchronous API calls, GPS tasks, and widget updates |
| **Networking** | Retrofit 2 + Gson Converter | `2.9.0` | REST API client and JSON serialization |
| **HTTP Client** | OkHttp 3 + `logging-interceptor` | `4.12.0` | Underlying HTTP engine and network traffic logging |
| **Location** | Google Play Services Location | `21.2.0` | High-efficiency GPS location retrieval |
| **Logging** | Jake Wharton's Timber | `5.0.1` | Extensible, build-type-aware logging framework |

---

## Architecture & Project Structure

The project follows a clean, modular package organization separating application initialization, data access, UI controllers, and home screen widget components:

```text
WeatherApp/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/weatherapp/
│       │   ├── WeatherApp.kt                  # Application class: initializes Timber DebugTree / ReleaseTree
│       │   ├── data/
│       │   │   ├── LocationPrefs.kt           # SharedPreferences wrapper for persisted city & coordinates
│       │   │   ├── WeatherApi.kt              # Retrofit interface & OkHttp client factory
│       │   │   └── WeatherResponse.kt         # DTOs for Weather & Geocoding API responses
│       │   ├── ui/
│       │   │   └── MainActivity.kt            # Main screen UI, permissions, GPS, search, and locale switching
│       │   └── widget/
│       │       └── WeatherWidgetProvider.kt   # AppWidgetProvider for the home screen weather widget
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml          # Main activity layout
│           │   └── weather_widget.xml         # RemoteViews layout for the home screen widget
│           ├── values/
│           │   ├── strings.xml                # Default (English) string resources
│           │   └── themes.xml                 # Material 3 application theme
│           ├── values-uk/
│           │   └── strings.xml                # Ukrainian string resources
│           └── xml/
│               ├── locale_config.xml          # Supported locales configuration (en, uk)
│               └── weather_widget_info.xml    # AppWidget metadata (dimensions, update period, resize mode)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .gitignore
├── LICENSE
├── SECURITY.md
└── README.md
```

---

## API Integration

Weather Tracker uses the free, open-source **[Open-Meteo API](https://open-meteo.com/)**, which requires no API key and communicates strictly over HTTPS:

1. **Weather Forecast Endpoint**
   - **Base URL**: `https://api.open-meteo.com/`
   - **Endpoint**: `GET /v1/forecast`
   - **Query Parameters**: `latitude`, `longitude`, `current_weather=true`
   - **Response Model**: `WeatherResponse` -> `CurrentWeather` (`temperature`, `windSpeed`, `weatherCode`)

2. **Geocoding Search Endpoint**
   - **URL**: `https://geocoding-api.open-meteo.com/v1/search`
   - **Query Parameters**: `name` (search query), `count=1`, `language` (`en` or `uk` matching active app locale)
   - **Response Model**: `GeocodingResponse` -> `List<GeocodingResult>` (`name`, `country`, `latitude`, `longitude`)

---

## Structured Logging Strategy

Logging is centrally configured in `WeatherApp.onCreate()` using **Timber**:

- **Debug Builds (`BuildConfig.DEBUG == true`)**:
  - Plants a custom `Timber.DebugTree` that formats log tags with the exact source location:
    ```text
    WeatherLog[(MainActivity.kt:187)#loadWeatherForSavedLocation]
    ```
  - Routes OkHttp network traffic (`HttpLoggingInterceptor.Level.BASIC`) into `Timber.tag("OkHttp")`.
- **Release Builds (`BuildConfig.DEBUG == false`)**:
  - Plants a custom `ReleaseTree` that suppresses `Log.VERBOSE`, `Log.DEBUG`, and `Log.INFO` messages to prevent leaking operational details in production logcat, forwarding only `Log.WARN` and `Log.ERROR` events.
- **Log Levels Used Across Components**:
  - `Timber.d()` — Fine-grained execution flow (API request parameters, widget update triggers, permission checks).
  - `Timber.i()` — Key lifecycle and business milestones (activity initialized, city resolved, weather updated, locale switched).
  - `Timber.w()` — Handled anomalies (location permission denied, empty geocoding results, fallback from reverse geocoder).
  - `Timber.e()` — Network or runtime exceptions with full `Throwable` stack traces.

---

## Internationalization (i18n)

The application is designed for seamless localization following Android best practices:

- **Resource Qualification**: All user-facing text and format patterns (`%1$.1f °C`, `%1$.1f km/h`) are externalized in `res/values/strings.xml` (English) and `res/values-uk/strings.xml` (Ukrainian).
- **Per-App Language Support**: Declared in `res/xml/locale_config.xml` and linked in `AndroidManifest.xml` via `android:localeConfig="@xml/locale_config"`, enabling native language selection in Android 13+ system settings.
- **In-App Language Switcher**: Users can toggle between English and Ukrainian at any time using the switch button in `MainActivity`, which invokes:
  ```kotlin
  AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLocale))
  ```
- **Locale-Aware Geocoding**: Both Open-Meteo city search and `android.location.Geocoder` reverse lookups automatically use the active language (`uk` or `en`).

---

## Home Screen Widget

`WeatherWidgetProvider` extends `AppWidgetProvider` to deliver glanceable weather updates directly on the user's home screen:

- **Metadata (`res/xml/weather_widget_info.xml`)**:
  - Minimum size: `180dp × 80dp`
  - Resize mode: `horizontal|vertical`
  - Periodic update interval: `1,800,000 ms` (30 minutes)
- **Asynchronous Updates**: Uses `BroadcastReceiver.goAsync()` combined with a `SupervisorJob` coroutine scope (`Dispatchers.IO`) to fetch the latest forecast from Open-Meteo without blocking the main thread.
- **Interactive Refresh & App Sync**:
  - Tapping the widget triggers an `ACTION_APPWIDGET_UPDATE` broadcast via an immutable `PendingIntent` (`FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`).
  - Changing or refreshing the city in `MainActivity` immediately broadcasts an update to all active widget instances.

---

## Getting Started

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** configured in Gradle settings
- **Android SDK 34** (compileSdk / targetSdk) and **Android SDK 26+** device or emulator

### Installation & Build

1. **Clone the repository**:
   ```bash
   git clone https://github.com/oleksandrmyhydiuk/oop_lab3_sem5.git
   cd oop_lab3_sem5
   ```
2. **Open in Android Studio**:
   - Select **File -> Open** and choose the project root directory.
   - Wait for Gradle sync to complete.
3. **Run the application**:
   - Select a connected Android device or emulator (API 26+) and click **Run ('app')**, or build via CLI:
     ```bash
     ./gradlew assembleDebug
     ```

---

## Permissions

Declared in `app/src/main/AndroidManifest.xml`:

| Permission | Purpose |
| :--- | :--- |
| `android.permission.INTERNET` | Required to fetch weather forecasts and geocoding data from Open-Meteo APIs. |
| `android.permission.ACCESS_COARSE_LOCATION` | Allows approximate device location detection when requested by the user. |
| `android.permission.ACCESS_FINE_LOCATION` | Allows precise GPS location detection when the user taps **"Use Current GPS Location"**. |

---

## Security & License

- Please review [SECURITY.md](SECURITY.md) for details on our security practices and how to report vulnerabilities.
- This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
