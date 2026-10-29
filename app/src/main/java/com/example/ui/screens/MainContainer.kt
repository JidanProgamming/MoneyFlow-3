package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.R
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

                // Tab 1: Wallet (Dompet & Rekening)
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Default.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Wallet"
                        )
                    },
                    label = { Text("Wallet") }
                )

                // Tab 2: Riwayat
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Default.History else Icons.Outlined.History,
                            contentDescription = "Riwayat"
                        )
                    },
                    label = { Text("Riwayat") }
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

                // Tab 4: Settings / Profil
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 4) Icons.Default.Settings else Icons.Outlined.Settings,
                            contentDescription = "Profil"
                        )
                    },
                    label = { Text("Profil") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1 || selectedTab == 2) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 6.dp, end = 4.dp)
                ) {
                    // Tombol Masuk Chatbot dengan Logo MoneyFlow HD (sesuai gambar yang diunggah)
                    Surface(
                        onClick = { navController.navigate("ai_chat") },
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 8.dp,
                        color = Color.Transparent,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("btn_ai_chat_logo")
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_moneyflow_logo),
                            contentDescription = "Buka Chatbot MoneyFlow",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Tombol Tambah Transaksi (+)
                    FloatingActionButton(
                        onClick = { navController.navigate("add_transaction") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("add_transaction_fab"),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Transaksi",
                            modifier = Modifier.size(28.dp)
                        )
                    }
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
                    1 -> WalletScreen(navController, financeViewModel)
                    2 -> HistoryScreen(navController, financeViewModel)
                    3 -> AnalyticsScreen(financeViewModel)
                    4 -> SettingsScreen(navController, financeViewModel, authViewModel)
                }
            }
        }
    }
}
