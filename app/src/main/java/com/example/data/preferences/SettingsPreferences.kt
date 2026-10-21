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
        val ADULT_BLOCKS_COUNT = intPreferencesKey("adult_blocks_count")
        val APP_GROUPS = stringSetPreferencesKey("app_groups")
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

    val adultBlocksCountFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ADULT_BLOCKS_COUNT] ?: 0
    }

    val appGroupsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_GROUPS] ?: emptySet()
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

    suspend fun setAppGroups(groups: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_GROUPS] = groups
        }
    }

    suspend fun incrementAdultBlocksCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.ADULT_BLOCKS_COUNT] ?: 0
            preferences[PreferencesKeys.ADULT_BLOCKS_COUNT] = current + 1
        }
    }
}
