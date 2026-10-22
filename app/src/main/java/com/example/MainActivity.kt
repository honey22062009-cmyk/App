package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.system.PermissionHelper
import com.example.data.repository.AppBlockerRepository
import com.example.ui.navigation.Screen
import com.example.ui.screens.adult.AdultProtectionScreen
import com.example.ui.screens.antibypass.AntiBypassGuideScreen
import com.example.ui.screens.apps.AppsScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.legal.LegalComplianceScreen
import com.example.ui.screens.onboarding.PermissionsScreen
import com.example.ui.screens.schedules.SchedulesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.theme.StrictBlockerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
    }

    // Compose observes this state, so a new intent sent to an existing Activity
    // updates the requested destination instead of being silently ignored.
    private var targetPackage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        targetPackage = intent?.getStringExtra(EXTRA_TARGET_PACKAGE)

        setContent {
            val context = LocalContext.current
            val repository = remember(context) {
                (context.applicationContext as? StrictBlockerApplication)?.container?.repository
                    ?: AppBlockerRepository(context)
            }
            val themeMode by repository.settings.themeModeFlow.collectAsState(initial = "system")
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            StrictBlockerTheme(darkTheme = isDark) {
                MainAppContent(
                    repository = repository,
                    initialTargetPackage = targetPackage
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Normal launcher intents should not reset navigation. Only reroute when an
        // explicit schedule deep-link target is supplied.
        if (intent.hasExtra(EXTRA_TARGET_PACKAGE)) {
            targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        }
    }
}

@Composable
fun MainAppContent(
    repository: AppBlockerRepository,
    initialTargetPackage: String?
) {
    var isSplashVisible by remember { mutableStateOf(true) }

    if (isSplashVisible) {
        SplashScreen(
            onSplashComplete = { isSplashVisible = false }
        )
    } else {
        MainAppScaffold(
            repository = repository,
            initialTargetPackage = initialTargetPackage
        )
    }
}

@Composable
fun MainAppScaffold(
    repository: AppBlockerRepository,
    initialTargetPackage: String?
) {
    var currentScreen by remember {
        mutableStateOf<Screen>(
            if (!initialTargetPackage.isNullOrEmpty()) Screen.Schedules else Screen.Permissions
        )
    }

    var selectedSchedulePackage by remember {
        mutableStateOf(initialTargetPackage)
    }
    var isInitialRouteResolved by remember { mutableStateOf(false) }

    // Resolve persisted onboarding before exposing navigation. A schedule deep link
    // remains the highest-priority destination.
    LaunchedEffect(repository, initialTargetPackage) {
        val onboardingCompleted = repository.settings.isOnboardingCompletedFlow.first()
        if (!initialTargetPackage.isNullOrEmpty()) {
            selectedSchedulePackage = initialTargetPackage
            currentScreen = Screen.Schedules
        } else if (onboardingCompleted) {
            currentScreen = Screen.Dashboard
        } else {
            currentScreen = Screen.Permissions
        }
        isInitialRouteResolved = true
    }

    // On first launch, Back should leave the permission guide rather than silently
    // navigating around the incomplete setup. Onboarding remains pending for next launch.
    BackHandler(
        enabled = currentScreen != Screen.Dashboard && currentScreen != Screen.Permissions
    ) {
        when (currentScreen) {
            Screen.AntiBypassGuide, Screen.Legal -> currentScreen = Screen.Settings
            else -> currentScreen = Screen.Dashboard
        }
    }

    if (!isInitialRouteResolved) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
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
                            tint = MaterialTheme.colorScheme.primary,
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
                            label = {
                                Text(
                                    screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.992f)) togetherWith
                                (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.996f))
                        },
                        label = "tablet_screen_transition"
                    ) { targetScreen ->
                        ScreenContent(
                            currentScreen = targetScreen,
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
        } else {
            // Phone Layout with Bottom NavigationBar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.testTag("phone_bottom_navigation"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
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
                                label = {
                                    Text(
                                        screen.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.992f)) togetherWith
                                (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.996f))
                        },
                        label = "phone_screen_transition"
                    ) { targetScreen ->
                        ScreenContent(
                            currentScreen = targetScreen,
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
    val scope = rememberCoroutineScope()
    when (currentScreen) {
        Screen.Dashboard -> {
            DashboardScreen(
                repository = repository,
                onNavigateToApps = { onNavigateToScreen(Screen.Apps) },
                onNavigateToPermissions = { onNavigateToScreen(Screen.Permissions) },
                onNavigateToSchedules = { onNavigateToScreen(Screen.Schedules) },
                onNavigateToAdultProtection = { onNavigateToScreen(Screen.AdultProtection) }
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
        Screen.AdultProtection -> {
            AdultProtectionScreen(
                repository = repository,
                onNavigateToPermissions = { onNavigateToScreen(Screen.Permissions) }
            )
        }
        Screen.Permissions -> {
            PermissionsScreen(
                onContinue = {
                    scope.launch {
                        repository.settings.setOnboardingCompleted(true)
                        onNavigateToScreen(Screen.Dashboard)
                    }
                }
            )
        }
        Screen.Settings -> {
            SettingsScreen(
                repository = repository,
                onNavigateToPermissions = { onNavigateToScreen(Screen.Permissions) },
                onNavigateToAntiBypassGuide = { onNavigateToScreen(Screen.AntiBypassGuide) },
                onNavigateToLegal = { onNavigateToScreen(Screen.Legal) },
                onNavigateToAdultProtection = { onNavigateToScreen(Screen.AdultProtection) }
            )
        }
        Screen.AntiBypassGuide -> {
            AntiBypassGuideScreen(
                onNavigateBack = { onNavigateToScreen(Screen.Settings) }
            )
        }
        Screen.Legal -> {
            LegalComplianceScreen(
                repository = repository,
                onNavigateBack = { onNavigateToScreen(Screen.Settings) }
            )
        }
    }
}
