# WorkTracker

Offline Single-User Arbeitszeit-Tracker für Android. Implementierung von [SPEC.md](SPEC.md).

## Bauen

```bash
# Gradle-Wrapper einmalig erzeugen (die gradle-wrapper.jar liegt bewusst nicht im Repo):
gradle wrapper --gradle-version 8.11.1

# danach
./gradlew :app:assembleDebug        # APK bauen
./gradlew :app:testDebugUnitTest    # Domain-Unit-Tests (§14.1–4)
```

Alternativ das Projekt direkt in **Android Studio** öffnen (Giraffe/Koala+), der Wrapper wird
automatisch ergänzt.

Voraussetzungen: JDK 17, Android SDK (compileSdk 35), minSdk 26.

## Architektur (Kurz)

- **domain** ist die einzige Wahrheit: Zustand wird nie persistiert, sondern per `stateFrom` /
  `summarize` aus den Events des lokalen Tages (Europe/Berlin) abgeleitet. `PunchUseCase.punch()`
  entscheidet in einem `Mutex`, was ein Tap bedeutet.
- **data** ist Room mit `Instant`-Persistenz (UTC) und Tie-Break `ts ASC, id ASC`.
- **trigger / widget / alarm** enden alle im selben `PunchUseCase.punch()`.
- Manuelle DI über `AppContainer` (kein Hilt).

## Sicherheit

Keine Secrets/Keystores im Repo (siehe `.gitignore`). Kein Netzwerk, keine Cloud, keine
Storage-Permission (CSV-Export via FileProvider aus dem Cache).
