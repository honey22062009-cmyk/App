> Version 1.4.0: pastel themes, translucent sliding dock, visible Pro workspace, daily-entry receipts, a joined 3D bunny with optional sound, a quick tour and browser inspection without a VPN. See [the implementation and release notes](docs/EXPERIENCE-1.4.md).

# Strict Blocker (Android)

Current reliability/UI update: **1.3.1**. See [implementation changes, verification and device limits](docs/RELIABILITY-1.3.1.md).

A production-grade, native Android application engineered for **strict scheduled application blocking** with non-root enforcement. Designed and optimized for phones and tablets, including the **Samsung Galaxy Tab A9+** running modern Samsung One UI and Android 13+.

---

## Core Purpose

Strict Blocker allows device owners to manage installed applications and define precise recurring time windows when those applications are allowed to run.

- **Strict Enforcement**: Outside an allowed schedule window, managed applications are strictly blocked.
- **Immediate Interruption**: When an unallowed application is opened, an immediate fullscreen interrupt shield (`BlockerActivity`) intercepts access.
- **No Compromise UI**: Does not display "Ignore", "Use Anyway", "5 More Minutes", or "Snooze" buttons.
- **Local-first design**: Schedules, preferences, and enforcement records are stored locally. No analytics or ad SDKs are configured. With consent, Browser Guard inspects visible address/search fields locally. Version 1.4 removes the VPN/DNS tunnel.

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
   - Privacy boundary: `canRetrieveWindowContent` is enabled because browser protection evaluates visible accessibility text. Inspection is limited to browser context; password fields and unrelated editable values are excluded from text evaluation. The app does not upload page text, although blocked attempts may be recorded locally.
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

- **Data flows**: Schedules, preferences, and enforcement records are stored locally via Room/DataStore. The manual Pro-payment flow uses Firebase Authentication and Cloud Firestore for payment request IDs, plan/amount, UPI transaction references, one-way device hashes, review status, and entitlement expiry. No Firebase Analytics or advertising SDK is configured. Browser Guard evaluates visible browser address/search fields on-device after consent; inspected values are not stored or uploaded. There is no VPN service or DNS proxy.
- **Data Minimization**: Stores only the minimum package identifiers and local hour/minute schedule intervals.
- **Accessibility Safeguards**: `canRetrieveWindowContent` is enabled in `accessibility_service_config.xml` for scheduled blocking and browser-context adult-content checks. Text evaluation is restricted to browser windows; password fields and unrelated editable values are skipped. No page text is uploaded. Blocked attempts can be recorded locally, so users should review the privacy behavior before enabling these features.
- **Terms of Service**: Personal productivity utility software provided as-is without warranty; distribution terms should be reviewed before commercial release.
- **Pricing and payments**: The Android app offers a manual UPI Pro flow: ₹39 for 30 days and ₹599 for 365 days. The user pays through an installed UPI app, submits the UPI transaction reference, and an authenticated owner manually approves or rejects the request in the app. Firebase does not verify the bank transfer automatically.
- **Refund Policy**: A real support contact and clear refund terms must be published before accepting payment. The repository does not implement automated refunds.
- **Cookie Policy**: The native Android application does not use HTTP cookies, tracking pixels, or web beacons. Review the hosting provider's behavior for the optional website.
- **Third-Party SDKs**: Zero advertising, tracking, or analytics SDKs bundled in the APK.
- **Children's Privacy (COPPA / GDPR-K)**: Safe for all ages; no personal data collected from children under 13/16.
- **Data Deletion (Right to Erasure)**: Fully functional "Delete All Data" feature available directly in app settings.
- **License**: Apache License, Version 2.0.



## UI Design System

The app uses a reusable Material 3 design system with theme-aware glass surfaces, restrained brand colors, adaptive rounded shapes, and responsive layouts for phones and tablets. Visual effects live in shared Compose components so screens remain maintainable and feature logic stays separate from presentation.


## UX redesign and release boundaries

The native app remains local-first: managed applications and schedules are stored with Room, and app preferences are stored with DataStore. The overview uses the saved protection setting together with current Accessibility, Usage Access, and overlay permission checks; it does not label the setting as active when these core permissions are missing. Permission state is re-checked when the activity resumes from Android Settings.

First-run users are sent to the existing permission/setup guide. Continuing from that guide records onboarding completion locally, so returning users are not sent through setup again.

### Authentication and billing status

The app now includes Firebase Authentication and Cloud Firestore for the manual Pro-payment workflow. Regular users can be represented by Firebase Anonymous Authentication solely for payment-request ownership; the owner uses an Email/Password Firebase account whose UID must exist in the Firestore `admins` collection. Firestore rules restrict normal users to creating and reading their own payment requests, while only an authenticated admin can approve or reject them.

The current Pro prices are ₹39 for 30 days and ₹599 for 365 days. Payment itself happens in the user's UPI app. Strict Blocker does not automatically verify the bank transfer; the owner must compare the submitted transaction reference with the actual received payment before approval. Approved entitlements are restored from Firestore when the app reopens and expire according to the approved plan.

### Verification

The repository's Android CI workflow builds the debug APK and runs `gradle :app:testDebugUnitTest`. A branch/PR CI result should be treated as the build/test evidence; no physical-device test is implied by a successful CI run.
