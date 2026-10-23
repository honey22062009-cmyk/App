<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permissions required for strict, reliable app blocking -->
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" tools:ignore="QueryAllPackagesPermission" />

    <!-- Query launchable apps -->
    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.HOME" />
        </intent>
    </queries>

    <application
        android:name=".StrictBlockerApplication"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MyApplication">

        <!-- Main Dashboard & Management Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:windowSoftInputMode="adjustResize"
            android:theme="@style/Theme.MyApplication">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Fullscreen Shield / Blocker Activity -->
        <activity
            android:name=".ui.blocker.BlockerActivity"
            android:exported="false"
            android:excludeFromRecents="true"
            android:launchMode="singleInstance"
            android:noHistory="true"
            android:showWhenLocked="true"
            android:turnScreenOn="true"
            android:theme="@style/Theme.StrictBlocker.Overlay" />

        <!-- Accessibility Service for Instant Foreground Interception -->
        <service
            android:name=".service.AppBlockerAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <!-- Persistent Monitoring & Fallback Service -->
        <service
            android:name=".service.BlockerMonitoringService"
            android:foregroundServiceType="specialUse"
            android:exported="false">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Enforce scheduled application blocking and background verification" />
        </service>

        <!-- On-Device DNS Filtering VPN Service for 18+ Adult Content Protection -->
        <service
            android:name=".service.AdultFilterVpnService"
            android:permission="android.permission.BIND_VPN_SERVICE"
            android:exported="false">
            <intent-filter>
                <action android:name="android.net.VpnService" />
            </intent-filter>
        </service>

        <!-- Boot & App Update Receiver -->
        <receiver
            android:name=".receiver.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
            </intent-filter>
        </receiver>

        <!-- Schedule Transition Exact Alarm Receiver -->
        <receiver
            android:name=".receiver.ScheduleAlarmReceiver"
            android:exported="false" />

        <!-- Time, Timezone & Date Change Receiver -->
        <receiver
            android:name=".receiver.TimeChangeReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.TIME_SET" />
                <action android:name="android.intent.action.TIMEZONE_CHANGED" />
                <action android:name="android.intent.action.DATE_CHANGED" />
            </intent-filter>
        </receiver>

    </application>

</manifest>


---

## Version 1.4: account, purchase UI, diagnostics, and data portability

New screens include Account, Upgrade, Diagnostics, Activity Reports, Schedule Templates, and encrypted Backup & Restore. The dashboard exposes these tools directly, with shortcuts in Settings.

### Configure account sign-in

Email/password sign-in uses Firebase Authentication; Strict Blocker does not store account passwords locally. Configure the following Gradle properties or environment variables for a reviewed build:

- FIREBASE_API_KEY
- FIREBASE_APP_ID
- FIREBASE_PROJECT_ID
- FIREBASE_MESSAGING_SENDER_ID

Enable Email/Password sign-in in the matching Firebase project. Without these values, the account screen reports that authentication is not configured. Signing in does not grant Premium access on its own.

### Configure UPI purchase options

Set the following Gradle properties or environment variables in a reviewed build:

- PAYMENT_UPI_ID: genuine UPI ID for the receiving account.
- PAYMENT_PAYEE_NAME: payee name verified against the receiving account.
- PAYMENT_PRICE_INR: final price.
- PAYMENT_SUPPORT_EMAIL: monitored support inbox.
- PAYMENT_WEBSITE_URL: optional HTTPS storefront URL.

No payment details are committed by default. Missing details keep the UPI action disabled. Manual email is not automatic transaction verification and does not unlock Premium. Automated unlock requires a backend that verifies transactions server-side. Have the adult/guardian responsible for the payment account review eligibility, seller details, terms, refund policy, and applicable obligations before taking payments.

### Encrypted backup and restore

Backup files use PBKDF2-HMAC-SHA256 and AES-GCM encryption. They include managed app records, schedule windows, and app groups, but exclude activity logs, passwords, payment details, and Android permission grants. The passphrase cannot be recovered. Restoring replaces existing managed-app schedules; check Diagnostics and test the schedules afterwards.

### Release status

Version 1.4 needs CI verification, a release APK signed with the intended release key, and real-device testing for schedules, reboot recovery, permissions, and content filtering. CI success is not proof of device-level reliability.
