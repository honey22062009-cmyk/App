# Strict Blocker (Android)

A production-grade, native Android application engineered for **strict scheduled application blocking** with non-root enforcement. Designed and optimized for phones and tablets, including the **Samsung Galaxy Tab A9+** running modern Samsung One UI and Android 13+.

---

## Core Purpose

Strict Blocker allows device owners to manage installed applications and define precise recurring time windows when those applications are allowed to run.

- **Strict Enforcement**: Outside an allowed schedule window, managed applications are strictly blocked.
- **Immediate Interruption**: When an unallowed application is opened, an immediate fullscreen interrupt shield (`BlockerActivity`) intercepts access.
- **No Compromise UI**: Does not display "Ignore", "Use Anyway", "5 More Minutes", or "Snooze" buttons.
- **Zero Telemetry & 100% Privacy**: Operates fully on-device with a local Room SQLite database. No network requests, tracking, or advertisements.

---

## Architecture & Technology Stack

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: Clean Architecture / MVVM with pure Kotlin decoupled evaluation engine
- **Local Persistence**: Room SQLite Database for managed apps, schedules, and enforcement logs
- **Configuration**: Jetpack DataStore Preferences for strict delay locks and runtime settings
- **Dual-Pane Tablet Support**: Responsive layout featuring Navigation Rail and side-by-side app/schedule management on Samsung Galaxy Tab A9+ (and screens >= 720dp)

### Strict Blocking Engine
1. **Accessibility Service (`AppBlockerAccessibilityService`)**:
   - Zero-latency (<50ms) window state change detection (`TYPE_WINDOW_STATE_CHANGED`).
   - Privacy guarantee: `canRetrieveWindowContent` is explicitly configured to `false`. The service never inspects screen text, passwords, or keyboard inputs.
2. **Usage Access Fallback (`BlockerMonitoringService`)**:
   - Persistent foreground service monitoring active apps via `UsageStatsManager.queryEvents`.
3. **Exact Schedule Cutoffs (`AlarmManager`)**:
   - `RTC_WAKEUP` exact alarms trigger at the exact minute an allowed window ends, guaranteeing immediate cutoff even if the user is already inside the application.
4. **Resilience & Reboot (`BootReceiver`, `TimeChangeReceiver`)**:
   - Restores monitoring and alarms upon `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, and `MY_PACKAGE_REPLACED`.
   - Automatically recalculates schedule boundaries on `TIMEZONE_CHANGED`, `TIME_SET`, and `DATE_CHANGED`.
5. **Safety Whitelist (`SafetyWhitelist`)**:
   - Automatically excludes system-critical components from being blocked: Android System UI, Android Settings, Default Launcher, Samsung Telephony/Dialer, and Strict Blocker itself.

---

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/
│   ├── StrictBlockerApplication.kt
│   ├── MainActivity.kt
│   ├── core/
│   │   ├── model/ (SchedulePeriod, BlockEvaluationResult, TimeRange)
│   │   ├── evaluator/ (ScheduleEvaluator - pure testable evaluation engine)
│   │   └── system/ (AlarmScheduler, PermissionHelper, SafetyWhitelist)
│   ├── data/
│   │   ├── db/ (AppDatabase, DAOs, Entities)
│   │   ├── preferences/ (SettingsPreferences with DataStore)
│   │   └── repository/ (AppBlockerRepository)
│   ├── receiver/ (BootReceiver, ScheduleAlarmReceiver, TimeChangeReceiver)
│   ├── service/ (AppBlockerAccessibilityService, BlockerMonitoringService)
│   └── ui/
│       ├── blocker/ (BlockerActivity - Fullscreen interrupt shield)
│       ├── navigation/ (Screen routes and definitions)
│       ├── screens/
│       │   ├── dashboard/ (DashboardScreen)
│       │   ├── apps/ (AppsScreen with search and multi-select)
│       │   ├── schedules/ (SchedulesScreen with timeline and ScheduleEditorDialog)
│       │   ├── onboarding/ (PermissionsScreen with WHY/WHAT/HOW guides)
│       │   ├── settings/ (SettingsScreen with delay lock)
│       │   └── antibypass/ (AntiBypassGuideScreen)
│       └── theme/ (Color, Theme, Type)
└── res/
    ├── drawable/ (App icons and layer-list foregrounds)
    ├── values/ (strings.xml, themes.xml, colors.xml)
    └── xml/ (accessibility_service_config.xml, backup rules)
```

