package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import androidx.datastore.preferences.core.floatPreferencesKey
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import androidx.datastore.preferences.core.doublePreferencesKey

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "strict_blocker_settings")

class SettingsPreferences(private val context: Context) {

    private object PreferencesKeys {
        val PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        val LOCAL_PIN_SALT = stringPreferencesKey("local_security_pin_salt_v1")
        val LOCAL_PIN_HASH = stringPreferencesKey("local_security_pin_hash_v1")
        val LOCAL_PIN_GATE_ENABLED = booleanPreferencesKey("local_security_pin_gate_enabled_v1")
        val LOCAL_PIN_LOCK_UNTIL = longPreferencesKey("local_security_pin_lock_until_v1")
        val LOCAL_PIN_FAILED_ATTEMPTS = intPreferencesKey("local_security_pin_failed_attempts_v1")
        val STRICT_CHANGE_DELAY_MINUTES = intPreferencesKey("strict_change_delay_minutes")
        val PENDING_CHANGE_UNLOCK_TIME = longPreferencesKey("pending_change_unlock_time")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SHOW_OVERLAY_SHIELD = booleanPreferencesKey("show_overlay_shield")
        val ACCENT_STYLE = stringPreferencesKey("accent_style")
        val QUICK_TOUR_SEEN = booleanPreferencesKey("quick_tour_seen_v1")
        val COMPANION_SOUND = booleanPreferencesKey("companion_sound")
        val BROWSER_INSPECTION_CONSENT = booleanPreferencesKey("browser_inspection_consent_v2")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"

        // Local-only onboarding/profile. These fields are intentionally not synced to Firebase.
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AGE = intPreferencesKey("user_age")
        val USER_GENDER = stringPreferencesKey("user_gender")
        val SELECTED_CHARACTER_ID = stringPreferencesKey("selected_character_id")
        val CHARACTER_NAME = stringPreferencesKey("character_name")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val SLEEP_REMINDER_ENABLED = booleanPreferencesKey("sleep_reminder_enabled")
        val SLEEP_REMINDER_HOUR = intPreferencesKey("sleep_reminder_hour")
        val SLEEP_REMINDER_MINUTE = intPreferencesKey("sleep_reminder_minute")

        // Adult Content Protection Settings
        val ADULT_PROTECTION_ENABLED = booleanPreferencesKey("adult_protection_enabled")
        val STRICT_ADULT_MODE = booleanPreferencesKey("strict_adult_mode")
        val ADULT_UNLOCK_NOT_BEFORE = longPreferencesKey("adult_unlock_not_before")
        val ADULT_UNLOCK_VERIFIED = booleanPreferencesKey("adult_unlock_verified")
        val ADULT_COOLDOWN_SECONDS = intPreferencesKey("adult_cooldown_seconds")
        val BLOCK_INCOGNITO_ENABLED = booleanPreferencesKey("block_incognito_enabled")
        val FORCE_SAFESEARCH_ENABLED = booleanPreferencesKey("force_safesearch_enabled")
        val DNS_FILTER_ENABLED = booleanPreferencesKey("dns_filter_enabled")
        val CUSTOM_DNS_PRIMARY = stringPreferencesKey("custom_dns_primary")
        val CUSTOM_DNS_SECONDARY = stringPreferencesKey("custom_dns_secondary")
        val ADULT_BLOCKS_COUNT = intPreferencesKey("adult_blocks_count")
        val APP_GROUPS = stringSetPreferencesKey("app_groups")

        // Personal streak tracker. This is entirely user-entered; the app does not
        // attempt to infer or monitor sexual activity.
        val TRACKER_STARTED_AT = longPreferencesKey("tracker_started_at")
        val LAST_FAP_AT = longPreferencesKey("last_fap_at")
        val PREVIOUS_FAP_STREAK_DAYS = intPreferencesKey("previous_fap_streak_days")
        val BEST_FAP_STREAK_DAYS = intPreferencesKey("best_fap_streak_days")
        val FAP_LOG_COUNT = intPreferencesKey("fap_log_count")
        val FAP_EVENT_TIMESTAMPS = stringSetPreferencesKey("fap_event_timestamps")
        val TRACKER_RESET_RECORDS = stringSetPreferencesKey("tracker_reset_records_v2")
        val DAILY_CHECK_INS = stringSetPreferencesKey("daily_check_ins")
        val SLEEP_ENTRIES = stringSetPreferencesKey("sleep_entries")
        val TRACKER_GOAL_DAYS = intPreferencesKey("tracker_goal_days")
        val DASHBOARD_CARD_ORDER = stringPreferencesKey("dashboard_card_order")
        val DASHBOARD_HIDDEN_CARDS = stringSetPreferencesKey("dashboard_hidden_cards")
        val SEEN_MILESTONES = stringSetPreferencesKey("seen_tracker_milestones")
        val FOCUS_SESSION_RECORDS = stringSetPreferencesKey("focus_session_records_v1")
        val DAILY_FOCUS_TASKS = stringSetPreferencesKey("daily_focus_tasks_v1")

