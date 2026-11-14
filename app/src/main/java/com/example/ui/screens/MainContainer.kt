package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val primaryBlue = Color(0xFF0066FF)
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav_container"),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        label = "Beranda",
                        icon = Icons.Default.GridView,
                        isSelected = selectedTab == 0,
                        activeColor = primaryBlue,
                        inactiveColor = inactiveColor,
                        onClick = { selectedTab = 0 },
                        testTag = "nav_item_beranda",
                        modifier = Modifier.weight(1f)
                    )

                    BottomNavItem(
                        label = "Wallet",
                        icon = Icons.Default.AccountBalanceWallet,
                        isSelected = selectedTab == 1,
                        activeColor = primaryBlue,
                        inactiveColor = inactiveColor,
                        onClick = { selectedTab = 1 },
                        testTag = "nav_item_wallet",
                        modifier = Modifier.weight(1f)
                    )

                    BottomNavItem(
                        label = "Riwayat",
                        icon = Icons.Default.History,
                        isSelected = selectedTab == 2,
                        activeColor = primaryBlue,
                        inactiveColor = inactiveColor,
                        onClick = { selectedTab = 2 },
                        testTag = "nav_item_riwayat",
                        modifier = Modifier.weight(1f)
                    )

                    BottomNavItem(
                        label = "Insight",
                        icon = Icons.Default.TrendingUp,
                        isSelected = selectedTab == 3,
                        activeColor = primaryBlue,
                        inactiveColor = inactiveColor,
                        onClick = { selectedTab = 3 },
                        testTag = "nav_item_insight",
                        modifier = Modifier.weight(1f)
                    )

                    BottomNavItem(
                        label = "Profil",
                        icon = Icons.Default.Settings,
                        isSelected = selectedTab == 4,
                        activeColor = primaryBlue,
                        inactiveColor = inactiveColor,
                        onClick = { selectedTab = 4 },
                        testTag = "nav_item_profil",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_transaction") },
                containerColor = primaryBlue,
                contentColor = Color.White,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_transaction_fab"),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Transaksi",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
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

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(activeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = activeColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = inactiveColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor,
            textAlign = TextAlign.Center
        )
    }
}
