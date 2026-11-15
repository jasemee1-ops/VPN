package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.LuckBoostScreen
import com.example.ui.screens.ServersSettingsScreen
import com.example.ui.screens.VpnHomeScreen
import com.example.ui.theme.ChampGold
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldContainer
import com.example.ui.theme.StadiumNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

const val ROUTE_VPN_HOME = "vpn_home"
const val ROUTE_LUCK_BOOST = "luck_boost"
const val ROUTE_SERVERS_SETTINGS = "servers_settings"

private data class NavDestinationItem(
    val route: String,
    val labelAr: String,
    val labelEn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PesVpnViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isArabic = uiState.settings.isArabic

    // Request Android 13+ notification permission gracefully so persistent VPN status notification displays cleanly
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Service continues cleanly even if notification denied */ }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // System VPN consent launcher (triggered automatically if Auto-Connect Germany is enabled)
    val vpnConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            viewModel.onVpnConsentResult(result.resultCode == Activity.RESULT_OK)
        }
    )

    val pendingIntent = uiState.pendingVpnConsentIntent
    LaunchedEffect(pendingIntent) {
        if (pendingIntent != null) {
            vpnConsentLauncher.launch(pendingIntent)
        }
    }

    val destinations = remember {
        listOf(
            NavDestinationItem(
                route = ROUTE_VPN_HOME,
                labelAr = "VPN ألمانيا",
                labelEn = "Germany VPN",
                selectedIcon = Icons.Filled.Shield,
                unselectedIcon = Icons.Outlined.Shield,
                testTag = "nav_vpn_home"
            ),
            NavDestinationItem(
                route = ROUTE_LUCK_BOOST,
                labelAr = "بوست الحظ",
                labelEn = "Luck Boost",
                selectedIcon = Icons.Filled.Bolt,
                unselectedIcon = Icons.Outlined.Bolt,
                testTag = "nav_luck_boost"
            ),
            NavDestinationItem(
                route = ROUTE_SERVERS_SETTINGS,
                labelAr = "السيرفرات",
                labelEn = "Servers",
                selectedIcon = Icons.Filled.Dns,
                unselectedIcon = Icons.Outlined.Dns,
                testTag = "nav_servers_settings"
            )
        )
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_VPN_HOME

    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(StadiumNavy, ObsidianBg)
                    )
                )
        ) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (isArabic) "PES VPN ألمانيا" else "PES VPN Germany",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            Text(
                                text = "DE • 84.200.69.80",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampGold,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ObsidianBg.copy(alpha = 0.72f)
                        )
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = StadiumNavy.copy(alpha = 0.96f),
                            tonalElevation = 8.dp
                        ) {
                            destinations.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = if (isArabic) item.labelAr else item.labelEn
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (isArabic) item.labelAr else item.labelEn,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PitchEmerald,
                                        selectedTextColor = PitchEmerald,
                                        indicatorColor = PitchEmeraldContainer,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = StadiumNavy.copy(alpha = 0.92f),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.weight(0.1f))
                            destinations.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = if (isArabic) item.labelAr else item.labelEn
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (isArabic) item.labelAr else item.labelEn,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = PitchEmerald,
                                        selectedTextColor = PitchEmerald,
                                        indicatorColor = PitchEmeraldContainer,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_VPN_HOME,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 680.dp)
                        ) {
                            composable(ROUTE_VPN_HOME) {
                                VpnHomeScreen(
                                    uiState = uiState,
                                    onToggleVpn = viewModel::onToggleConnection,
                                    onNavigateToLuckBoost = {
                                        navController.navigate(ROUTE_LUCK_BOOST) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onNavigateToServers = {
                                        navController.navigate(ROUTE_SERVERS_SETTINGS) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onLaunchEfootball = {
                                        val launched = viewModel.launchEfootball()
                                        if (!launched) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (isArabic) "لعبة eFootball غير مثبّتة على الجهاز" else "eFootball is not installed on this device"
                                                )
                                            }
                                        }
                                    }
                                )
                            }

                            composable(ROUTE_LUCK_BOOST) {
                                LuckBoostScreen(
                                    uiState = uiState,
                                    onRunBoostCalibration = viewModel::runLuckBoostCalibration,
                                    onToggleRitualStep = viewModel::toggleRitualStep,
                                    onTogglePackTimer = viewModel::togglePackTimer,
                                    onResetPackTimer = viewModel::resetPackTimer,
                                    onLogPackPull = viewModel::logPackPull,
                                    onClearPackHistory = viewModel::clearPackHistory
                                )
                            }

                            composable(ROUTE_SERVERS_SETTINGS) {
                                ServersSettingsScreen(
                                    uiState = uiState,
                                    onSelectNode = viewModel::selectNode,
                                    onProbeAllNodes = { viewModel.probeAllNodes(initialFast = false) },
                                    onToggleAutoConnectGermany = viewModel::setAutoConnectGermany,
                                    onToggleSplitTunnelEfootball = viewModel::setSplitTunnelEfootball,
                                    onToggleLuckStabilityMode = viewModel::setLuckStabilityMode,
                                    onToggleLanguage = viewModel::setIsArabic
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