---

## Permissions & Onboarding

Strict Blocker requires the following permissions for reliable enforcement:

| Permission | Purpose | Why Required |
| :--- | :--- | :--- |
| `BIND_ACCESSIBILITY_SERVICE` | Real-time window state detection | Provides instant (<50ms) blocking reaction when a restricted app is launched. |
| `PACKAGE_USAGE_STATS` | Usage Access query | Acts as a secondary verification engine to monitor foreground transitions. |
| `SYSTEM_ALERT_WINDOW` | Display over other apps | Allows full-screen shield overlay to interrupt running unallowed apps. |
| `POST_NOTIFICATIONS` | Foreground service notification | Required on Android 13+ to maintain active background protection status. |
| `SCHEDULE_EXACT_ALARM` | Precise cutoff alarms | Guarantees exact to-the-minute schedule cutoff transitions. |
| `RECEIVE_BOOT_COMPLETED` | System startup handling | Restores schedules and alarm triggers immediately upon tablet reboot. |

### Samsung Galaxy Tab A9+ / One UI Optimization
On Samsung One UI devices:
1. Open **Settings** > **Apps** > **Strict Blocker**.
2. Tap **Battery** and select **Unrestricted**.
3. Under **Accessibility** > **Installed Apps**, switch **Strict Blocker** to **ON**.

---

## Build Instructions

### Prerequisites
- JDK 17 or higher
- Android SDK 36 (compileSdk = 36, minSdk = 26)

### Build Debug APK
To compile the project and generate the debug APK:

```bash
gradle assembleDebug
```

The resulting APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Run Unit and Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### Release Build
To generate a signed release APK or Android App Bundle (AAB):
```bash
gradle assembleRelease
# or for Play Store bundle:
gradle bundleRelease
```

The release build uses the signing configuration defined in `app/build.gradle.kts` via environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`).

---

## Anti-Bypass & Enterprise Device Owner (Optional)

In compliance with standard Android security architecture, non-root applications cannot prevent a physical device owner from uninstalling apps or revoking permissions in Settings. Strict Blocker provides an optional **Schedule Change Delay** (e.g. 15 or 30 minutes) to eliminate impulsive bypassing.

For enterprise environments, dedicated kiosks, or educational Samsung Galaxy Tab A9+ tablets requiring strict uninstall prevention:
```bash
adb shell dpm set-device-owner com.aistudio.strictblocker.kzvwpq/.receiver.DeviceAdmin
```
*(Note: Device Owner mode requires setting upon a fresh factory reset or zero-accounts setup.)*

---

## Privacy Policy, Terms & Legal Compliance

- **Privacy Policy**: 100% on-device local storage via Room SQLite. Zero telemetry, cloud sync, or remote network transmission.
- **Data Minimization**: Stores only the minimum package identifiers and local hour/minute schedule intervals.
- **Accessibility Safeguards**: `canRetrieveWindowContent` is explicitly configured to `false` in `accessibility_service_config.xml`, preventing any reading of screen text, keyboard inputs, passwords, or messages.
- **Terms of Service**: Personal productivity utility software provided as-is without warranty.
- **Refund Policy & Fees**: 100% free and open source. Zero hidden fees, microtransactions, or subscription fees.
- **Cookie Policy**: Native Android application; zero HTTP cookies, tracking pixels, or web beacons used.
- **Third-Party SDKs**: Zero advertising, tracking, or analytics SDKs bundled in the APK.
- **Children's Privacy (COPPA / GDPR-K)**: Safe for all ages; no personal data collected from children under 13/16.
- **Data Deletion (Right to Erasure)**: Fully functional "Delete All Data" feature available directly in app settings.
- **License**: Apache License, Version 2.0.

