# Prebuilt APK

`worktracker-v1.0-debug.apk` — Debug-Build (debug-signiert), direkt auf einem Android-Gerät
installierbar (minSdk 26 / Android 8.0+).

- Gebaut aus dem jeweiligen Commit-Stand mit JDK 17 + Android SDK (compileSdk 35, build-tools 35.0.0).
- Verifiziert: `./gradlew testDebugUnitTest` → 4/4 grün (§14.1–4), `assembleDebug` erfolgreich.
- Prüfsumme siehe `.sha256`.

Installation: Datei aufs Gerät kopieren, antippen, „Aus unbekannten Quellen" erlauben — oder
`adb install worktracker-v1.0-debug.apk`.

Hinweis: Debug-APK, nicht für Play-Store-Verteilung gedacht. Für eine Release-Signatur einen eigenen
Keystore verwenden (niemals committen).
