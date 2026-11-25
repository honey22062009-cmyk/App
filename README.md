# Strict Blocker 2.2.0

Strict Blocker is a native Android app for scheduled app blocking, focus sessions and private habit progress. It uses Kotlin, Jetpack Compose, Room and DataStore, with layouts for phones and tablets.

## What is new in 2.2.0

- Replaced the green-heavy interface with a plum, rose and warm-ivory theme.
- Added a new bunny-and-shield launcher mark, used across the splash screen, dashboard, tablet rail and Focus companion panel.
- Removed the DNS controls, DNS preferences, VPN service registration, reboot startup and unused DNS tunnel implementation. Browser Guard now describes its supported on-device browser checks and their limitations honestly.
- Added a local profile name. It is stored on the device and shown on the dashboard; it is not a cloud account.
- Added a guided first-run tour with five short steps and a replay action in Settings.
- Added an automatic streak counter based on the exact locally saved start timestamp. Completed days advance after each full 24 hours without daily check-ins.
- Added historical streak runs that remain visible after a reset, a separate previous-best benchmark, and 7-day, 30-day, 90-day and all-time graph filters.
- Removed the Daily Notes section and manual daily check-in action from Progress; resets remain user-recorded.
- Improved Focus with a saved task name, activity category, goal progress and a daily focus target.
- Rebuilt Settings with clearer sections, local profile settings, appearance controls, focus targets, backup and restore, permission links and an expanded About section.
- Added ZIP export and restore for the local Room database and known DataStore files, including personal progress.
- Reduced accessibility-event scanning frequency by ignoring continuous scroll/content-change events and sampling text/focus events at most once every 700 ms. App-launch events remain immediate.

## Ten useful additions

1. First-run walkthrough and replay.
2. On-device display profile.
3. Editable personal-goal tracker.
4. Automatic 24-hour streak rollover.
5. Historical streak line graph with reset events.
6. Previous-best benchmark and date-range filters.
7. Milestone markers.
8. Local profile and PIN gate.
9. Focus task names and activity categories.
10. Export/import backup for app schedules, settings, focus records and progress.

## Data, privacy and sign-in

The app's core features remain local-first. App rules, schedules and blocked events are stored with Room. Preferences, focus history and personal progress use separate DataStore files. The new profile is a local display name only.

There is **no cloud login, account backend or cross-device sync** configured in this build. No fake sign-in screen is used. DNS/VPN filtering has been removed; Browser Guard only checks visible accessibility information in supported browser contexts and cannot guarantee coverage of every browser, embedded view or page. Page text is not uploaded.

Personal progress is a private, user-controlled goal tracker. The day counter advances from the exact locally stored start time; the app does not monitor device activity or automatically detect personal behaviour. The user records resets. Records remain on-device unless the user exports a backup.

## Backup and restore

Open **Settings → Backup and restore**.

- **Create backup** writes a ZIP containing the local database and supported DataStore files.
- **Import previous version data** accepts a Strict Blocker ZIP containing the supported database/preferences files.
- The restore flow checks the SQLite database before copying the supported files.
- Export a current backup before importing. Restoring replaces matching local app data. After a restore, fully close and reopen Strict Blocker so the database and preferences are reopened from disk.
- Backups can contain schedules, app choices, blocked events, focus totals, a local profile, notes and personal progress. Store the file somewhere private.

This importer is not a general Android device-backup reader. It accepts this app's supported ZIP format and matching legacy file names. Older files with incompatible database schemas may still need a version-specific migration.

## Build and validation

Requirements: JDK 17 and Android SDK 36.

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

CI builds the installable debug APK, verifies its Android APK signature with `apksigner`, uploads the artifact and runs JVM/Robolectric tests. A successful CI run does not replace installation and runtime testing on the Samsung tablet.

## Android limitations

Strict Blocker is a regular Android app, not a device-owner/MDM controller. Android can revoke permissions, stop background work or uninstall the app. Scheduled blocking reliability depends on Accessibility, Usage Access, overlay, exact-alarm and battery settings remaining correctly configured.

The application ID remains `com.aistudio.strictblocker.kzvwpq`. Version 2.2.0 uses versionCode 14. Updating an existing install in place still requires the same signing certificate as the installed version. The debug APK is signed with the Gradle debug key; a permanent release update requires the retained release signing bundle to be configured in repository Actions secrets.

## License

Apache License 2.0.
