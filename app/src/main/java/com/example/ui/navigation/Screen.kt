package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    )

    data object Apps : Screen(
        route = "apps",
        title = "Apps",
        selectedIcon = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps
    )

    data object Schedules : Screen(
        route = "schedules",
        title = "Schedules",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )

    data object AdultProtection : Screen(
        route = "adult_protection",
        title = "18+ Shield",
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield
    )

    data object Permissions : Screen(
        route = "permissions",
        title = "Permissions",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object AntiBypassGuide : Screen(
        route = "anti_bypass_guide",
        title = "Security Guide",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security
    )

    data object Legal : Screen(
        route = "legal",
        title = "Privacy & Legal",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security
    )

    companion object {
        val navItems: List<Screen>
            get() = listOf(Dashboard, Apps, Schedules, AdultProtection, Settings)
    }
}
