# Gothic Browser (Android)

A starter Android Studio project for a black-and-white Gothic mobile browser, using Mozilla GeckoView rather than Android WebView.

## Included
- Custom black-and-white loading screen
- GeckoView browsing engine and address/search bar
- Back, forward, reload and home controls
- Voice search using Android speech recognition (requires microphone permission and an available speech recognition service)
- Website audio/video playback through GeckoView
- Extension manager for Mozilla-signed, GeckoView-compatible `.xpi` extensions: install, list, enable, disable and uninstall
- Minimal dependencies

## Build
1. Install Android Studio (a recent version) and Android SDK Platform 35.
2. Open this folder in Android Studio.
3. Use JDK 17 (Android Studio's embedded JDK is suitable).
4. Let Gradle sync. An internet connection is required to download GeckoView from Mozilla's Maven repository.
5. Connect an Android phone with USB debugging enabled, or create an emulator, then press Run.
6. To create an APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

## Extension notes
GeckoView does not run every Chrome extension. Install only extensions signed by Mozilla and compatible with GeckoView/Android. Use a direct HTTPS `.xpi` URL. The app asks GeckoView to perform the installation; availability depends on the extension and GeckoView API version. Never install extensions you do not trust.

## Important current limitations
This is a starter project, not a fully production-ready browser. The tab button is currently a simple counter, and bookmarks/history/download handling, private-session isolation, and a full permission-confirmation UI need further implementation. Website audio/video works through GeckoView's media engine; voice search uses Android's speech recognizer. Websites asking to use their own microphone may require a GeckoView permission delegate before every site can use it.

## Tech
- Java 17
- Android Gradle Plugin 8.9.2
- GeckoView `158.0.20261021001945`
- Package: `com.daiki.gothicbrowser`

Official GeckoView docs:
- https://mozilla.github.io/geckoview/consumer/docs/
- https://mozilla.github.io/geckoview/consumer/docs/web-extensions
