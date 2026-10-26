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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.example.core.system.PermissionHelper
import com.example.data.repository.AppBlockerRepository
import com.example.payments.ManualPaymentManager
import com.example.ui.components.FloatingGlassDock
import com.example.ui.navigation.Screen
import com.example.ui.screens.account.AccountDialog
import com.example.ui.screens.account.AccountScreen
import com.example.ui.screens.adult.AdultProtectionScreen
import com.example.ui.screens.antibypass.AntiBypassGuideScreen
import com.example.ui.screens.apps.AppsScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.insights.InsightsScreen
import com.example.ui.screens.legal.LegalComplianceScreen
import com.example.ui.screens.onboarding.OnboardingExperienceScreen
import com.example.ui.screens.onboarding.PermissionsScreen
import com.example.ui.screens.owner.OwnerConsoleScreen
import com.example.ui.screens.pro.BlockerCustomizationScreen
import com.example.ui.screens.pro.PaywallDialog
import com.example.ui.screens.pro.PaywallScreen
import com.example.ui.screens.schedules.SchedulesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.theme.StrictBlockerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        const val EXTRA_TRACKER_ACTION = "extra_tracker_action"
    }

    // Compose observes this state, so a new intent sent to an existing Activity
    // updates the requested destination instead of being silently ignored.
    private var targetPackage by mutableStateOf<String?>(null)
    private var trackerAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        targetPackage = intent?.getStringExtra(EXTRA_TARGET_PACKAGE)
        trackerAction = intent?.getStringExtra(EXTRA_TRACKER_ACTION)

        setContent {
            val context = LocalContext.current
            val repository = remember(context) {
                (context.applicationContext as? StrictBlockerApplication)?.container?.repository
                    ?: AppBlockerRepository(context)
            }
            val themeMode by repository.settings.themeModeFlow.collectAsState(initial = "system")
            val reduceMotion by repository.settings.reduceMotionFlow.collectAsState(initial = false)
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            StrictBlockerTheme(darkTheme = isDark) {
                MainAppContent(
                    repository = repository,
                    initialTargetPackage = targetPackage,
                    initialTrackerAction = trackerAction,
                    reduceMotion = reduceMotion
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
        if (intent.hasExtra(EXTRA_TRACKER_ACTION)) {
            trackerAction = intent.getStringExtra(EXTRA_TRACKER_ACTION)
        }
    }
}

@Composable
fun MainAppContent(
    repository: AppBlockerRepository,
    initialTargetPackage: String?,
    initialTrackerAction: String?,
    reduceMotion: Boolean
) {
    var isSplashVisible by remember { mutableStateOf(true) }

    if (isSplashVisible) {
        SplashScreen(
            reduceMotion = reduceMotion,
            onSplashComplete = { isSplashVisible = false }
        )
    } else {
        MainAppScaffold(
            repository = repository,
            initialTargetPackage = initialTargetPackage,
            initialTrackerAction = initialTrackerAction,
            reduceMotion = reduceMotion
        )
    }
}

@Composable
fun MainAppScaffold(
    repository: AppBlockerRepository,
    initialTargetPackage: String?,
    initialTrackerAction: String?,
    reduceMotion: Boolean
) {
    val context = LocalContext.current
    var currentScreen by remember {
        mutableStateOf<Screen>(
            if (!initialTargetPackage.isNullOrEmpty()) Screen.Schedules
            else if (!initialTrackerAction.isNullOrEmpty()) Screen.Dashboard
            else Screen.Onboarding
        )
    }

    var selectedSchedulePackage by remember {
        mutableStateOf(initialTargetPackage)
    }
    var isInitialRouteResolved by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showPaywallDialog by remember { mutableStateOf(false) }

    // Resolve persisted onboarding before exposing navigation. A schedule deep link
    // remains the highest-priority destination.
    LaunchedEffect(repository, initialTargetPackage, initialTrackerAction) {
        repository.settings.refreshExpiredProEntitlement()
        val lastPaymentRequestId = repository.settings.lastPaymentRequestIdFlow.first()
        if (lastPaymentRequestId.isNotBlank()) {
            ManualPaymentManager(
                context.applicationContext,
                repository.settings
            ).refreshUserRequestOnce(lastPaymentRequestId)
        }

        val onboardingCompleted = repository.settings.isOnboardingCompletedFlow.first()
        if (!initialTargetPackage.isNullOrEmpty()) {
            selectedSchedulePackage = initialTargetPackage
            currentScreen = Screen.Schedules
        } else if (!initialTrackerAction.isNullOrEmpty() && onboardingCompleted) {
            currentScreen = Screen.Dashboard
        } else if (onboardingCompleted) {
            currentScreen = Screen.Dashboard
        } else {
            currentScreen = Screen.Onboarding
        }
        isInitialRouteResolved = true
    }

    // On first launch, Back should leave the permission guide rather than silently
    // navigating around the incomplete setup. Onboarding remains pending for next launch.
    BackHandler(
        enabled = currentScreen != Screen.Dashboard && currentScreen != Screen.Permissions
    ) {
        when (currentScreen) {
            Screen.Onboarding -> currentScreen = Screen.Onboarding
            Screen.AntiBypassGuide,
            Screen.Legal,
            Screen.Paywall,
            Screen.BlockerCustomization,
            Screen.Account,
            Screen.Insights,
            Screen.OwnerConsole -> currentScreen = Screen.Settings
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
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (currentScreen != Screen.Onboarding && currentScreen != Screen.Permissions) {
                    FloatingGlassDock(
                        currentScreen = currentScreen,
                        onSelect = { screen -> currentScreen = screen },
                        reduceMotion = reduceMotion
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding()
                    )
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                        } else {
                            (fadeIn(tween(210)) + scaleIn(tween(210), initialScale = 0.988f)) togetherWith
                                (fadeOut(tween(130)) + scaleOut(tween(130), targetScale = 0.996f))
                        }
                    },
                    label = "screen_transition"
                ) { targetScreen ->
                    ScreenContent(
                        currentScreen = targetScreen,
                        repository = repository,
                        selectedSchedulePackage = selectedSchedulePackage,
                        initialTrackerAction = initialTrackerAction,
                        onNavigateToScreen = { screen -> currentScreen = screen },
                        onOpenAccountDialog = {
                            showAccountDialog = true
                        },
                        onOpenPaywallDialog = { showPaywallDialog = true },
                        onConfigureScheduleForApp = { pkg ->
                            selectedSchedulePackage = pkg
                            currentScreen = Screen.Schedules
                        }
                    )
                }
            }
        }
    }

    if (showAccountDialog) {
        AccountDialog(
            settings = repository.settings,
            firstRun = false,
            onDismiss = {
                showAccountDialog = false
            }
        )
    }

    if (showPaywallDialog) {
        PaywallDialog(
            settings = repository.settings,
            onDismiss = { showPaywallDialog = false },
            onOpenCustomization = {
                showPaywallDialog = false
                currentScreen = Screen.BlockerCustomization
            }
        )
    }
}

