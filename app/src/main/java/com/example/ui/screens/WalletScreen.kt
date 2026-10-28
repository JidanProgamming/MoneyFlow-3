package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.theme.SoftRedDanger
import com.example.utils.FormatUtils
import com.example.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavController,
    financeViewModel: FinanceViewModel
) {
    val transactions by financeViewModel.transactions.collectAsState()

    val initialCash by financeViewModel.initialCash.collectAsState()
    val initialEWallet by financeViewModel.initialEWallet.collectAsState()
    val initialBca by financeViewModel.initialBca.collectAsState()
    val initialBri by financeViewModel.initialBri.collectAsState()
    val initialDanamon by financeViewModel.initialDanamon.collectAsState()
    val initialOther by financeViewModel.initialOther.collectAsState()

    // Dynamically calculate balances
    val walletBalances = remember(transactions, initialCash, initialEWallet, initialBca, initialBri, initialDanamon, initialOther) {
        var cash = initialCash
        var ewallet = initialEWallet
        var bca = initialBca
        var bri = initialBri
        var danamon = initialDanamon
        var other = initialOther

        transactions.forEach { t ->
            val amt = t.amount
            if (t.walletAccount != "Piutang" && t.category != "Piutang") {
                if (t.type == "INCOME") {
                    when (t.walletAccount) {
                        "Tunai" -> cash += amt
                        "E-Wallet" -> ewallet += amt
                        "BCA", "Rekening 1" -> bca += amt
                        "BRI", "Rekening 2" -> bri += amt
                        "Danamon", "Rekening 3" -> danamon += amt
                        else -> other += amt
                    }
                } else {
                    when (t.walletAccount) {
                        "Tunai" -> cash -= amt
                        "E-Wallet" -> ewallet -= amt
                        "BCA", "Rekening 1" -> bca -= amt
                        "BRI", "Rekening 2" -> bri -= amt
                        "Danamon", "Rekening 3" -> danamon -= amt
                        else -> other -= amt
                    }
                }
            }
        }
        mapOf(
            "Tunai" to cash,
            "E-Wallet" to ewallet,
            "Rekening 1" to bca,
            "Rekening 2" to bri,
            "Rekening 3" to danamon,
            "Lainnya" to other
        )
    }

    val totalCashBalance = walletBalances.values.sum()
    val totalPortfolio = totalCashBalance

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet & Rekening", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Net Balance / Portfolio Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Total Saldo",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatRupiah(totalPortfolio),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // Wallet accounts list
            Text(
                text = "Daftar Akun & Dompet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            val accounts = listOf(
                Triple("Tunai", walletBalances["Tunai"] ?: 0.0, Icons.Default.Payments),
                Triple("E-Wallet", walletBalances["E-Wallet"] ?: 0.0, Icons.Default.Wallet),
                Triple("Rekening 1", walletBalances["Rekening 1"] ?: 0.0, Icons.Default.AccountBalance),
                Triple("Rekening 2", walletBalances["Rekening 2"] ?: 0.0, Icons.Default.AccountBalance),
                Triple("Rekening 3", walletBalances["Rekening 3"] ?: 0.0, Icons.Default.AccountBalance),
                Triple("Lainnya", walletBalances["Lainnya"] ?: 0.0, Icons.Default.AccountBalance)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                accounts.forEach { (name, bal, icon) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                              ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column {
                                    Text(
                                        text = name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val subText = when(name) {
                                        "Rekening 1" -> "Bank Utama (BCA)"
                                        "Rekening 2" -> "Bank Kedua (BRI)"
                                        "Rekening 3" -> "Bank Ketiga (Danamon)"
                                        else -> "Dompet Aktif"
                                    }
                                    Text(
                                        text = subText,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = FormatUtils.formatRupiah(bal),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (bal >= 0) MaterialTheme.colorScheme.onSurface else SoftRedDanger
                            )
                        }
                    }
                }
            }
        }
    }
}
