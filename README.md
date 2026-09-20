# Android Attendance App

## Requirements

- Android Studio
- JDK 21 or a compatible Android Studio bundled JDK
- Android SDK
- Gradle Wrapper (Gradle 8.7)

The project uses the Gradle Wrapper, so Gradle does not need to be installed separately.

## Technology / Architecture

-   Kotlin
-   Jetpack Compose + Material 3
-   MVVM architecture
-   Hilt for dependency injection
-   Room database for local storage
-   Coroutines and Flow
-   Navigation Compose
-   CameraX for camera and selfie capture
-   ML Kit Face Detection
-   TensorFlow Lite with MobileFaceNet for face embeddings
-   Location Provider for latitude and longitude

The app uses a local database and keeps the main logic separated into
ViewModels, repositories, and reusable services.

## How to Run the App

1.  Open the project in Android Studio.
2.  Let Gradle sync and build the project.
3.  Make sure `mobilefacenet.tflite` is present in
    `app/src/main/assets/`.
4.  Run the app on an Android device or emulator.
5.  Allow Camera and Location permissions when requested.
6.  Use the demo credentials below to login.

For face recognition, a real camera/device is recommended.

## Assumptions / Limitations

-   This is a local Android application and does not use a backend
    server.
-   The app is designed for a shared/single-device.
-   Demo staff accounts are provided for testing.
-   Staff added from the Admin screen do not automatically get a login
    account.
-   Attendance is limited to one successful check-in per staff member
    per day.
-   Attendance is blocked if a valid location cannot be obtained.
-   Face recognition is done on-device using TensorFlow Lite.
-   The face recognition threshold is a fixed starting value and can be
    tuned with more real-world test data.
-   The app does not include liveness or anti-spoofing detection.
-   Face recognition can be affected by lighting, camera quality, face
    angle, and other real-world conditions.

## Demo Credentials

### Admin

-   Username: `admin`
-   Password: `admin123`

### Staff 1

-   Username: `staff1`
-   Password: `staff123`

### Staff 2

-   Username: `staff2`
-   Password: `staff123`
