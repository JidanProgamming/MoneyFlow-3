package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.FinanceViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    financeViewModel: FinanceViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(navController, authViewModel)
        }
        composable("login") {
            LoginScreen(navController, authViewModel)
        }
        composable("register") {
            RegisterScreen(navController, authViewModel)
        }
        composable("main") {
            MainContainer(navController, financeViewModel, authViewModel)
        }
        composable("add_transaction") {
            AddTransactionScreen(navController, financeViewModel)
        }
        composable(
            route = "transaction_detail/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.IntType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getInt("transactionId") ?: -1
            TransactionDetailScreen(transactionId, navController, financeViewModel)
        }
        composable(
            route = "edit_transaction/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.IntType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getInt("transactionId") ?: -1
            EditTransactionScreen(transactionId, navController, financeViewModel)
        }
        composable("wallet") {
            WalletScreen(navController, financeViewModel)
        }
        composable("ai_chat") {
            AiAssistantChatScreen(financeViewModel)
        }
    }
}
