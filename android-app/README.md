# Pterodactyl Mobile

A small Android WebView client for an existing Pterodactyl Panel. The app delegates
login, server controls, console, and file management to the Panel's own mobile UI; it
does not store a separate Panel API key.

The initial Panel URL is `https://panel2.vannilasmp.my.id`. Use the URL button in the
app to configure a different HTTPS Panel URL after moving to a VPS.

## Build

Install Android SDK Platform 36 and use the included Gradle wrapper:

```sh
./gradlew assembleDebug
```

The test APK is written to `app/build/outputs/apk/debug/app-debug.apk`. It is debug
signed for sideload testing and is not a Play Store release.