        // User-selectable blocker customization
        val BLOCKER_STYLE = stringPreferencesKey("blocker_style")
        val BLOCKER_PHOTO_URI = stringPreferencesKey("blocker_photo_uri")
        val BLOCKER_MESSAGE = stringPreferencesKey("blocker_message")
        val SHOW_PACKAGE_ON_BLOCKER = booleanPreferencesKey("show_package_on_blocker")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val BLOCK_HISTORY_RETENTION_DAYS = intPreferencesKey("block_history_retention_days")

        // Remembered Google account identity. Signing out clears the session flag
        // but deliberately keeps these display fields for faster re-login.
        val ACCOUNT_SIGNED_IN = booleanPreferencesKey("account_signed_in")
        val ACCOUNT_ID = stringPreferencesKey("account_id")
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        val ACCOUNT_DISPLAY_NAME = stringPreferencesKey("account_display_name")
        val ACCOUNT_PHOTO_URI = stringPreferencesKey("account_photo_uri")
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "light"
    }

    val isProtectionEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PROTECTION_ENABLED] ?: true
    }

    val localSecurityPinConfiguredFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        !preferences[PreferencesKeys.LOCAL_PIN_SALT].isNullOrBlank() && !preferences[PreferencesKeys.LOCAL_PIN_HASH].isNullOrBlank()
    }

    val localSecurityPinGateEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LOCAL_PIN_GATE_ENABLED] ?: false
    }

    val strictChangeDelayMinutesFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.STRICT_CHANGE_DELAY_MINUTES] ?: 0
    }

    val pendingChangeUnlockTimeFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PENDING_CHANGE_UNLOCK_TIME] ?: 0L
    }

    val isOnboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        // Recover profiles saved by older builds before they wrote the completion flag.
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] == true ||
            (!preferences[PreferencesKeys.USER_NAME].isNullOrBlank() &&
                (preferences[PreferencesKeys.USER_AGE] ?: 0) in 13..120)
    }

    val isOverlayShieldEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_OVERLAY_SHIELD] ?: true
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_NAME] ?: ""
    }

    val accentStyleFlow: Flow<String> = context.dataStore.data.map { it[PreferencesKeys.ACCENT_STYLE] ?: "rose" }
    val quickTourSeenFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.QUICK_TOUR_SEEN] ?: false }
    val companionSoundFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.COMPANION_SOUND] ?: false }
    val browserInspectionConsentFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.BROWSER_INSPECTION_CONSENT] ?: false }

    suspend fun setAccentStyle(style: String) {
        require(style in setOf("rose", "lilac", "peach"))
        context.dataStore.edit { it[PreferencesKeys.ACCENT_STYLE] = style }
    }
    suspend fun setQuickTourSeen(seen: Boolean) { context.dataStore.edit { it[PreferencesKeys.QUICK_TOUR_SEEN] = seen } }
    suspend fun setCompanionSound(enabled: Boolean) { context.dataStore.edit { it[PreferencesKeys.COMPANION_SOUND] = enabled } }
    suspend fun setBrowserInspectionConsent(granted: Boolean) { context.dataStore.edit { it[PreferencesKeys.BROWSER_INSPECTION_CONSENT] = granted } }

    val userAgeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_AGE] ?: 0
    }

    val userGenderFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_GENDER] ?: "prefer_not_to_say"
    }

    val selectedCharacterIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELECTED_CHARACTER_ID] ?: "nova"
    }

    val characterNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CHARACTER_NAME] ?: ""
    }

    val dailyReminderEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_REMINDER_ENABLED] ?: false
    }

    val dailyReminderHourFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[PreferencesKeys.DAILY_REMINDER_HOUR] ?: 20).coerceIn(0, 23)
    }

    val dailyReminderMinuteFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[PreferencesKeys.DAILY_REMINDER_MINUTE] ?: 0).coerceIn(0, 59)
    }

    val sleepReminderEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SLEEP_REMINDER_ENABLED] ?: false
    }

    val sleepReminderHourFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[PreferencesKeys.SLEEP_REMINDER_HOUR] ?: 6).coerceIn(0, 23)
    }

    val sleepReminderMinuteFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[PreferencesKeys.SLEEP_REMINDER_MINUTE] ?: 0).coerceIn(0, 59)
    }

    // Adult Content Protection Flows
    val isAdultProtectionEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ADULT_PROTECTION_ENABLED] ?: true
    }

    val isStrictAdultModeFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.STRICT_ADULT_MODE] ?: true }
    val adultUnlockNotBeforeFlow: Flow<Long> = context.dataStore.data.map { it[PreferencesKeys.ADULT_UNLOCK_NOT_BEFORE] ?: 0L }
    val adultUnlockVerifiedFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.ADULT_UNLOCK_VERIFIED] ?: false }
    val adultCooldownSecondsFlow: Flow<Int> = context.dataStore.data.map { it[PreferencesKeys.ADULT_COOLDOWN_SECONDS] ?: 60 }

    val adultBlocksCountFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ADULT_BLOCKS_COUNT] ?: 0
    }

    val appGroupsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_GROUPS] ?: emptySet()
    }

    val focusSessionsFlow: Flow<List<FocusSessionRecord>> = context.dataStore.data.map { preferences ->
        (preferences[PreferencesKeys.FOCUS_SESSION_RECORDS] ?: emptySet())
            .mapNotNull(FocusSessionRecord::decode)
            .sortedByDescending { it.startedAtMillis }
    }

    val dailyFocusTasksFlow: Flow<List<DailyFocusTask>> = context.dataStore.data.map { preferences ->
        (preferences[PreferencesKeys.DAILY_FOCUS_TASKS] ?: emptySet())
            .mapNotNull(DailyFocusTask::decode)
            .sortedWith(compareBy<DailyFocusTask> { it.localDate }.thenBy { it.createdAtMillis })
    }

    val trackerStartedAtFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TRACKER_STARTED_AT] ?: 0L
    }

    val lastFapAtFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_FAP_AT] ?: 0L
    }

    val previousFapStreakDaysFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PREVIOUS_FAP_STREAK_DAYS] ?: 0
    }

    val bestFapStreakDaysFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BEST_FAP_STREAK_DAYS] ?: 0
    }

    val fapLogCountFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FAP_LOG_COUNT] ?: 0
    }

    val fapEventTimestampsFlow: Flow<Set<Long>> = context.dataStore.data.map { preferences ->
        (preferences[PreferencesKeys.FAP_EVENT_TIMESTAMPS] ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    }

    val trackerResetRecordsFlow: Flow<List<TrackerResetRecord>> = context.dataStore.data.map { preferences ->
        val structured = (preferences[PreferencesKeys.TRACKER_RESET_RECORDS] ?: emptySet())
            .mapNotNull(TrackerResetRecord::decode)
            .toMutableList()
        val structuredTimestamps = structured.mapTo(mutableSetOf()) { it.timestamp }

        // Legacy timestamp-only history is merged without collapsing distinct structured
        // records that happen to share the same exact timestamp.
        (preferences[PreferencesKeys.FAP_EVENT_TIMESTAMPS] ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .forEach { timestamp ->
                if (timestamp !in structuredTimestamps) {
                    structured += TrackerResetRecord.legacy(timestamp)
                }
            }

        structured.sortedWith(
            compareByDescending<TrackerResetRecord> { it.timestamp }
                .thenBy { it.id }
        )
    }

    val dailyCheckInsFlow: Flow<List<DailyCheckIn>> = context.dataStore.data.map { preferences ->
        (preferences[PreferencesKeys.DAILY_CHECK_INS] ?: emptySet())
            .mapNotNull(DailyCheckIn::decode)
            .sortedByDescending { it.localDate }
    }

    val sleepEntriesFlow: Flow<List<SleepEntry>> = context.dataStore.data.map { preferences ->
        (preferences[PreferencesKeys.SLEEP_ENTRIES] ?: emptySet())
            .mapNotNull(SleepEntry::decode)
            .sortedByDescending { it.localDate }
    }

    val trackerGoalDaysFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[PreferencesKeys.TRACKER_GOAL_DAYS] ?: 0).coerceIn(0, 3650)
    }

    val dashboardCardOrderFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val defaultOrder = defaultDashboardCardOrder()
        val saved = preferences[PreferencesKeys.DASHBOARD_CARD_ORDER]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it in defaultOrder }
            .orEmpty()
        (saved + defaultOrder).distinct()
    }

    val dashboardHiddenCardsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        val hasSavedLayout = preferences.contains(PreferencesKeys.DASHBOARD_CARD_ORDER) ||
            preferences.contains(PreferencesKeys.DASHBOARD_HIDDEN_CARDS)
        if (hasSavedLayout) {
            preferences[PreferencesKeys.DASHBOARD_HIDDEN_CARDS] ?: emptySet()
        } else {
            setOf("stats", "insights", "timeline")
        }
    }

    // All former premium features are available without payment; Pro is a future-features destination.
    val areAllFeaturesUnlockedFlow: Flow<Boolean> = flowOf(true)

    val seenMilestonesFlow: Flow<Set<Int>> = context.dataStore.data.map { preferences ->
        (preferences[PreferencesKeys.SEEN_MILESTONES] ?: emptySet())
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    }



    val blockerStyleFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BLOCKER_STYLE] ?: "classic"
    }

    val blockerPhotoUriFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BLOCKER_PHOTO_URI] ?: ""
    }

    val blockerMessageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BLOCKER_MESSAGE] ?: "Stay with the plan. This can wait."
    }

    val showPackageOnBlockerFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_PACKAGE_ON_BLOCKER] ?: false
    }

    val reduceMotionFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REDUCE_MOTION] ?: false
    }

    val blockHistoryRetentionDaysFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BLOCK_HISTORY_RETENTION_DAYS] ?: 30
    }

    val isAccountSignedInFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACCOUNT_SIGNED_IN] ?: false
    }

    val accountIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACCOUNT_ID] ?: ""
    }

    val accountEmailFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACCOUNT_EMAIL] ?: ""
    }

    val accountDisplayNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACCOUNT_DISPLAY_NAME] ?: ""
    }

    val accountPhotoUriFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACCOUNT_PHOTO_URI] ?: ""
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROTECTION_ENABLED] = enabled
        }
    }

    /**
     * Local in-app PIN gate only. This does NOT block Android's package installer/uninstaller.
     * The verifier is salted PBKDF2-HMAC-SHA256; the PIN itself is never persisted.
     */
    suspend fun configureLocalSecurityPin(pin: String) {
        require(pin.matches(Regex("[0-9]{4,8}"))) { "PIN must contain 4 to 8 digits." }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = deriveLocalPinHash(pin, salt)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCAL_PIN_SALT] = Base64.getEncoder().encodeToString(salt)
            preferences[PreferencesKeys.LOCAL_PIN_HASH] = Base64.getEncoder().encodeToString(hash)
            preferences[PreferencesKeys.LOCAL_PIN_FAILED_ATTEMPTS] = 0
            preferences.remove(PreferencesKeys.LOCAL_PIN_LOCK_UNTIL)
        }
    }

    suspend fun verifyLocalSecurityPin(pin: String): Boolean {
        val snapshot = context.dataStore.data.first()
        val lockUntil = snapshot[PreferencesKeys.LOCAL_PIN_LOCK_UNTIL] ?: 0L
        if (System.currentTimeMillis() < lockUntil) return false
        val saltText = snapshot[PreferencesKeys.LOCAL_PIN_SALT] ?: return false
        val hashText = snapshot[PreferencesKeys.LOCAL_PIN_HASH] ?: return false
        val salt = runCatching { Base64.getDecoder().decode(saltText) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(hashText) }.getOrNull() ?: return false
        val actual = runCatching { deriveLocalPinHash(pin, salt) }.getOrNull() ?: return false
        val matches = MessageDigest.isEqual(expected, actual)
        context.dataStore.edit { preferences ->
            if (matches) {
                preferences[PreferencesKeys.LOCAL_PIN_FAILED_ATTEMPTS] = 0
                preferences.remove(PreferencesKeys.LOCAL_PIN_LOCK_UNTIL)
            } else {
                val failures = (preferences[PreferencesKeys.LOCAL_PIN_FAILED_ATTEMPTS] ?: 0) + 1
                preferences[PreferencesKeys.LOCAL_PIN_FAILED_ATTEMPTS] = failures
                if (failures >= 5) {
                    preferences[PreferencesKeys.LOCAL_PIN_LOCK_UNTIL] = System.currentTimeMillis() + 30_000L
                    preferences[PreferencesKeys.LOCAL_PIN_FAILED_ATTEMPTS] = 0
                }
            }
        }
        return matches
    }

    suspend fun setLocalSecurityPinGateEnabled(enabled: Boolean) {
        val configured = localSecurityPinConfiguredFlow.first()
        require(!enabled || configured) { "Set a PIN before enabling the PIN gate." }
        context.dataStore.edit { it[PreferencesKeys.LOCAL_PIN_GATE_ENABLED] = enabled }
    }

    private fun deriveLocalPinHash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 150_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    suspend fun setStrictChangeDelayMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STRICT_CHANGE_DELAY_MINUTES] = minutes
        }
    }

    suspend fun setPendingChangeUnlockTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PENDING_CHANGE_UNLOCK_TIME] = timestamp
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun saveLocalProfile(
        name: String,
        age: Int,
        gender: String,
        characterId: String,
        companionName: String?
    ) {
        val safeName = name.trim().replace(Regex("\\s+"), " ").take(32)
        val safeAge = age.coerceIn(13, 120)
        val safeGender = gender.lowercase().trim().takeIf {
            it in setOf("male", "female", "other", "prefer_not_to_say")
        } ?: "prefer_not_to_say"
        val safeCharacter = characterId.lowercase().trim().takeIf {
            it in setOf("nova", "milo", "aria", "kai", "luna", "sol")
        } ?: "nova"
        val safeCompanionName = companionName
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(24)
            .orEmpty()

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = safeName
            preferences[PreferencesKeys.USER_AGE] = safeAge
            preferences[PreferencesKeys.USER_GENDER] = safeGender
            preferences[PreferencesKeys.SELECTED_CHARACTER_ID] = safeCharacter
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = true
            if (safeCompanionName.isBlank()) {
                preferences.remove(PreferencesKeys.CHARACTER_NAME)
            } else {
                preferences[PreferencesKeys.CHARACTER_NAME] = safeCompanionName
            }
        }
    }

    suspend fun saveDailyEntry(date: String, rating: String, sleepMinutes: Int?, removeSleep: Boolean = false) {
        val day = java.time.LocalDate.parse(date)
        require(rating in DailyCheckIn.allowedRatings)
        context.dataStore.edit { p ->
            val checkIns = (p[PreferencesKeys.DAILY_CHECK_INS] ?: emptySet()).toMutableSet()
            checkIns.removeAll { DailyCheckIn.decode(it)?.localDate == day }
            checkIns.add(DailyCheckIn(day, rating).encode())
            p[PreferencesKeys.DAILY_CHECK_INS] = checkIns
            if (removeSleep) {
                p[PreferencesKeys.SLEEP_ENTRIES] = (p[PreferencesKeys.SLEEP_ENTRIES] ?: emptySet())
                    .filterNot { SleepEntry.decode(it)?.localDate == day }.toSet()
            } else if (sleepMinutes != null) {
                val sleep = (p[PreferencesKeys.SLEEP_ENTRIES] ?: emptySet()).toMutableSet()
                val previous = sleep.mapNotNull(SleepEntry::decode).firstOrNull { it.localDate == day }
                sleep.removeAll { SleepEntry.decode(it)?.localDate == day }
                sleep.add(SleepEntry(day, sleepMinutes.coerceIn(0, 1440), previous?.note).encode())
                p[PreferencesKeys.SLEEP_ENTRIES] = sleep
            }
        }
    }

    suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.DAILY_REMINDER_HOUR] = hour.coerceIn(0, 23)
            preferences[PreferencesKeys.DAILY_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setSleepReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SLEEP_REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.SLEEP_REMINDER_HOUR] = hour.coerceIn(0, 23)
            preferences[PreferencesKeys.SLEEP_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setOverlayShieldEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_OVERLAY_SHIELD] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setAdultProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.ADULT_PROTECTION_ENABLED] = enabled
            it[PreferencesKeys.STRICT_ADULT_MODE] = false
            it[PreferencesKeys.ADULT_UNLOCK_NOT_BEFORE] = 0L
            it[PreferencesKeys.DNS_FILTER_ENABLED] = false
            it[PreferencesKeys.BLOCK_INCOGNITO_ENABLED] = false
        }
    }

    suspend fun requestAdultProtectionDisable(delayMinutes: Int = 10) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STRICT_ADULT_MODE] = true
            preferences[PreferencesKeys.ADULT_UNLOCK_NOT_BEFORE] =
                System.currentTimeMillis() + delayMinutes.coerceAtLeast(1) * 60_000L
            preferences[PreferencesKeys.ADULT_UNLOCK_VERIFIED] = false
        }
    }

    suspend fun markAdultUnlockVerificationPassed() {
        context.dataStore.edit { it[PreferencesKeys.ADULT_UNLOCK_VERIFIED] = true }
    }

    suspend fun completeAdultProtectionDisable(verificationPassed: Boolean): Boolean {
        val snapshot = context.dataStore.data.first()
        val notBefore = snapshot[PreferencesKeys.ADULT_UNLOCK_NOT_BEFORE] ?: 0L
        val verified = verificationPassed || (snapshot[PreferencesKeys.ADULT_UNLOCK_VERIFIED] ?: false)
        if (notBefore <= 0L || System.currentTimeMillis() < notBefore || !verified) return false
        context.dataStore.edit {
            it[PreferencesKeys.ADULT_PROTECTION_ENABLED] = false
            it[PreferencesKeys.STRICT_ADULT_MODE] = false
            it[PreferencesKeys.ADULT_UNLOCK_NOT_BEFORE] = 0L
            it[PreferencesKeys.ADULT_UNLOCK_VERIFIED] = false
        }
        return true
    }

    suspend fun setAdultCooldownSeconds(seconds: Int) {
        context.dataStore.edit { it[PreferencesKeys.ADULT_COOLDOWN_SECONDS] = seconds.coerceIn(30, 300) }
    }

    suspend fun setAppGroups(groups: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_GROUPS] = groups
        }
    }

    suspend fun addDailyFocusTask(title: String, localDate: LocalDate = LocalDate.now()): Boolean {
        val cleanTitle = title.trim().take(80)
        if (cleanTitle.isBlank()) return false
        var added = false
        context.dataStore.edit { preferences ->
            val tasks = (preferences[PreferencesKeys.DAILY_FOCUS_TASKS] ?: emptySet())
                .mapNotNull(DailyFocusTask::decode)
                .toMutableList()
            if (tasks.count { it.localDate == localDate } >= 3) return@edit
            tasks += DailyFocusTask.create(localDate, cleanTitle)
            val keepFrom = localDate.minusDays(60)
            preferences[PreferencesKeys.DAILY_FOCUS_TASKS] = tasks
                .filter { !it.localDate.isBefore(keepFrom) }
                .map(DailyFocusTask::encode)
                .toSet()
            added = true
        }
        return added
    }

    suspend fun setDailyFocusTaskCompleted(id: String, completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_FOCUS_TASKS] =
                (preferences[PreferencesKeys.DAILY_FOCUS_TASKS] ?: emptySet())
                    .mapNotNull(DailyFocusTask::decode)
                    .map { if (it.id == id) it.copy(completed = completed) else it }
                    .map(DailyFocusTask::encode)
                    .toSet()
        }
    }

    suspend fun deleteDailyFocusTask(id: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_FOCUS_TASKS] =
                (preferences[PreferencesKeys.DAILY_FOCUS_TASKS] ?: emptySet())
                    .mapNotNull(DailyFocusTask::decode)
                    .filterNot { it.id == id }
                    .map(DailyFocusTask::encode)
                    .toSet()
        }
    }

    suspend fun saveFocusSession(record: FocusSessionRecord) {
        context.dataStore.edit { preferences ->
            val sessions = (preferences[PreferencesKeys.FOCUS_SESSION_RECORDS] ?: emptySet())
                .mapNotNull(FocusSessionRecord::decode)
                .filterNot { it.id == record.id }
                .toMutableList()
            sessions += record
            preferences[PreferencesKeys.FOCUS_SESSION_RECORDS] = sessions
                .sortedByDescending { it.startedAtMillis }
                .take(1000)
                .map(FocusSessionRecord::encode)
                .toSet()
        }
    }

    suspend fun deleteFocusSession(id: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOCUS_SESSION_RECORDS] =
                (preferences[PreferencesKeys.FOCUS_SESSION_RECORDS] ?: emptySet())
                    .filterNot { FocusSessionRecord.decode(it)?.id == id }
                    .toSet()
        }
    }

    suspend fun clearFocusSessionHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.FOCUS_SESSION_RECORDS)
        }
    }

    suspend fun ensureStreakTrackerStarted(nowMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            if ((preferences[PreferencesKeys.TRACKER_STARTED_AT] ?: 0L) <= 0L) {
                preferences[PreferencesKeys.TRACKER_STARTED_AT] = nowMillis
            }
        }
    }

    suspend fun recordFapReset(nowMillis: Long = System.currentTimeMillis()) {
        recordTrackerReset(timestamp = nowMillis)
    }

    suspend fun recordTrackerReset(
        timestamp: Long,
        category: String? = null,
        note: String? = null
    ) {
        val safeTimestamp = timestamp.coerceIn(1L, System.currentTimeMillis())
        context.dataStore.edit { preferences ->
            val start = (preferences[PreferencesKeys.TRACKER_STARTED_AT] ?: safeTimestamp)
                .takeIf { it > 0L }
                ?: safeTimestamp
            preferences[PreferencesKeys.TRACKER_STARTED_AT] = minOf(start, safeTimestamp)

            val structured = (preferences[PreferencesKeys.TRACKER_RESET_RECORDS] ?: emptySet())
                .mapNotNull(TrackerResetRecord::decode)
                .toMutableList()
            structured += TrackerResetRecord.create(safeTimestamp, category, note)
            preferences[PreferencesKeys.TRACKER_RESET_RECORDS] = structured
                .sortedByDescending { it.timestamp }
                .take(1500)
                .map { it.encode() }
                .toSet()

            val timestamps = ((preferences[PreferencesKeys.FAP_EVENT_TIMESTAMPS] ?: emptySet())
                .mapNotNull { it.toLongOrNull() } + safeTimestamp)
                .sorted()
                .takeLast(1500)
            preferences[PreferencesKeys.FAP_EVENT_TIMESTAMPS] = timestamps.map(Long::toString).toSet()
            rebuildTrackerCache(preferences, timestamps)
        }
    }

    suspend fun updateTrackerReset(
        id: String,
        timestamp: Long,
        category: String? = null,
        note: String? = null
    ) {
        val safeTimestamp = timestamp.coerceIn(1L, System.currentTimeMillis())
        context.dataStore.edit { preferences ->
            val records = (preferences[PreferencesKeys.TRACKER_RESET_RECORDS] ?: emptySet())
                .mapNotNull(TrackerResetRecord::decode)
                .toMutableList()
            var index = records.indexOfFirst { it.id == id }
            if (index < 0 && id.startsWith("legacy-")) {
                val legacyTimestamp = id.removePrefix("legacy-").toLongOrNull()
                if (legacyTimestamp != null) {
                    records += TrackerResetRecord.legacy(legacyTimestamp)
                    index = records.lastIndex
                }
            }
            if (index < 0) return@edit
            val oldTimestamp = records[index].timestamp
            records[index] = records[index].copy(
                timestamp = safeTimestamp,
                category = category?.trim()?.takeIf { it.isNotEmpty() },
                note = note?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
            )
            preferences[PreferencesKeys.TRACKER_RESET_RECORDS] = records.map { it.encode() }.toSet()

            val timestamps = (preferences[PreferencesKeys.FAP_EVENT_TIMESTAMPS] ?: emptySet())
                .mapNotNull { it.toLongOrNull() }
                .toMutableList()
            timestamps.remove(oldTimestamp)
            timestamps += safeTimestamp
            val sorted = timestamps.sorted().takeLast(1500)
            preferences[PreferencesKeys.FAP_EVENT_TIMESTAMPS] = sorted.map(Long::toString).toSet()
            rebuildTrackerCache(preferences, sorted)
        }
    }

    suspend fun deleteTrackerReset(id: String) {
        context.dataStore.edit { preferences ->
            val records = (preferences[PreferencesKeys.TRACKER_RESET_RECORDS] ?: emptySet())
                .mapNotNull(TrackerResetRecord::decode)
                .toMutableList()
            val structured = records.firstOrNull { it.id == id }
            val legacyTimestamp = id.takeIf { it.startsWith("legacy-") }
                ?.removePrefix("legacy-")
                ?.toLongOrNull()
            val targetTimestamp = structured?.timestamp ?: legacyTimestamp ?: return@edit
            records.removeAll { it.id == id || (legacyTimestamp != null && it.timestamp == legacyTimestamp) }
            preferences[PreferencesKeys.TRACKER_RESET_RECORDS] = records.map { it.encode() }.toSet()

            val timestamps = (preferences[PreferencesKeys.FAP_EVENT_TIMESTAMPS] ?: emptySet())
                .mapNotNull { it.toLongOrNull() }
                .toMutableList()
            timestamps.remove(targetTimestamp)
            val sorted = timestamps.sorted().takeLast(1500)
            preferences[PreferencesKeys.FAP_EVENT_TIMESTAMPS] = sorted.map(Long::toString).toSet()
            rebuildTrackerCache(preferences, sorted)
        }
    }

    suspend fun saveDailyCheckIn(localDate: String, rating: String) {
        val checkIn = runCatching {
            DailyCheckIn(java.time.LocalDate.parse(localDate), rating.lowercase())
        }.getOrNull() ?: return
        if (checkIn.rating !in DailyCheckIn.allowedRatings) return
        context.dataStore.edit { preferences ->
            val items = (preferences[PreferencesKeys.DAILY_CHECK_INS] ?: emptySet())
                .mapNotNull(DailyCheckIn::decode)
                .filterNot { it.localDate == checkIn.localDate }
                .toMutableList()
            items += checkIn
            preferences[PreferencesKeys.DAILY_CHECK_INS] = items.map { it.encode() }.toSet()
        }
    }

    suspend fun saveSleepEntry(
        localDate: String,
        durationMinutes: Int,
        note: String? = null
    ) {
        val date = runCatching { java.time.LocalDate.parse(localDate) }.getOrNull() ?: return
        val entry = SleepEntry(
            localDate = date,
            durationMinutes = durationMinutes.coerceIn(0, 24 * 60),
            note = note?.trim()?.take(500)?.takeIf { it.isNotBlank() }
        )
        context.dataStore.edit { preferences ->
            val items = (preferences[PreferencesKeys.SLEEP_ENTRIES] ?: emptySet())
                .mapNotNull(SleepEntry::decode)
                .filterNot { it.localDate == date }
                .toMutableList()
            items += entry
            preferences[PreferencesKeys.SLEEP_ENTRIES] = items
                .sortedByDescending { it.localDate }
                .take(730)
                .map { it.encode() }
                .toSet()
        }
    }

    suspend fun setTrackerGoalDays(days: Int?) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRACKER_GOAL_DAYS] = (days ?: 0).coerceIn(0, 3650)
        }
    }

    suspend fun setDashboardLayout(order: List<String>, hidden: Set<String>) {
        val defaults = defaultDashboardCardOrder()
        val normalized = (order.filter { it in defaults } + defaults).distinct()
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DASHBOARD_CARD_ORDER] = normalized.joinToString(",")
            preferences[PreferencesKeys.DASHBOARD_HIDDEN_CARDS] = hidden.filter { it in defaults }.toSet()
        }
    }

    suspend fun markMilestoneSeen(days: Int) {
        if (days <= 0) return
        context.dataStore.edit { preferences ->
            val current = (preferences[PreferencesKeys.SEEN_MILESTONES] ?: emptySet()).toMutableSet()
            current += days.toString()
            preferences[PreferencesKeys.SEEN_MILESTONES] = current
        }
    }

    suspend fun resetStreakTracker(nowMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRACKER_STARTED_AT] = nowMillis
            preferences.remove(PreferencesKeys.LAST_FAP_AT)
            preferences.remove(PreferencesKeys.PREVIOUS_FAP_STREAK_DAYS)
            preferences.remove(PreferencesKeys.BEST_FAP_STREAK_DAYS)
            preferences.remove(PreferencesKeys.FAP_LOG_COUNT)
            preferences.remove(PreferencesKeys.FAP_EVENT_TIMESTAMPS)
            preferences.remove(PreferencesKeys.TRACKER_RESET_RECORDS)
            preferences.remove(PreferencesKeys.DAILY_CHECK_INS)
            preferences.remove(PreferencesKeys.SLEEP_ENTRIES)
            preferences.remove(PreferencesKeys.SEEN_MILESTONES)
        }
    }

    suspend fun setBlockerStyle(style: String) {
        val normalized = style.lowercase().trim()
        val allowed = setOf("classic", "pulse", "focus", "photo")
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLOCKER_STYLE] =
                if (normalized in allowed) normalized else "classic"
        }
    }

    suspend fun setBlockerPhotoUri(uri: String?) {
        context.dataStore.edit { preferences ->
            val value = uri?.trim().orEmpty()
            if (value.isBlank()) preferences.remove(PreferencesKeys.BLOCKER_PHOTO_URI)
            else preferences[PreferencesKeys.BLOCKER_PHOTO_URI] = value
        }
    }

    suspend fun setBlockerMessage(message: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLOCKER_MESSAGE] =
                message.trim().take(120).ifBlank { "Stay with the plan. This can wait." }
        }
    }

    suspend fun setShowPackageOnBlocker(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_PACKAGE_ON_BLOCKER] = enabled
        }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REDUCE_MOTION] = enabled
        }
    }

    suspend fun setBlockHistoryRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLOCK_HISTORY_RETENTION_DAYS] =
                days.coerceIn(7, 365)
        }
    }

    suspend fun saveSignedInAccount(
        id: String,
        email: String,
        displayName: String?,
        photoUri: String?
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCOUNT_SIGNED_IN] = true
            preferences[PreferencesKeys.ACCOUNT_ID] = id
            preferences[PreferencesKeys.ACCOUNT_EMAIL] = email
            preferences[PreferencesKeys.ACCOUNT_DISPLAY_NAME] = displayName?.trim().orEmpty()
            preferences[PreferencesKeys.ACCOUNT_PHOTO_URI] = photoUri?.trim().orEmpty()
        }
    }

    suspend fun signOutKeepRememberedAccount() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCOUNT_SIGNED_IN] = false
        }
    }

    suspend fun forgetRememberedAccount() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCOUNT_SIGNED_IN] = false
            preferences.remove(PreferencesKeys.ACCOUNT_ID)
            preferences.remove(PreferencesKeys.ACCOUNT_EMAIL)
            preferences.remove(PreferencesKeys.ACCOUNT_DISPLAY_NAME)
            preferences.remove(PreferencesKeys.ACCOUNT_PHOTO_URI)
        }
    }

    suspend fun incrementAdultBlocksCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.ADULT_BLOCKS_COUNT] ?: 0
            preferences[PreferencesKeys.ADULT_BLOCKS_COUNT] = current + 1
        }
    }

    private fun rebuildTrackerCache(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        timestamps: List<Long>
    ) {
        val sorted = timestamps.filter { it > 0L }.sorted()
        val now = System.currentTimeMillis()
        val start = (preferences[PreferencesKeys.TRACKER_STARTED_AT] ?: now)
            .takeIf { it > 0L }
            ?: now
        val last = sorted.lastOrNull() ?: 0L
        val previousBaseline = if (sorted.size >= 2) sorted[sorted.lastIndex - 1] else start
        val previous = if (last > 0L) {
            ((last - previousBaseline).coerceAtLeast(0L) / 86_400_000L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        } else 0

        var baseline = start
        var best = 0
        sorted.forEach { event ->
            val days = ((event - baseline).coerceAtLeast(0L) / 86_400_000L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            best = maxOf(best, days)
            baseline = maxOf(baseline, event)
        }

        if (last > 0L) preferences[PreferencesKeys.LAST_FAP_AT] = last
        else preferences.remove(PreferencesKeys.LAST_FAP_AT)
        preferences[PreferencesKeys.PREVIOUS_FAP_STREAK_DAYS] = previous
        preferences[PreferencesKeys.BEST_FAP_STREAK_DAYS] = best
        preferences[PreferencesKeys.FAP_LOG_COUNT] = sorted.size
    }

    companion object {
        fun defaultDashboardCardOrder(): List<String> = listOf(
            "hero",
            "checkin",
            "graph",
            "focus",
            "calendar",
            "milestones",
            "stats",
            "insights",
            "timeline",
            "protection"
        )
    }

    /**
     * Portable, versioned backup for every DataStore preference, including tracker and focus history.
     * The user chooses where the file is stored; it is not silently uploaded anywhere.
     */
    suspend fun exportBackupJson(): String {
        val entries = JSONObject()
        context.dataStore.data.first().asMap().forEach { (key, value) ->
            val item = JSONObject()
            when (value) {
                is Boolean -> item.put("type", "boolean").put("value", value)
                is Int -> item.put("type", "int").put("value", value)
                is Long -> item.put("type", "long").put("value", value)
                is Float -> item.put("type", "float").put("value", value.toDouble())
                is Double -> item.put("type", "double").put("value", value)
                is String -> item.put("type", "string").put("value", value)
                is Set<*> -> {
                    val values = JSONArray()
                    value.filterIsInstance<String>().forEach(values::put)
                    item.put("type", "string_set").put("value", values)
                }
                else -> error("Unsupported preference type for ${key.name}")
            }
            entries.put(key.name, item)
        }
        return JSONObject()
            .put("format", "no-goon-data-backup")
            .put("schema", 1)
            .put("applicationId", "com.aistudio.strictblocker.kzvwpq")
            .put("createdAt", System.currentTimeMillis())
            .put("entries", entries)
            .toString(2)
    }

    /**
     * Restores only a validated No goon backup. Existing preferences are replaced atomically.
     */
    suspend fun importBackupJson(json: String) {
        val root = JSONObject(json)
        require(root.optString("format") == "no-goon-data-backup") { "This is not a No goon backup." }
        require(root.optInt("schema") == 1) { "This backup version is not supported." }
        require(root.optString("applicationId") == "com.aistudio.strictblocker.kzvwpq") {
            "This backup belongs to a different app."
        }
        val entries = root.getJSONObject("entries")
        context.dataStore.edit { preferences ->
            preferences.clear()
            val iterator = entries.keys()
            while (iterator.hasNext()) {
                val name = iterator.next()
                val item = entries.getJSONObject(name)
                when (item.getString("type")) {
                    "boolean" -> preferences[booleanPreferencesKey(name)] = item.getBoolean("value")
                    "int" -> preferences[intPreferencesKey(name)] = item.getInt("value")
                    "long" -> preferences[longPreferencesKey(name)] = item.getLong("value")
                    "float" -> preferences[floatPreferencesKey(name)] = item.getDouble("value").toFloat()
                    "double" -> preferences[doublePreferencesKey(name)] = item.getDouble("value")
                    "string" -> preferences[stringPreferencesKey(name)] = item.getString("value")
                    "string_set" -> {
                        val values = item.getJSONArray("value")
                        preferences[stringSetPreferencesKey(name)] = buildSet {
                            for (index in 0 until values.length()) add(values.getString(index))
                        }
                    }
                    else -> error("Unknown backup preference type.")
                }
            }
        }
    }

    /**
     * Clears all persisted preferences when the user explicitly requests a full local-data wipe.
     * All settings flows have safe defaults when their keys are absent.
     */
    suspend fun clearAllUserPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

}
