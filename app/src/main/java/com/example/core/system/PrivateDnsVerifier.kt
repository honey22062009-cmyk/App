package com.example.core.system

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.util.Locale

/**
 * Option B: inspect Android Private DNS only. This app never changes the device-wide
 * DNS setting and does not create a VPN or DNS tunnel.
 */
object PrivateDnsVerifier {
    const val FAMILY_FILTER_HOSTNAME = "family-filter-dns.cleanbrowsing.org"

    enum class State {
        CONFIGURED,
        NEEDS_SETUP,
        WRONG_HOSTNAME,
        UNSUPPORTED,
        UNREADABLE
    }

    data class Result(
        val state: State,
        val configuredHostname: String,
        val expectedHostname: String
    ) {
        val hostnameMatches: Boolean get() = state == State.CONFIGURED
        val displayMessage: String
            get() = when (state) {
                State.CONFIGURED -> "Private DNS hostname matches. DNS connectivity has not been tested."
                State.NEEDS_SETUP -> "Private DNS is off or opportunistic. A provider hostname is needed."
                State.WRONG_HOSTNAME -> "A different Private DNS hostname is configured."
                State.UNSUPPORTED -> "Android Private DNS hostname mode requires Android 9 or later."
                State.UNREADABLE -> "Android did not expose the current Private DNS setting."
            }
    }

    fun read(context: Context, expectedHostname: String): Result {
        val expected = normalizeHostname(expectedHostname)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return Result(State.UNSUPPORTED, "", expected)
        }
        return try {
            val mode = Settings.Global.getString(context.contentResolver, "private_dns_mode")
            val configured = Settings.Global.getString(context.contentResolver, "private_dns_specifier")
                .orEmpty().let(::normalizeHostname)
            evaluate(mode, configured, expected)
        } catch (_: SecurityException) {
            Result(State.UNREADABLE, "", expected)
        } catch (_: Exception) {
            Result(State.UNREADABLE, "", expected)
        }
    }

    fun evaluate(mode: String?, configuredHostname: String?, expectedHostname: String): Result {
        val expected = normalizeHostname(expectedHostname)
        val configured = normalizeHostname(configuredHostname.orEmpty())
        val state = when {
            expected.isBlank() -> State.NEEDS_SETUP
            mode.equals("hostname", ignoreCase = true) && configured == expected -> State.CONFIGURED
            mode.equals("hostname", ignoreCase = true) -> State.WRONG_HOSTNAME
            mode.equals("off", ignoreCase = true) ||
                mode.equals("opportunistic", ignoreCase = true) -> State.NEEDS_SETUP
            else -> State.UNREADABLE
        }
        return Result(state, configured, expected)
    }

    fun openSettings(context: Context) {
        val intent = Intent("android.settings.PRIVATE_DNS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            context.startActivity(
                Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun normalizeHostname(value: String): String =
        value.trim().trimEnd('.').lowercase(Locale.ROOT)
}
