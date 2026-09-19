# LinkUp

**LinkUp** is an Android social discovery app that helps friends connect for spontaneous hangouts. Users can find nearby friends, send a LinkUp request, and start a temporary chat when someone accepts.

Developed as the **Team 4 — SENA** project for Northeastern University's Mobile Application Development course.

## Features

- Email and password authentication
- User profiles with profile photos and bios
- Friend search, requests, and friend management
- Nearby-friend discovery using device location
- Google Maps integration with distance-based markers
- Time-limited LinkUp requests
- Real-time one-to-one chat
- Chat and LinkUp history
- Background request monitoring and local notifications
- Password reset and profile editing

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java |
| UI | Android Views and XML layouts |
| Backend | Firebase Authentication, Cloud Firestore, Firebase Storage |
| Maps and location | Google Maps SDK for Android, Google Play Services Location |
| Image loading | Picasso |
| Build system | Gradle with Kotlin DSL |
| Minimum Android version | Android 8.1 / API 27 |
| Target SDK | API 36 |

## Project Structure

```text
app/src/main/
├── java/.../
│   ├── activities/   # Authentication, home, friends, chat, and profile screens
│   ├── adapters/     # RecyclerView adapters
│   ├── models/       # Application data models
│   ├── receivers/    # Boot receiver
│   ├── services/     # Background LinkUp listener
│   └── utils/        # Shared utilities
├── res/              # Layouts, drawables, strings, themes, and animations
└── AndroidManifest.xml
```

## Getting Started

### Prerequisites

- Android Studio
- JDK 11 or newer
- An Android emulator or physical device running API 27+
- A Firebase project
- A Google Cloud project with **Maps SDK for Android** enabled

### 1. Clone the repository

```bash
git clone https://github.com/srujankothuri/linkup-android.git
cd linkup-android
```

### 2. Configure Firebase

1. Create or open a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with this package name:

   ```text
   edu.northeastern.numad26sp_team4_sena
   ```

3. Download `google-services.json` and place it at:

   ```text
   app/google-services.json
   ```

4. Enable the Firebase services used by the app:

   - Email/Password Authentication
   - Cloud Firestore
   - Firebase Storage

### 3. Configure the Google Maps API key

Copy the example secrets file:

```bash
cp secrets.properties.example secrets.properties
```

Add your Maps API key to `secrets.properties`:

```properties
MAPS_API_KEY=your_restricted_api_key
```

For security, restrict the key in Google Cloud Console to:

- **Application restriction:** Android apps
- **Package name:** `edu.northeastern.numad26sp_team4_sena`
- **Certificate:** your debug or release SHA-1 fingerprint
- **API restriction:** Maps SDK for Android

To print the signing fingerprints:

```bash
./gradlew signingReport
```

> `secrets.properties` and `app/google-services.json` are ignored by Git and must never be committed.

Alternatively, provide the key through the `MAPS_API_KEY` environment variable.

### 4. Build and run

Open the project in Android Studio, allow Gradle to sync, select an emulator or connected device, and run the `app` configuration.

You can also build from the command line:

```bash
./gradlew assembleDebug
```

## Required Permissions

LinkUp requests permissions for:

- Precise or approximate location
- Camera access for profile photos
- Notifications
- Internet access
- Foreground-service execution
- Start-on-boot support for the LinkUp listener

Grant location and notification permissions when prompted for the complete experience.

## Security Notes

- Never commit API keys, Firebase configuration files, keystores, or generated APKs.
- A Maps key included in an Android build can be extracted from the APK, so Google Cloud package-name and SHA-1 restrictions are essential.
- Use separate restricted keys and Firebase projects for development and production.
- Review Firebase Security Rules before deploying the app outside a development environment.

## Testing

Run local unit tests:

```bash
./gradlew test
```

Run instrumented tests on a connected device or emulator:

```bash
./gradlew connectedAndroidTest
```

## Team

**Team 4 — SENA**

Built for educational purposes as a Northeastern University mobile application development project.
