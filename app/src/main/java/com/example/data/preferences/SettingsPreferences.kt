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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "strict_blocker_settings")

class SettingsPreferences(private val context: Context) {

    private object PreferencesKeys {
        val PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        val STRICT_CHANGE_DELAY_MINUTES = intPreferencesKey("strict_change_delay_minutes")
        val PENDING_CHANGE_UNLOCK_TIME = longPreferencesKey("pending_change_unlock_time")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SHOW_OVERLAY_SHIELD = booleanPreferencesKey("show_overlay_shield")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"

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

        // Pro / premium blocker customization
        val PRO_ENTITLED = booleanPreferencesKey("pro_entitled")
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
        preferences[PreferencesKeys.THEME_MODE] ?: "system"
    }

    val isProtectionEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PROTECTION_ENABLED] ?: true
    }

    val strictChangeDelayMinutesFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.STRICT_CHANGE_DELAY_MINUTES] ?: 0
    }

    val pendingChangeUnlockTimeFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PENDING_CHANGE_UNLOCK_TIME] ?: 0L
    }

    val isOnboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    val isOverlayShieldEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_OVERLAY_SHIELD] ?: true
    }

    // Adult Content Protection Flows
    val isAdultProtectionEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ADULT_PROTECTION_ENABLED] ?: true
    }

    val isStrictAdultModeFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.STRICT_ADULT_MODE] ?: true }
    val adultUnlockNotBeforeFlow: Flow<Long> = context.dataStore.data.map { it[PreferencesKeys.ADULT_UNLOCK_NOT_BEFORE] ?: 0L }
    val adultUnlockVerifiedFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.ADULT_UNLOCK_VERIFIED] ?: false }
    val adultCooldownSecondsFlow: Flow<Int> = context.dataStore.data.map { it[PreferencesKeys.ADULT_COOLDOWN_SECONDS] ?: 60 }

    val isBlockIncognitoEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BLOCK_INCOGNITO_ENABLED] ?: true
    }

    val isForceSafeSearchEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FORCE_SAFESEARCH_ENABLED] ?: true
    }

    val isDnsFilterEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DNS_FILTER_ENABLED] ?: true
    }

    val customDnsPrimaryFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_DNS_PRIMARY] ?: "1.1.1.3"
    }

    val customDnsSecondaryFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_DNS_SECONDARY] ?: "1.0.0.3"
    }

    val adultBlocksCountFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ADULT_BLOCKS_COUNT] ?: 0
    }

    val appGroupsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_GROUPS] ?: emptySet()
    }

    val isProEntitledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PRO_ENTITLED] ?: false
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
        if (enabled) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ADULT_PROTECTION_ENABLED] = true
                preferences[PreferencesKeys.STRICT_ADULT_MODE] = true
                preferences[PreferencesKeys.ADULT_UNLOCK_NOT_BEFORE] = 0L
                preferences[PreferencesKeys.ADULT_UNLOCK_VERIFIED] = false
                preferences[PreferencesKeys.BLOCK_INCOGNITO_ENABLED] = true
                preferences[PreferencesKeys.FORCE_SAFESEARCH_ENABLED] = true
                preferences[PreferencesKeys.DNS_FILTER_ENABLED] = true
            }
        } else {
            requestAdultProtectionDisable()
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

    suspend fun setBlockIncognitoEnabled(enabled: Boolean) {
        if (isStrictAdultModeFlow.first() && !enabled) return
        context.dataStore.edit { it[PreferencesKeys.BLOCK_INCOGNITO_ENABLED] = enabled }
    }

    suspend fun setForceSafeSearchEnabled(enabled: Boolean) {
        if (isStrictAdultModeFlow.first() && !enabled) return
        context.dataStore.edit { it[PreferencesKeys.FORCE_SAFESEARCH_ENABLED] = enabled }
    }

    suspend fun setDnsFilterEnabled(enabled: Boolean) {
        if (isStrictAdultModeFlow.first() && !enabled) return
        context.dataStore.edit { it[PreferencesKeys.DNS_FILTER_ENABLED] = enabled }
    }

    suspend fun setCustomDns(primary: String, secondary: String) {
        context.dataStore.edit {
            it[PreferencesKeys.CUSTOM_DNS_PRIMARY] = primary.trim()
            it[PreferencesKeys.CUSTOM_DNS_SECONDARY] = secondary.trim()
        }
    }

    suspend fun setAppGroups(groups: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_GROUPS] = groups
        }
    }

    suspend fun setProEntitled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRO_ENTITLED] = enabled
            if (!enabled) {
                preferences[PreferencesKeys.BLOCKER_STYLE] = "classic"
                preferences.remove(PreferencesKeys.BLOCKER_PHOTO_URI)
            }
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