@Composable
fun ScreenContent(
    currentScreen: Screen,
    repository: AppBlockerRepository,
    selectedSchedulePackage: String?,
    initialTrackerAction: String?,
    onNavigateToScreen: (Screen) -> Unit,
    onOpenAccountDialog: () -> Unit,
    onOpenPaywallDialog: () -> Unit,
    onConfigureScheduleForApp: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    when (currentScreen) {
        Screen.Dashboard -> {
            DashboardScreen(
                repository = repository,
                initialTrackerAction = initialTrackerAction,
                onNavigateToApps = { onNavigateToScreen(Screen.Apps) },
                onNavigateToPermissions = { onNavigateToScreen(Screen.Permissions) },
                onNavigateToSchedules = { onNavigateToScreen(Screen.Schedules) },
                onNavigateToAdultProtection = { onNavigateToScreen(Screen.AdultProtection) },
                onOpenPro = onOpenPaywallDialog
            )
        }
        Screen.Apps -> {
            AppsScreen(
                repository = repository,
                onConfigureScheduleForApp = onConfigureScheduleForApp,
                onNavigateToPaywall = onOpenPaywallDialog
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
        Screen.Onboarding -> {
            OnboardingExperienceScreen(
                settings = repository.settings,
                scope = scope,
                reduceMotion = repository.settings.reduceMotionFlow.collectAsState(initial = false).value,
                onFinished = {
                    scope.launch {
                        repository.settings.setOnboardingCompleted(true)
                        onNavigateToScreen(Screen.Permissions)
                    }
                }
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
                onNavigateToAdultProtection = { onNavigateToScreen(Screen.AdultProtection) },
                onNavigateToPaywall = onOpenPaywallDialog,
                onNavigateToBlockerCustomization = { onNavigateToScreen(Screen.BlockerCustomization) },
                onNavigateToAccount = onOpenAccountDialog,
                onNavigateToInsights = { onNavigateToScreen(Screen.Insights) },
                onNavigateToOwnerConsole = { onNavigateToScreen(Screen.OwnerConsole) }
            )
        }
        Screen.Account -> {
            AccountScreen(
                settings = repository.settings,
                onBack = { onNavigateToScreen(Screen.Settings) }
            )
        }
        Screen.Insights -> {
            InsightsScreen(
                repository = repository,
                onUpgrade = onOpenPaywallDialog,
                onBack = { onNavigateToScreen(Screen.Settings) }
            )
        }
        Screen.Paywall -> {
            PaywallScreen(
                settings = repository.settings,
                onBack = { onNavigateToScreen(Screen.Settings) },
                onOpenCustomization = { onNavigateToScreen(Screen.BlockerCustomization) }
            )
        }
        Screen.BlockerCustomization -> {
            BlockerCustomizationScreen(
                settings = repository.settings,
                onUpgrade = onOpenPaywallDialog,
                onBack = { onNavigateToScreen(Screen.Settings) }
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
        Screen.OwnerConsole -> {
            OwnerConsoleScreen(
                settings = repository.settings,
                onBack = { onNavigateToScreen(Screen.Settings) }
            )
        }
    }
}
