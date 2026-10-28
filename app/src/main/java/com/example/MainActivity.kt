package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.payments.OwnerAccess
import com.example.core.system.TrackerReminderScheduler
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
        intent?.removeExtra(EXTRA_TRACKER_ACTION)

        setContent {
            val context = LocalContext.current
            val repository = remember(context) {
                (context.applicationContext as? StrictBlockerApplication)?.container?.repository
                    ?: AppBlockerRepository(context)
            }
            val themeMode by repository.settings.themeModeFlow.collectAsState(initial = "light")
            val accentStyle by repository.settings.accentStyleFlow.collectAsState(initial = "rose")
            val reduceMotion by repository.settings.reduceMotionFlow.collectAsState(initial = false)
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
                if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
            }
            StrictBlockerTheme(darkTheme = isDark, accentStyle = accentStyle) {
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
            intent.removeExtra(EXTRA_TRACKER_ACTION)
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
    var isSplashVisible by rememberSaveable { mutableStateOf(true) }

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
    var dataEntryVisible by remember { mutableStateOf(false) }
    var pendingTrackerAction by remember(initialTrackerAction) { mutableStateOf(initialTrackerAction) }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // Resolve persisted onboarding before exposing navigation. A schedule deep link
    // remains the highest-priority destination.
    LaunchedEffect(repository, initialTargetPackage, initialTrackerAction) {
        val onboardingCompleted = repository.settings.isOnboardingCompletedFlow.first()
        if (!onboardingCompleted) {
            currentScreen = Screen.Onboarding
        } else if (!initialTargetPackage.isNullOrEmpty()) {
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

    // Local navigation must never wait for Firebase or a network connection.
    LaunchedEffect(repository) {
        repository.settings.refreshExpiredProEntitlement()
        OwnerAccess.restore()
        TrackerReminderScheduler.refresh(context)
        val requestId = repository.settings.lastPaymentRequestIdFlow.first()
        if (requestId.isNotBlank()) {
            ManualPaymentManager(context.applicationContext, repository.settings)
                .refreshUserRequestOnce(requestId)
        }
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
                if (currentScreen != Screen.Onboarding && currentScreen != Screen.Permissions &&
                    !dataEntryVisible && !keyboardVisible) {
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
                            val order = Screen.navItems
                            val direction = if (order.indexOf(targetState) >= order.indexOf(initialState)) 1 else -1
                            ((slideInHorizontally(tween(320)) { it / 5 * direction } + fadeIn(tween(240))) togetherWith
                                (slideOutHorizontally(tween(260)) { -it / 6 * direction } + fadeOut(tween(180))))
                                .using(SizeTransform(clip = true))
                        }
                    },
                    label = "screen_transition"
                ) { targetScreen ->
                    ScreenContent(
                        currentScreen = targetScreen,
                        repository = repository,
                        selectedSchedulePackage = selectedSchedulePackage,
                        initialTrackerAction = pendingTrackerAction,
                        onTrackerActionConsumed = { pendingTrackerAction = null },
                        onDataEntryVisibilityChanged = { dataEntryVisible = it },
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
    onDataEntryVisibilityChanged: (Boolean) -> Unit = {},
    onTrackerActionConsumed: () -> Unit = {},
    onNavigateToScreen: (Screen) -> Unit,
    onOpenAccountDialog: () -> Unit,
    onOpenPaywallDialog: () -> Unit,
    onConfigureScheduleForApp: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    when (currentScreen) {
        Screen.Dashboard, Screen.Progress -> {
            DashboardScreen(
                repository = repository,
                initialTrackerAction = initialTrackerAction,
                progressOnly = currentScreen == Screen.Progress,
                onTrackerActionConsumed = onTrackerActionConsumed,
                onDataEntryVisibilityChanged = onDataEntryVisibilityChanged,
                onNavigateToApps = { onNavigateToScreen(Screen.Apps) },
                onNavigateToPermissions = { onNavigateToScreen(Screen.Permissions) },
                onNavigateToSchedules = { onNavigateToScreen(Screen.Schedules) },
                onNavigateToAdultProtection = { onNavigateToScreen(Screen.AdultProtection) },
                onOpenPro = { onNavigateToScreen(Screen.Paywall) }
            )
        }
        Screen.Apps -> {
            AppsScreen(
                repository = repository,
                onConfigureScheduleForApp = onConfigureScheduleForApp,
                onNavigateToPaywall = { onNavigateToScreen(Screen.Paywall) }
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
                settings = repository.settings,
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
                onNavigateToPaywall = { onNavigateToScreen(Screen.Paywall) },
                onNavigateToBlockerCustomization = { onNavigateToScreen(Screen.BlockerCustomization) },
                onNavigateToAccount = onOpenAccountDialog,
                onNavigateToInsights = { onNavigateToScreen(Screen.Insights) },
                onReplayTour = {
                    scope.launch {
                        repository.settings.setQuickTourSeen(false)
                        onNavigateToScreen(Screen.Dashboard)
                    }
                },
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
                onUpgrade = { onNavigateToScreen(Screen.Paywall) },
                onBack = { onNavigateToScreen(Screen.Settings) }
            )
        }
        Screen.Paywall -> {
            PaywallScreen(
                settings = repository.settings,
                onBack = { onNavigateToScreen(Screen.Settings) },
                onOpenCustomization = { onNavigateToScreen(Screen.BlockerCustomization) },
                onOpenInsights = { onNavigateToScreen(Screen.Insights) },
                onOpenProgress = { onNavigateToScreen(Screen.Progress) }
            )
        }
        Screen.BlockerCustomization -> {
            BlockerCustomizationScreen(
                settings = repository.settings,
                onUpgrade = { onNavigateToScreen(Screen.Paywall) },
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
                onPreviewPro = { onNavigateToScreen(Screen.BlockerCustomization) },
                onBack = { onNavigateToScreen(Screen.Settings) }
            )
        }
    }
}
