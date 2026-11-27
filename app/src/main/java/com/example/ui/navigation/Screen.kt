package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    data object Progress : Screen("progress", "Progress", Icons.Filled.Insights, Icons.Outlined.Insights)
    data object Focus : Screen("focus", "Focus", Icons.Filled.Timer, Icons.Outlined.Timer)
    data object Apps : Screen("apps", "Apps", Icons.Filled.Apps, Icons.Outlined.Apps)
    data object Schedules : Screen("schedules", "Schedules", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    data object AdultProtection : Screen("adult_protection", "Browser Guard", Icons.Filled.Shield, Icons.Outlined.Shield)
    data object Permissions : Screen("permissions", "Permissions", Icons.Filled.Security, Icons.Outlined.Security)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    data object AntiBypassGuide : Screen("anti_bypass_guide", "Security Guide", Icons.Filled.Security, Icons.Outlined.Security)
    data object Legal : Screen("legal", "Privacy & Legal", Icons.Filled.Security, Icons.Outlined.Security)

    companion object {
        val navItems: List<Screen>
            get() = listOf(Dashboard, Progress, Focus, Apps, Schedules, Settings)
    }
}
