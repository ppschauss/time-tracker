# SPEC — Arbeitszeit-Tracker (Android, offline, single-user)

Kein Backend, keine Cloud, keine Runtime-Permissions außer den in §12 gelisteten. Alles on-device.

## 1. Scope
- Single-User Arbeitszeit-Tracker, offline (Room/SQLite).
- Zentraler punch()-Use-Case entscheidet kontextabhängig, was ein Tap bedeutet.
- Auslöser ohne App-Öffnen: Quick-Settings-Tile, Home-Screen-Widget (Glance), App-Shortcut.
- Zeitfenster disambiguieren Stop-vs-Pause; Auto-Close + Morning-Reminder via AlarmManager.
- Kein Geofencing, kein Background-Location, kein Foreground-Service.

## 2. Stack
Kotlin, Compose (Material 3), min SDK 26. Room (Flow), Coroutines. Glance. DataStore (Preferences). AlarmManager (exact) statt WorkManager. Manuelle DI über AppContainer, kein Hilt. Gradle Kotlin DSL, Version Catalog libs.versions.toml, aktuelle stable Versionen per Gradle auflösen.

## 3. Package-Layout
de.pattaku.worktracker
- App.kt (Application + AppContainer)
- di/AppContainer.kt
- data/db/AppDatabase.kt, PunchDao.kt, Converters.kt
- data/model/Punch.kt, PunchKind.kt
- data/PunchRepository.kt
- domain/WorkState.kt, DaySummary.kt, TimeWindows.kt (dayBounds, inWindow, stateFrom, summarize), PunchUseCase.kt
- settings/SettingsRepository.kt, AppSettings.kt
- trigger/Puncher.kt, PunchTileService.kt, PunchTrampolineActivity.kt, ShortcutSetup.kt
- widget/PunchWidget.kt, PunchWidgetReceiver.kt, PunchAction.kt
- alarm/AlarmScheduler.kt, AutoCloseReceiver.kt, ReminderReceiver.kt, BootReceiver.kt
- export/CsvExporter.kt
- ui/MainActivity.kt, theme/, home/, history/, settings/ (je Screen + ViewModel)

## 4. Datenmodell
enum PunchKind { CLOCK_IN, BREAK_START, BREAK_END, CLOCK_OUT }
@Entity("punches") Punch(id Long autoGen, ts Instant [UTC], kind PunchKind, auto Boolean=false)
Converters: Instant<->Long(epochMilli), PunchKind<->String(name).
DAO — Tie-Break id ASC PFLICHT (BREAK_END+CLOCK_OUT teilen ts):
- insert/update/delete (suspend)
- between(from,to): List<Punch> ORDER BY ts ASC, id ASC
- observeBetween(from,to): Flow<List<Punch>> ORDER BY ts ASC, id ASC
- observeAll(): Flow<List<Punch>> ORDER BY ts DESC, id DESC

## 5. Domain — die eine Wahrheit
Zustand NIE persistiert, immer aus Events des lokalen Tages ableiten.
enum WorkState { IDLE, WORKING, ON_BREAK, DONE }
stateFrom(dayEvents): letztes Event -> null=IDLE; CLOCK_IN|BREAK_END=WORKING; BREAK_START=ON_BREAK; CLOCK_OUT=DONE.
ZONE=Europe/Berlin (DST-safe). dayBounds(now)=lokaler Tagesanfang/-ende als Instant. inWindow(now, from:LocalTime, to:LocalTime).
summarize(events, now) -> DaySummary(state, worked:Duration, pause:Duration, firstIn, lastOut): fold mit openWork/openBreak; CLOCK_IN öffnet work (firstIn); BREAK_START schließt work+öffnet break; BREAK_END schließt break+öffnet work; CLOCK_OUT schließt work (lastOut). Offene Intervalle bis now kappen.
PunchUseCase.punch(force=false) in Mutex.withLock: now=Instant.now(clock); events=repo.between(dayBounds); state=stateFrom; evening=inWindow(now, cfg.eveningStart, cfg.eveningEnd):
- IDLE -> [CLOCK_IN]
- WORKING -> [evening ? CLOCK_OUT : BREAK_START]
- ON_BREAK -> [BREAK_END] + (evening ? [CLOCK_OUT] : [])
- DONE -> force ? [CLOCK_IN] : []
leer=NoOp; sonst kinds sequenziell inserten (ts=now), Recorded(kinds, summarize(after,now)).
Morgenfenster ist KEIN Gate (Clock-in jederzeit), nur für Reminder.

## 6. Repository
insert/between/update/delete durchreichen; observeToday(now):Flow via dayBounds+observeBetween; observeAll().

## 7. Settings
AppSettings(morningStart 07:00, morningEnd 09:00, eveningStart 16:30, eveningEnd 20:00, autoCloseTime 20:15, reminderTime 09:00, autoCloseEnabled true, reminderEnabled true).
SettingsRepository: DataStore, LocalTime als Minuten-seit-Mitternacht (Int). flow():Flow<AppSettings>, current():AppSettings, Setter je Feld. Nach Änderung von autoCloseTime/reminderTime/Enabled -> AlarmScheduler.rescheduleAll().

