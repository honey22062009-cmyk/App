package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.system.PermissionHelper
import com.example.data.repository.AppBlockerRepository
import com.example.ui.navigation.Screen
import com.example.ui.screens.antibypass.AntiBypassGuideScreen
import com.example.ui.screens.apps.AppsScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.onboarding.PermissionsScreen
import com.example.ui.screens.schedules.SchedulesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.StrictBlockerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetPackage = intent?.getStringExtra(EXTRA_TARGET_PACKAGE)

        setContent {
            StrictBlockerTheme(darkTheme = true) {
                MainAppContent(
                    initialTargetPackage = targetPackage,
                    onNewIntentTarget = { /* no-op */ }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainAppContent(
    initialTargetPackage: String?,
    onNewIntentTarget: (String?) -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) {
        (context.applicationContext as? StrictBlockerApplication)?.container?.repository
            ?: AppBlockerRepository(context)
    }
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember {
        mutableStateOf<Screen>(
            if (!initialTargetPackage.isNullOrEmpty()) Screen.Schedules else Screen.Dashboard
        )
    }

    var selectedSchedulePackage by remember {
        mutableStateOf(initialTargetPackage)
    }

    // Intercept back button when on sub-screens
    BackHandler(enabled = currentScreen != Screen.Dashboard) {
        currentScreen = Screen.Dashboard
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet) {
            // Tablet Layout with NavigationRail
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NavigationRail(
                    modifier = Modifier.testTag("tablet_navigation_rail"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Strict Blocker",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier
                                .size(36.dp)
                                .padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                ) {
                    Screen.navItems.forEach { screen ->
                        val isSelected = currentScreen.route == screen.route
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title, fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color(0xFF38BDF8),
                                indicatorColor = Color(0xFF0284C7)
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    ScreenContent(
                        currentScreen = currentScreen,
                        repository = repository,
                        selectedSchedulePackage = selectedSchedulePackage,
                        onNavigateToScreen = { screen -> currentScreen = screen },
                        onConfigureScheduleForApp = { pkg ->
                            selectedSchedulePackage = pkg
                            currentScreen = Screen.Schedules
                        }
                    )
                }
            }
        } else {
            // Phone Layout with Bottom NavigationBar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.testTag("phone_bottom_navigation"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Screen.navItems.forEach { screen ->
                            val isSelected = currentScreen.route == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color(0xFF38BDF8),
                                    indicatorColor = Color(0xFF0284C7)
                                ),
                                modifier = Modifier.testTag("nav_item_${screen.route}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    ScreenContent(
                        currentScreen = currentScreen,
                        repository = repository,
                        selectedSchedulePackage = selectedSchedulePackage,
                        onNavigateToScreen = { screen -> currentScreen = screen },
                        onConfigureScheduleForApp = { pkg ->
                            selectedSchedulePackage = pkg
                            currentScreen = Screen.Schedules
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenContent(
    currentScreen: Screen,
    repository: AppBlockerRepository,
    selectedSchedulePackage: String?,
    onNavigateToScreen: (Screen) -> Unit,
    onConfigureScheduleForApp: (String) -> Unit
) {
    when (currentScreen) {
        Screen.Dashboard -> {
            DashboardScreen(
                repository = repository,
                onNavigateToApps = { onNavigateToScreen(Screen.Apps) },
                onNavigateToPermissions = { onNavigateToScreen(Screen.Permissions) },
                onNavigateToSchedules = { onNavigateToScreen(Screen.Schedules) }
            )
        }
        Screen.Apps -> {
            AppsScreen(
                repository = repository,
                onConfigureScheduleForApp = onConfigureScheduleForApp
            )
        }
        Screen.Schedules -> {
            SchedulesScreen(
                repository = repository,
                initialSelectedPackage = selectedSchedulePackage,
                onNavigateToApps = { onNavigateToScreen(Screen.Apps) }
            )
        }
        Screen.Permissions -> {
            PermissionsScreen(
                onContinue = { onNavigateToScreen(Screen.Dashboard) }
            )
        }
        Screen.Settings -> {
            SettingsScreen(
                repository = repository,
                onNavigateToPermissions = { onNavigateToScreen(Screen.Permissions) },
                onNavigateToAntiBypassGuide = { onNavigateToScreen(Screen.AntiBypassGuide) },
                onNavigateToLegal = { onNavigateToScreen(Screen.Legal) }
            )
        }
        Screen.AntiBypassGuide -> {
            AntiBypassGuideScreen(
                onNavigateBack = { onNavigateToScreen(Screen.Settings) }
            )
        }
        Screen.Legal -> {
            com.example.ui.screens.legal.LegalComplianceScreen(
                repository = repository,
                onNavigateBack = { onNavigateToScreen(Screen.Settings) }
            )
        }
    }
}
