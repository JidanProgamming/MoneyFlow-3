package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.FinanceViewModel

@Composable
fun MainContainer(
    navController: NavController,
    financeViewModel: FinanceViewModel,
    authViewModel: AuthViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }

    // Enforce Edge-to-Edge and responsive notch insets
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav"),
                tonalElevation = 8.dp
            ) {
                // Tab 0: Dashboard
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Beranda"
                        )
                    },
                    label = { Text("Beranda") }
                )

                // Tab 1: Riwayat
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Default.History else Icons.Outlined.History,
                            contentDescription = "Riwayat"
                        )
                    },
                    label = { Text("Riwayat") }
                )

                // Tab 2: Tanya AI Assistant
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Default.SmartToy else Icons.Outlined.SmartToy,
                            contentDescription = "Tanya AI"
                        )
                    },
                    label = { Text("Tanya AI") }
                )

                // Tab 3: Insight
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Default.TrendingUp else Icons.Outlined.TrendingUp,
                            contentDescription = "Insight"
                        )
                    },
                    label = { Text("Insight") }
                )

                // Tab 4: Settings
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 4) Icons.Default.Settings else Icons.Outlined.Settings,
                            contentDescription = "Pengaturan"
                        )
                    },
                    label = { Text("Profil") }
                )
            }
        },
        floatingActionButton = {
            // Floating Action Button to add transactions seamlessly
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_transaction") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .testTag("add_transaction_fab")
                        .padding(bottom = 12.dp),
                    shape = FloatingActionButtonDefaults.largeShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Transaksi",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        // Smooth crossfade animation on active tab transition
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "MainTabsAnimation"
            ) { targetTab ->
                when (targetTab) {
                    0 -> DashboardScreen(navController, financeViewModel, authViewModel)
                    1 -> HistoryScreen(navController, financeViewModel)
                    2 -> AiAssistantChatScreen(financeViewModel)
                    3 -> AnalyticsScreen(financeViewModel)
                    4 -> SettingsScreen(navController, financeViewModel, authViewModel)
                }
            }
        }
    }
}
