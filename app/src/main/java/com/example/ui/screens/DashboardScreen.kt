package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.model.Transaction
import com.example.ui.theme.SoftGreenSuccess
import com.example.ui.theme.SoftRedDanger
import com.example.utils.FormatUtils
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.FinanceViewModel
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.components.ReceiptScanDialog
import com.example.ui.components.VoiceInputDialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    navController: NavController,
    financeViewModel: FinanceViewModel,
    authViewModel: AuthViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val transactions by financeViewModel.transactions.collectAsState()

    var showScanReceiptDialog by remember { mutableStateOf(false) }
    var showVoiceInputDialog by remember { mutableStateOf(false) }

    val initialCash by financeViewModel.initialCash.collectAsState()
    val initialEWallet by financeViewModel.initialEWallet.collectAsState()
    val initialBca by financeViewModel.initialBca.collectAsState()
    val initialBri by financeViewModel.initialBri.collectAsState()
    val initialDanamon by financeViewModel.initialDanamon.collectAsState()
    val initialOther by financeViewModel.initialOther.collectAsState()

    // Dynamically calculate final balances for each wallet based on transactions!
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

    // Calculating Stats based on real-time Room values including 50/30/20 actual spend categories
    val (totalBalance, totalIncome, totalExpense, needsActual, wantsActual) = remember(transactions, walletBalances) {
        var income = 0.0
        var expense = 0.0
        var needs = 0.0
        var wants = 0.0
        
        // Find current month start timestamp
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis

        transactions.forEach { trans ->
            if (trans.type == "INCOME") {
                // Ignore Piutang because it is unpaid
                if (trans.category != "Piutang") {
                    if (trans.date >= monthStart) {
                        income += trans.amount
                    }
                }
            } else {
                if (trans.date >= monthStart) {
                    expense += trans.amount
                    // Map categories: Needs -> Makanan, Minuman, Kesehatan, Kebutuhan Sekolah, Perawatan Diri, Transportasi, Pajak, Cicilan/Hutang(Produktif), Zakat & Sedekah, Pulsa/Data, Keluarga, Tagihan Rutin, Tempat Tinggal.
                    val needsCategories = listOf(
                        "Makanan", "Minuman", "Kesehatan", "Kebutuhan Sekolah", "Perawatan Diri", 
                        "Transportasi", "Pajak", "Cicilan/Hutang", "Cicilan/Hutang(Produktif)", 
                        "Zakat & Sedekah", "Pulsa/Data", "Keluarga", "Tagihan Rutin", "Tempat Tinggal"
                    )
                    if (trans.category in needsCategories) {
                        needs += trans.amount
                    } else {
                        // Wants -> Belanja(Shopping), Hiburan, Jajan, E-Wallet, Cicilan/Hutang(Konsumtif), Lainnya, Langganan
                        wants += trans.amount
                    }
                }
            }
        }
        val totalCashBalance = walletBalances.values.sum()
        val calculatedBalance = totalCashBalance
        listOf(calculatedBalance, income, expense, needs, wants)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Welcome Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Halo, 👋",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = currentUser?.fullName ?: "Pengguna",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Total Balance Hero Card (Dynamic Gradient)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("balance_hero_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Saldo Dompet & Rekening",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = FormatUtils.formatRupiah(totalBalance),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet_custom),
                            contentDescription = "Dompet",
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(52.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Monthly Income Section
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Pemasukan",
                                    tint = SoftGreenSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Pemasukan Bulan Ini",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(totalIncome),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Monthly Expense Section
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Pengeluaran",
                                    tint = SoftRedDanger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Pengeluaran Bulan Ini",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(totalExpense),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick AI Actions: Scan Struk, Voice Over, & Chat Asisten AI
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("ai_quick_actions_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fitur Cerdas AI & OCR",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "OpenRouter AI",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Action 1: Scan Struk
                        OutlinedButton(
                            onClick = { showScanReceiptDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("btn_scan_receipt_dashboard"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Pindai Struk", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Action 2: Voice Over
                        OutlinedButton(
                            onClick = { showVoiceInputDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("btn_voice_input_dashboard"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Voice Over", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Action 3: Tanya AI Assistant
                        Button(
                            onClick = { navController.navigate("ai_chat") },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("btn_ai_chat_dashboard"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Tanya AI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Aturan Pengelolaan Keuangan 50/30/20 (Tata Letak Kompak & Rapi)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .testTag("card_50_30_20_method"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = "Metode 50/30/20",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Aturan Keuangan 50/30/20",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Alokasi target anggaran bulanan Anda",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (totalIncome <= 0.0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Saran",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = "Belum Ada Pemasukan Bulan Ini",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Catat pemasukan untuk mengaktifkan target 50% Kebutuhan, 30% Keinginan, dan 20% Tabungan.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Button(
                                onClick = { navController.navigate("add_transaction") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Catat Pemasukan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        val limitNeeds = totalIncome * 0.50
                        val limitWants = totalIncome * 0.30
                        val limitSavings = totalIncome * 0.20

                        val progressNeeds = if (limitNeeds > 0) (needsActual / limitNeeds).toFloat().coerceIn(0f, 1f) else 0f
                        val progressWants = if (limitWants > 0) (wantsActual / limitWants).toFloat().coerceIn(0f, 1f) else 0f

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 1. Kebutuhan (50%)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("50", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Kebutuhan",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "${FormatUtils.formatRupiah(needsActual)} / ${FormatUtils.formatRupiah(limitNeeds)} (${(progressNeeds * 100).toInt()}%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (needsActual > limitNeeds) SoftRedDanger else MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progressNeeds },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (needsActual > limitNeeds) SoftRedDanger else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }

                            // 2. Keinginan (30%)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("30", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD48A00))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Keinginan",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "${FormatUtils.formatRupiah(wantsActual)} / ${FormatUtils.formatRupiah(limitWants)} (${(progressWants * 100).toInt()}%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (wantsActual > limitWants) SoftRedDanger else Color(0xFFD48A00)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progressWants },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (wantsActual > limitWants) SoftRedDanger else Color(0xFFFFB300),
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }

                            // 3. Tabungan & Investasi (20%)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(SoftGreenSuccess.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("20", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SoftGreenSuccess)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Tabungan & Investasi",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "Target: ${FormatUtils.formatRupiah(limitSavings)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftGreenSuccess
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = SoftGreenSuccess,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Weekly Trend Section
        item {
            Text(
                text = "Tren Keuangan Pekan Ini",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    WeeklySummaryChart(transactions = transactions)
                }
            }
        }

        // Recent Transactions Label
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terbaru",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Recent Transactions List
        val recentTransactions = transactions.take(5)
        if (recentTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada transaksi dimasukkan",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(recentTransactions) { trans ->
                TransactionRowItem(
                    transaction = trans,
                    onClick = {
                        navController.navigate("transaction_detail/${trans.id}")
                    }
                )
            }
        }
    }

    ReceiptScanDialog(
        isOpen = showScanReceiptDialog,
        onDismiss = { showScanReceiptDialog = false },
        financeViewModel = financeViewModel
    )

    VoiceInputDialog(
        isOpen = showVoiceInputDialog,
        onDismiss = { showVoiceInputDialog = false },
        financeViewModel = financeViewModel
    )
}

@Composable
fun TransactionRowItem(transaction: Transaction, onClick: () -> Unit) {
    val categoryIcon = getCategoryIcon(transaction.category)
    val colorAccent = if (transaction.type == "INCOME") SoftGreenSuccess else SoftRedDanger
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorAccent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = transaction.category,
                    tint = colorAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note.ifBlank { transaction.category },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.date)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Text(
                text = (if (transaction.type == "INCOME") "+" else "-") + FormatUtils.formatRupiah(transaction.amount),
                color = colorAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Tempat Tinggal" -> Icons.Default.Home
        "Tagihan Rutin" -> Icons.Default.Receipt
        "Pulsa/Data" -> Icons.Default.PhoneAndroid
        "Langganan" -> Icons.Default.Star
        "Belanja(Shopping)" -> Icons.Default.ShoppingCart
        "Kesehatan" -> Icons.Default.LocalHospital
        "Pajak" -> Icons.Default.AccountBalance
        "Cicilan/Hutang", "Cicilan/Hutang(Produktif)", "Cicilan/Hutang(Konsumtif)" -> Icons.Default.CreditCard
        "Zakat & Sedekah" -> Icons.Default.Favorite
        "Perawatan Diri" -> Icons.Default.Face
        "Keluarga" -> Icons.Default.People
        "Kebutuhan Sekolah" -> Icons.Default.School
        "Bonus/Insentif" -> Icons.Default.CardGiftcard
        "Pekerjaan Sampingan(Freelancer)" -> Icons.Default.Work
        "Penjualan" -> Icons.Default.Storefront
        "Makanan" -> Icons.Default.Restaurant
        "Transportasi" -> Icons.Default.DirectionsCar
        "Hiburan" -> Icons.Default.VideogameAsset
        "E-Wallet" -> Icons.Default.AccountBalanceWallet
        "Gaji", "Gaji Utama" -> Icons.Default.MonetizationOn
        "Minuman" -> Icons.Default.LocalCafe
        "Jajan" -> Icons.Default.Fastfood
        else -> Icons.Default.Category
    }
}

@Composable
fun WeeklySummaryChart(transactions: List<Transaction>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    
    // Group transactions of last 7 days and build chart data representation
    val chartData = remember(transactions) {
        val days = Array(7) { "" }
        val values = FloatArray(7) { 0f }
        
        // Let's populate days of week (backwards)
        val sdf = SimpleDateFormat("EEE", Locale("in", "ID"))
        for (i in 6 downTo 0) {
            val dateCal = Calendar.getInstance()
            dateCal.add(Calendar.DAY_OF_YEAR, -i)
            days[6 - i] = sdf.format(dateCal.time)
            
            // Calculate sum for that day of absolute amount increments
            dateCal.set(Calendar.HOUR_OF_DAY, 0)
            dateCal.set(Calendar.MINUTE, 0)
            dateCal.set(Calendar.SECOND, 0)
            dateCal.set(Calendar.MILLISECOND, 0)
            val startOfDay = dateCal.timeInMillis
            
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000
            
            val daySum = transactions.filter {
                it.date in startOfDay until endOfDay
            }.sumOf { if (it.type == "EXPENSE") it.amount else 0.0 }
            
            values[6 - i] = daySum.toFloat()
        }
        Pair(days, values)
    }

    val daysLabel = chartData.first
    val barValues = chartData.second
    val maxExpense = barValues.maxOrNull()?.takeIf { it > 0 } ?: 100000f

    Column {
        Text(
            text = "Total Pengeluaran Harian (Seminggu)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val totalBars = 7
            val spaceBetween = size.width / totalBars
            val barWidth = 32.dp.toPx()
            val canvasHeight = size.height - 20.dp.toPx()

            for (i in 0 until totalBars) {
                val valueRatio = barValues[i] / maxExpense
                val barHeight = canvasHeight * valueRatio
                val xOffset = i * spaceBetween + (spaceBetween - barWidth) / 2
                val yOffset = canvasHeight - barHeight

                // Draw background bar
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.05f),
                    topLeft = Offset(xOffset, 0f),
                    size = Size(barWidth, canvasHeight),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                if (barHeight > 0) {
                    // Draw visual bar filled
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        ),
                        topLeft = Offset(xOffset, yOffset),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }

                // Draw days of week text labels
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    drawText(
                        daysLabel[i],
                        xOffset + barWidth / 2,
                        size.height,
                        paint
                    )
                }
            }
        }
    }
}