## 8. Trigger
Puncher.fire(context, force=false, onDone): Coroutine auf appScope aus AppContainer, ruft punchUseCase.punch(force), onDone auf Main.
- Tile (PunchTileService: TileService): onClick -> Puncher.fire -> Subtitle=Status/Worked, updateTile(). onStartListening: Status setzen.
- Widget (Glance): Button + Live-Status (state + worked/pause). onClick=actionRunCallback<PunchAction>(). PunchAction: ActionCallback ruft Puncher.fire, dann PunchWidget().update(). Optional Reset-Button (force=true). Jedes Update frisch aus DB.
- Shortcut: dynamischer Shortcut -> PunchTrampolineActivity (translucent, kein Layout): onCreate Puncher.fire { finish() }. ShortcutSetup.ensure() beim Start.
Alle drei enden im selben PunchUseCase.punch().

## 9. Alarme
AlarmScheduler: scheduleAutoClose() nächstes Vorkommen autoCloseTime via setExactAndAllowWhileIdle -> AutoCloseReceiver; scheduleReminder() analog -> ReminderReceiver; rescheduleAll() nach Settings-Change und in BootReceiver. Android 12+: canScheduleExactAlarms() prüfen, sonst ACTION_REQUEST_SCHEDULE_EXACT_ALARM (Hinweis-Card). Jeder Receiver self-reschedule.
AutoCloseReceiver (goAsync + Coroutine): last event: BREAK_START -> BREAK_END(auto)+CLOCK_OUT(auto); CLOCK_IN|BREAK_END -> CLOCK_OUT(auto); sonst nichts. Danach scheduleAutoClose().
ReminderReceiver: wenn heute kein CLOCK_IN und now nach morningEnd -> Notification (feste ID). Danach scheduleReminder().
Notification-Channel in App.onCreate. POST_NOTIFICATIONS beim ersten Start (33+).

## 10. UI
Home: Punch-Button (Label je State: Einstempeln/Pause/Weiter/Ausstempeln/Fertig). Live-Karten: gearbeitet H:MM, Pause H:MM, Status, erste/letzte Buchung. VM: observeToday+settings.flow, summarize(now), sekündlicher Tick. DONE -> Reset(force=true) mit Bestätigung.
History: observeAll, gruppiert nach lokalem Tag, Summen+Event-Liste, auto=true als Chip, Tap -> Edit-Dialog (TimePicker -> update; delete). Wochensumme oben.
Settings: TimePicker für alle Zeiten §7, Switches Auto-Close/Reminder, Exact-Alarm-Hinweis-Card, CSV-Export-Button.
Fallback-Menü "Manuell buchen": alle 4 Kinds direkt einfügen.

## 11. Export
CsvExporter: alle Punches -> datum;zeit;kind;auto + optional aggregiert datum;ein;aus;arbeit_min;pause_min. Zeiten in Europe/Berlin. ACTION_SEND (text/csv) über FileProvider aus cacheDir. Kein Storage-Permission.

## 12. Manifest / Permissions
POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED.
Registrieren: PunchTileService (BIND_QUICK_SETTINGS_TILE, Tile-Meta), PunchWidgetReceiver (APPWIDGET_UPDATE + Glance-Meta), AutoCloseReceiver/ReminderReceiver (nicht exported), BootReceiver (BOOT_COMPLETED, LOCKED_BOOT_COMPLETED), PunchTrampolineActivity (exported, kein Launcher, translucent), FileProvider.

## 13. Fallstricke
- Ties: gleicher ts -> ORDER BY ts ASC, id ASC + sequenzielle Inserts.
- DST: alles Instant; Grenzen/Anzeige über ZONE; nie LocalDateTime speichern.
- Prozess-Tod: kein In-Memory-State.
- Exact-Alarm-Drosselung (12+/14): Permission + Runtime-Check + Self-Reschedule.
- Widget-State nie cachen; kein Activity-Start beim Button.
- Doppel-Trigger: Mutex in PunchUseCase.
- Reminder-Spam: eine Notification/Tag, feste ID, still wenn eingestempelt.
- Receiver-DB: goAsync() + Coroutine.

## 14. Akzeptanzkriterien
1. Leer + Tile-Tap 08:00 -> genau ein CLOCK_IN, Home "arbeitet".
2. Tap 12:00 (WORKING) -> BREAK_START; erneut -> BREAK_END, Pause steigt.
3. Tap Abendfenster WORKING -> CLOCK_OUT; DONE; weiterer Tap NoOp.
4. Tap Abendfenster ON_BREAK -> BREAK_END+CLOCK_OUT (Reihenfolge korrekt, worked zählt Pause nicht).
5. Auto-Close 20:15 bei offenem Tag -> auto=true-Events; History markiert; schon zu -> nichts.
6. Reminder nur wenn bis morningEnd kein CLOCK_IN.
7. Widget/Tile/Shortcut identisches Verhalten.
8. Prozess-Kill zwischen Taps ändert nichts.
9. Reboot -> BootReceiver plant beide Alarme neu.
10. DST-Tag: Summen plausibel, keine negativen/doppelten Stunden.
11. History-Edit rechnet Summen neu.
12. CSV-Export öffnet Share-Sheet mit gültiger Datei.
13. Zeitfenster in Settings änderbar, greifen sofort (inkl. Alarm-Reschedule).

## 15. Build-Reihenfolge
1. Datenmodell + DAO + DB + Converters + Repository.
2. Domain (TimeWindows, stateFrom, summarize, PunchUseCase) + Unit-Tests gegen §14.1-4 mit fixer Clock.
3. Settings + DataStore.
4. Puncher + Trampoline + Tile + Shortcut.
5. Glance-Widget.
6. Alarme + Receiver + Boot + Notification.
7. UI (Home -> History -> Settings).
8. Export.
9. §14 abarbeiten.
