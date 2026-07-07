# §14 Akzeptanzkriterien — Durchsprache

Status je Kriterium und wo es umgesetzt/abgesichert ist. Hinweis: In der Build-Umgebung standen
kein JDK/Android-SDK/Gradle zur Verfügung, daher wurde **nicht** kompiliert/emuliert. Die
Domain-Logik (§14.1–4) ist über ausführbare Unit-Tests mit fixer Clock spezifiziert; der Rest ist
per Code-Review gegen die SPEC abgesichert.

| # | Kriterium | Umsetzung | Status |
|---|-----------|-----------|--------|
| 1 | Leer + Tap 08:00 → genau ein CLOCK_IN, "arbeitet" | `PunchUseCase` IDLE→[CLOCK_IN]; Tile→`Puncher.fire` | ✓ Unit-Test `leer_tap_0800_…` |
| 2 | Tap 12:00 WORKING → BREAK_START; erneut → BREAK_END, Pause steigt | WORKING(¬Abend)→BREAK_START, ON_BREAK→BREAK_END; `summarize` Pause | ✓ Unit-Test `tap_1200_…` |
| 3 | Abend WORKING → CLOCK_OUT; DONE; weiterer Tap NoOp | WORKING∧Abend→CLOCK_OUT; DONE∧¬force→[] | ✓ Unit-Test `tap_abend_working_…` |
| 4 | Abend ON_BREAK → BREAK_END+CLOCK_OUT, Reihenfolge, worked ohne Pause | ON_BREAK∧Abend→[BREAK_END,CLOCK_OUT]; Tie-Break id ASC | ✓ Unit-Test `tap_abend_on_break_…` |
| 5 | Auto-Close 20:15 offen → auto=true; History markiert; zu → nichts | `AutoCloseReceiver` (goAsync+Coroutine), `auto=true`, History `auto`-Chip | ✓ Code (nicht laufzeit-getestet) |
| 6 | Reminder nur wenn bis morningEnd kein CLOCK_IN | `ReminderReceiver`: `!clockedInToday && now≥morningEnd` | ✓ Code |
| 7 | Widget/Tile/Shortcut identisch | Alle → `PunchUseCase.punch()` | ✓ Code |
| 8 | Prozess-Kill zwischen Taps ändert nichts | Kein In-Memory-State; Ableitung aus DB | ✓ Design |
| 9 | Reboot → beide Alarme neu geplant | `BootReceiver`→`rescheduleAll` | ✓ Code (Caveat s. u.) |
| 10 | DST-Tag: Summen plausibel, nichts negativ/doppelt | Alles `Instant`; `Duration.between`; offene Intervalle gekappt | ✓ Design |
| 11 | History-Edit rechnet Summen neu | `editTime`→`update`→`observeAll`→`summarize` | ✓ Code |
| 12 | CSV-Export öffnet Share-Sheet mit gültiger Datei | `CsvExporter`+FileProvider (cacheDir) | ✓ Code |
| 13 | Zeitfenster sofort wirksam inkl. Alarm-Reschedule | Setter→`rescheduleAll`; VMs beobachten `settings.flow` | ✓ Code |

## Abweichungen / Caveats

1. **Kein Build/Run in dieser Umgebung** (kein JDK/SDK). Verifikation per Review + Domain-Unit-Tests
   (geschrieben, nicht ausgeführt).
2. **`gradle-wrapper.jar` fehlt** (Binär nicht erzeugbar) — einmalig `gradle wrapper` bzw. Android
   Studio nötig.
3. **Feste Versionen** statt „latest per Gradle": bewusst gepinnte, bekannte stable Versionen im
   Version-Catalog (reproduzierbar) statt dynamischer `+`-Ranges.
4. **`LOCKED_BOOT_COMPLETED`**: DataStore (credential-encrypted) ist vor dem Entsperren nicht lesbar;
   die effektive Neuplanung greift spätestens bei `BOOT_COMPLETED`. Kriterium 9 bleibt erfüllt.
5. **Reminder-Grenze**: Auslösung bei `now ≥ morningEnd` (inkl. exakt morningEnd), SPEC formuliert
   „nach morningEnd".
6. **Home-Tagesgrenze**: `observeToday(now)` fixiert die Grenzen bei VM-Erzeugung; über Mitternacht
   im Vordergrund minimal veraltet bis Neuaufbau.
7. **Exact-Alarm ohne Recht**: Fallback auf `setAndAllowWhileIdle` (best effort) + Hinweis-Card — wie
   in der SPEC vorgesehen.
