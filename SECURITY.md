# Security Policy

## Supported Versions

The following versions of **Weather Tracker** are currently supported with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Security Architecture & Practices

Weather Tracker implements several Android security best practices out of the box:

1. **Encrypted Network Traffic (HTTPS Only)**
   - All weather forecast (`https://api.open-meteo.com/`) and geocoding (`https://geocoding-api.open-meteo.com/`) requests are performed exclusively over TLS/HTTPS.
2. **No Hardcoded Secrets or API Keys**
   - The application integrates with the keyless Open-Meteo API and does not embed sensitive credentials in source code, resources, or `BuildConfig`.
   - Keystore files (`*.jks`, `*.keystore`) and local SDK configurations (`local.properties`) are excluded via `.gitignore`.
3. **Production-Safe Logging (`ReleaseTree`)**
   - In release builds (`!BuildConfig.DEBUG`), `WeatherApp` plants a restricted `ReleaseTree` that strips out `VERBOSE`, `DEBUG`, and `INFO` log messages so sensitive runtime data or coordinates are not exposed in production system logs.
4. **Principle of Least Privilege for Location Access**
   - Runtime permissions (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) are requested on demand only when the user explicitly interacts with the GPS location button, and the app gracefully falls back when permissions are denied.
5. **Component & PendingIntent Hardening**
   - `WeatherWidgetProvider` is configured with `android:exported="false"` in `AndroidManifest.xml` to prevent unauthorized external applications from spoofing widget broadcasts.
   - Widget click intents enforce `PendingIntent.FLAG_IMMUTABLE` to prevent intent mutability attacks on Android 12+ (API 31+).

## Reporting a Vulnerability

If you discover a security vulnerability within this project, please report it responsibly:

1. **Do not** open a public GitHub issue for security vulnerabilities.
2. Send a detailed report via GitHub private vulnerability reporting or contact the repository maintainer (**@oleksandrmyhydiuk**).
3. Include steps to reproduce, affected Android OS versions, and potential impact.

You can expect an initial acknowledgment within **48 hours** and a resolution plan or patch within **7 days**.
