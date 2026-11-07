package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Transaction
import com.example.ui.components.ReceiptScanDialog
import com.example.utils.ChatMessage
import com.example.utils.FinancialCategorySpend
import com.example.utils.FinancialReportData
import com.example.utils.FormatUtils
import com.example.utils.OpenRouterAiService
import com.example.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantChatScreen(
    financeViewModel: FinanceViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onNavigateToAnalytics: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val baseUrl by financeViewModel.openRouterBaseUrl.collectAsState()
    val apiKey by financeViewModel.openRouterApiKey.collectAsState()
    val model by financeViewModel.openRouterModel.collectAsState()
    val allTransactions by financeViewModel.transactions.collectAsState()

    var userMessageInput by remember { mutableStateOf("") }
    var isLoadingResponse by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showScanReceiptDialog by remember { mutableStateOf(false) }

    // 20 Defined Categories with individual icons, colors, and mapping
    val definedCategories = remember {
        listOf(
            "Makanan" to Triple("food", 0xFFFFA500, Icons.Default.Restaurant),
            "Minuman" to Triple("drink", 0xFF0EA5E9, Icons.Default.LocalCafe),
            "Transportasi" to Triple("transport", 0xFF10B981, Icons.Default.DirectionsCar),
            "Hiburan" to Triple("entertainment", 0xFFA855F7, Icons.Default.SportsEsports),
            "Tempat Tinggal" to Triple("housing", 0xFFF43F5E, Icons.Default.Home),
            "Tagihan Rutin" to Triple("bills", 0xFFF59E0B, Icons.Default.Receipt),
            "Pulsa/Data" to Triple("phone", 0xFF6366F1, Icons.Default.PhoneAndroid),
            "Langganan" to Triple("subscription", 0xFFEC4899, Icons.Default.Star),
            "Belanja" to Triple("shopping", 0xFF06B6D4, Icons.Default.ShoppingCart),
            "Kesehatan" to Triple("health", 0xFFEF4444, Icons.Default.LocalHospital),
            "Pajak" to Triple("tax", 0xFF64748B, Icons.Default.AccountBalance),
            "Cicilan/Hutang Produktif" to Triple("debt_productive", 0xFF14B8A6, Icons.Default.CreditCard),
            "Cicilan/Hutang Konsumtif" to Triple("debt_consumptive", 0xFFFB923C, Icons.Default.Payments),
            "Zakat & Sedekah" to Triple("charity", 0xFF22C55E, Icons.Default.Favorite),
            "Perawatan Diri" to Triple("self_care", 0xFFD946EF, Icons.Default.Face),
            "Keluarga" to Triple("family", 0xFF3B82F6, Icons.Default.People),
            "Kebutuhan Sekolah" to Triple("school", 0xFF8B5CF6, Icons.Default.School),
            "E-Wallet" to Triple("ewallet", 0xFF38BDF8, Icons.Default.AccountBalanceWallet),
            "Jajan" to Triple("snack", 0xFFEAB308, Icons.Default.Fastfood),
            "Lainnya" to Triple("other", 0xFF94A3B8, Icons.Default.Category)
        )
    }

    fun mapCategoryKey(raw: String): String {
        val l = raw.lowercase()
        return when {
            l.contains("makan") || l.contains("food") -> "Makanan"
            l.contains("minum") || l.contains("kopi") || l.contains("cafe") || l.contains("tea") -> "Minuman"
            l.contains("transport") || l.contains("bensin") || l.contains("ojol") || l.contains("grab") || l.contains("gojek") -> "Transportasi"
            l.contains("hiburan") || l.contains("game") || l.contains("nonton") || l.contains("bioskop") -> "Hiburan"
            l.contains("tempat tinggal") || l.contains("kos") || l.contains("sewa") || l.contains("kontrakan") -> "Tempat Tinggal"
            l.contains("tagihan rutin") || l.contains("listrik") || l.contains("pln") || l.contains("air") || l.contains("pdam") -> "Tagihan Rutin"
            l.contains("pulsa") || l.contains("data") || l.contains("kuota") -> "Pulsa/Data"
            l.contains("langganan") || l.contains("subscription") || l.contains("netflix") || l.contains("spotify") -> "Langganan"
            l.contains("belanja") || l.contains("shopping") || l.contains("supermarket") || l.contains("mall") -> "Belanja"
            l.contains("kesehatan") || l.contains("obat") || l.contains("dokter") || l.contains("apotek") || l.contains("klinik") -> "Kesehatan"
            l.contains("pajak") || l.contains("tax") -> "Pajak"
            l.contains("produktif") -> "Cicilan/Hutang Produktif"
            l.contains("konsumtif") -> "Cicilan/Hutang Konsumtif"
            l.contains("zakat") || l.contains("sedekah") || l.contains("infak") || l.contains("donasi") -> "Zakat & Sedekah"
            l.contains("perawatan") || l.contains("salon") || l.contains("skincare") -> "Perawatan Diri"
            l.contains("keluarga") || l.contains("orang tua") || l.contains("anak") -> "Keluarga"
            l.contains("sekolah") || l.contains("kuliah") || l.contains("pendidikan") || l.contains("kursus") -> "Kebutuhan Sekolah"
            l.contains("e-wallet") || l.contains("ewallet") || l.contains("topup") || l.contains("gopay") || l.contains("ovo") || l.contains("dana") -> "E-Wallet"
            l.contains("jajan") || l.contains("snack") || l.contains("cemilan") -> "Jajan"
            else -> "Lainnya"
        }
    }

    // Helper to generate dynamic financial report from actual Room database (100% REAL DATA, NO DUMMY DATA)
    fun buildRealFinancialReport(period: String): FinancialReportData {
        val now = System.currentTimeMillis()
        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calToday.timeInMillis

        data class PeriodBounds(
            val curStart: Long,
            val curEnd: Long,
            val prevStart: Long,
            val prevEnd: Long,
            val title: String,
            val suffix: String
        )

        // Determine current and previous comparison periods
        val bounds = when (period.lowercase()) {
            "mingguan", "laporan mingguan" -> {
                val curStart = now - (7L * 24 * 60 * 60 * 1000)
                val pStart = curStart - (7L * 24 * 60 * 60 * 1000)
                PeriodBounds(curStart, now, pStart, curStart, "Berikut laporan keuangan kamu 7 hari terakhir:", "pekan lalu")
            }
            "bulanan", "laporan bulanan" -> {
                val curStart = now - (30L * 24 * 60 * 60 * 1000)
                val pStart = curStart - (30L * 24 * 60 * 60 * 1000)
                PeriodBounds(curStart, now, pStart, curStart, "Berikut laporan keuangan kamu 30 hari terakhir:", "bulan lalu")
            }
            else -> {
                val calYesterday = (calToday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
                val yStart = calYesterday.timeInMillis
                PeriodBounds(todayStart, now, yStart, todayStart, "Berikut laporan keuangan kamu hari ini:", "kemarin")
            }
        }

        val curStartLong = bounds.curStart
        val curEndLong = bounds.curEnd
        val prevStartLong = bounds.prevStart
        val prevEndLong = bounds.prevEnd
        val titleStr = bounds.title
        val suffixStr = bounds.suffix

        val currentTrans = allTransactions.filter { it.date in curStartLong..curEndLong }
        val prevTrans = allTransactions.filter { it.date in prevStartLong until prevEndLong }

        val totalIncome = currentTrans.filter { it.type == "INCOME" && it.category != "Piutang" }.sumOf { it.amount }
        val totalExpense = currentTrans.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        val prevIncome = prevTrans.filter { it.type == "INCOME" && it.category != "Piutang" }.sumOf { it.amount }
        val prevExpense = prevTrans.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        // Compute actual trend change based on real previous data
        fun formatIncomeTrend(current: Double, prev: Double, suffix: String): String {
            if (prev <= 0.0) {
                return if (current > 0.0) "↑ +100% dari $suffix" else "0% dari $suffix"
            }
            val pct = ((current - prev) / prev) * 100.0
            return if (pct >= 0) {
                "↑ +${String.format(Locale.US, "%.0f", pct)}% dari $suffix"
            } else {
                "↓ -${String.format(Locale.US, "%.0f", Math.abs(pct))}% dari $suffix"
            }
        }

        fun formatExpenseTrend(current: Double, prev: Double, suffix: String): String {
            if (prev <= 0.0) {
                return if (current > 0.0) "↑ +100% dari $suffix" else "0% dari $suffix"
            }
            val pct = ((current - prev) / prev) * 100.0
            return if (pct <= 0) {
                "↓ ${String.format(Locale.US, "%.0f", pct)}% dari $suffix"
            } else {
                "↑ +${String.format(Locale.US, "%.0f", pct)}% dari $suffix"
            }
        }

        val incomeTrendText = formatIncomeTrend(totalIncome, prevIncome, suffixStr)
        val expenseTrendText = formatExpenseTrend(totalExpense, prevExpense, suffixStr)

        // Group actual expenses into the 20 requested categories
        val expenseMap = mutableMapOf<String, Double>()
        currentTrans.filter { it.type == "EXPENSE" }.forEach { t ->
            val mapped = mapCategoryKey(t.category)
            expenseMap[mapped] = (expenseMap[mapped] ?: 0.0) + t.amount
        }

        // Build all 20 category items with actual amounts and exact percentages
        val categoryList = definedCategories.map { (catName, meta) ->
            val amt = expenseMap[catName] ?: 0.0
            val pct = if (totalExpense > 0.0) (amt / totalExpense) * 100.0 else 0.0
            FinancialCategorySpend(
                name = catName,
                amount = amt,
                percentage = pct,
                colorHex = meta.second,
                iconType = meta.first
            )
        }.sortedWith(compareByDescending<FinancialCategorySpend> { it.amount }.thenBy { it.name })

        return FinancialReportData(
            title = titleStr,
            netIncome = totalIncome,
            incomeChangeText = incomeTrendText,
            totalExpense = totalExpense,
            expenseChangeText = expenseTrendText,
            categories = categoryList
        )
    }

    // Helper to build largest expenses text response from actual database
    fun buildLargestExpensesResponse(): String {
        val expenses = allTransactions.filter { it.type == "EXPENSE" }
        if (expenses.isEmpty()) {
            return "Belum ada catatan pengeluaran di database aplikasi Anda. Silakan catat transaksi melalui tombol (+) atau Pindai Struk."
        }

        val totalExpense = expenses.sumOf { it.amount }
        val topCategories = expenses.groupBy { mapCategoryKey(it.category) }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val topTransactions = expenses.sortedByDescending { it.amount }.take(5)

        val catText = topCategories.mapIndexed { idx, (cat, amt) ->
            val pct = if (totalExpense > 0) (amt / totalExpense) * 100 else 0.0
            "${idx + 1}. **$cat**: ${FormatUtils.formatRupiah(amt)} (${String.format(Locale.getDefault(), "%.1f", pct)}%)"
        }.joinToString("\n")

        val transText = topTransactions.joinToString("\n") { t ->
            val dStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(t.date))
            "• **${FormatUtils.formatRupiah(t.amount)}** - ${t.note.ifBlank { t.category }} ($dStr, ${t.walletAccount})"
        }

        return """
            📊 **Rangkuman Pengeluaran Terbesar (Data Aktual Database)**:
            Total Seluruh Pengeluaran: **${FormatUtils.formatRupiah(totalExpense)}**

            🏆 **Top Kategori Pengeluaran Terbesar**:
            $catText

            🧾 **5 Transaksi Pengeluaran Tertinggi**:
            $transText
        """.trimIndent()
    }

    // Helper to build wallet balance text response from actual database
    fun buildWalletBalanceResponse(): String {
        val totalIncome = allTransactions.filter { it.type == "INCOME" && it.category != "Piutang" }.sumOf { it.amount }
        val totalExpense = allTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalBalance = totalIncome - totalExpense

        fun getWalletBalance(name: String, initial: Double): Double {
            val inc = allTransactions.filter { it.walletAccount.equals(name, ignoreCase = true) && it.type == "INCOME" }.sumOf { it.amount }
            val exp = allTransactions.filter { it.walletAccount.equals(name, ignoreCase = true) && it.type == "EXPENSE" }.sumOf { it.amount }
            return initial + inc - exp
        }

        val cash = getWalletBalance("Tunai", financeViewModel.initialCash.value)
        val ewallet = getWalletBalance("E-Wallet", financeViewModel.initialEWallet.value)
        val bca = getWalletBalance("BCA", financeViewModel.initialBca.value)
        val bri = getWalletBalance("BRI", financeViewModel.initialBri.value)
        val danamon = getWalletBalance("Danamon", financeViewModel.initialDanamon.value)
        val other = getWalletBalance("Lainnya", financeViewModel.initialOther.value)

        return """
            💳 **Rincian Sisa Saldo Dompet & Rekening (Data Real-Time Database)**:
            Total Saldo Bersih: **${FormatUtils.formatRupiah(totalBalance)}**

            • 💵 **Tunai**: ${FormatUtils.formatRupiah(cash)}
            • 📱 **E-Wallet**: ${FormatUtils.formatRupiah(ewallet)}
            • 🏦 **Bank BCA**: ${FormatUtils.formatRupiah(bca)}
            • 🏦 **Bank BRI**: ${FormatUtils.formatRupiah(bri)}
            • 🏦 **Bank Danamon**: ${FormatUtils.formatRupiah(danamon)}
            • 💳 **Rekening Lainnya**: ${FormatUtils.formatRupiah(other)}

            💡 *Data di atas dihitung otomatis secara real-time dari transaksi Room Database.*
        """.trimIndent()
    }

    // Initial conversation matching exact Gambar 1
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                role = "assistant",
                content = "Hallo! 👋\nSaya **MoneyFlow**, asisten keuangan pribadi kamu. Ada yang bisa saya bantu hari ini?",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // Pill suggestions requested by user
    val quickActionSuggestions = remember {
        listOf(
            Pair("Laporan Harian", Icons.Default.CalendarToday),
            Pair("Laporan Mingguan", Icons.Default.BarChart),
            Pair("Laporan Bulanan", Icons.Default.CalendarMonth),
            Pair("Pengeluaran Terbesar", Icons.Default.TrendingDown),
            Pair("Sisa Saldo Dompet", Icons.Default.AccountBalanceWallet)
        )
    }

    fun sendUserMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || isLoadingResponse) return

        messages.add(ChatMessage(role = "user", content = trimmed))
        userMessageInput = ""
        focusManager.clearFocus()

        val lower = trimmed.lowercase()

        // 1. Check for Financial Report requests (Harian, Mingguan, Bulanan)
        if (lower.contains("laporan harian") || lower.contains("hari ini")) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
                isLoadingResponse = true
                kotlinx.coroutines.delay(200)
                val report = buildRealFinancialReport("harian")
                messages.add(
                    ChatMessage(
                        role = "assistant",
                        content = report.title,
                        isFinancialReport = true,
                        reportData = report
                    )
                )
                isLoadingResponse = false
                listState.animateScrollToItem(messages.size - 1)
            }
            return
        }

        if (lower.contains("laporan mingguan") || lower.contains("minggu ini")) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
                isLoadingResponse = true
                kotlinx.coroutines.delay(200)
                val report = buildRealFinancialReport("mingguan")
                messages.add(
                    ChatMessage(
                        role = "assistant",
                        content = report.title,
                        isFinancialReport = true,
                        reportData = report
                    )
                )
                isLoadingResponse = false
                listState.animateScrollToItem(messages.size - 1)
            }
            return
        }

        if (lower.contains("laporan bulanan") || lower.contains("bulan ini")) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
                isLoadingResponse = true
                kotlinx.coroutines.delay(200)
                val report = buildRealFinancialReport("bulanan")
                messages.add(
                    ChatMessage(
                        role = "assistant",
                        content = report.title,
                        isFinancialReport = true,
                        reportData = report
                    )
                )
                isLoadingResponse = false
                listState.animateScrollToItem(messages.size - 1)
            }
            return
        }

        // 2. Check for "Pengeluaran Terbesar"
        if (lower.contains("pengeluaran terbesar") || lower.contains("paling boros")) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
                isLoadingResponse = true
                kotlinx.coroutines.delay(200)
                val answer = buildLargestExpensesResponse()
                messages.add(ChatMessage(role = "assistant", content = answer))
                isLoadingResponse = false
                listState.animateScrollToItem(messages.size - 1)
            }
            return
        }

        // 3. Check for "Sisa Saldo Dompet"
        if (lower.contains("sisa saldo dompet") || lower.contains("saldo dompet") || lower.contains("sisa saldo")) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
                isLoadingResponse = true
                kotlinx.coroutines.delay(200)
                val answer = buildWalletBalanceResponse()
                messages.add(ChatMessage(role = "assistant", content = answer))
                isLoadingResponse = false
                listState.animateScrollToItem(messages.size - 1)
            }
            return
        }

        // 4. General reports query
        if (lower.contains("laporan") || lower.contains("keuangan") || lower.contains("rekap")) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
                isLoadingResponse = true
                kotlinx.coroutines.delay(200)
                val report = buildRealFinancialReport("harian")
                messages.add(
                    ChatMessage(
                        role = "assistant",
                        content = report.title,
                        isFinancialReport = true,
                        reportData = report
                    )
                )
                isLoadingResponse = false
                listState.animateScrollToItem(messages.size - 1)
            }
            return
        }

        // 5. Connect to OpenRouter AI or the local smart assistant
        isLoadingResponse = true
        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)

            val databaseContext = financeViewModel.buildDatabaseContextForAi()
            val answer = OpenRouterAiService.chatWithAssistant(
                userQuery = trimmed,
                contextData = databaseContext,
                history = messages.filter { !it.isFinancialReport }.toList(),
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model
            )

            messages.add(ChatMessage(role = "assistant", content = answer))
            isLoadingResponse = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Receipt scanner dialog trigger from top menu
    if (showScanReceiptDialog) {
        ReceiptScanDialog(
            isOpen = true,
            onDismiss = { showScanReceiptDialog = false },
            financeViewModel = financeViewModel
        )
    }

    // Full screen layout with rich vibrant blue header matching the design mockup
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B63F6), // Vibrant Electric Blue
                        Color(0xFF0052CC)  // Deep Blue
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar matching the mockup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = { onBack?.invoke() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // MoneyFlow Wallet Logo Badge
                Image(
                    painter = painterResource(id = R.drawable.ic_moneyflow_logo),
                    contentDescription = "Logo MoneyFlow",
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(13.dp))
                        .clip(RoundedCornerShape(13.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MoneyFlow",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        text = "AI Finance Assistant",
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Three-dot Overflow Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi Lainnya",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Laporan Keuangan Hari Ini") },
                            onClick = {
                                showMenu = false
                                sendUserMessage("Laporan Harian")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Assessment, contentDescription = null, tint = Color(0xFF0B63F6))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Pindai Struk (OCR AI)") },
                            onClick = {
                                showMenu = false
                                showScanReceiptDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF0B63F6))
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Bersihkan Obrolan") },
                            onClick = {
                                showMenu = false
                                messages.clear()
                                messages.add(
                                    ChatMessage(
                                        role = "assistant",
                                        content = "Hallo! 👋\nSaya **MoneyFlow**, asisten keuangan pribadi kamu. Ada yang bisa saya bantu hari ini?"
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            }
                        )
                    }
                }
            }

            // Main Chat Area with Smooth Rounded Top Corners matching mockup
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFFF8FAFD)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat messages list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 18.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(messages) { msg ->
                            ModernChatBubble(
                                message = msg,
                                onReportBannerClick = {
                                    onNavigateToAnalytics?.invoke() ?: onBack?.invoke()
                                }
                            )
                        }

                        if (isLoadingResponse) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                ) {
                                    BotAvatarBadge()
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF0B63F6)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "MoneyFlow sedang menganalisis data...",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick Suggested Chips Pill Row (Placed directly above input bar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickActionSuggestions.forEach { (label, icon) ->
                            Surface(
                                onClick = { sendUserMessage(label) },
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                border = BorderStroke(1.2.dp, Color(0xFF007AFF)),
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF007AFF)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Input Area (Paperclip removed completely as requested!)
                    Surface(
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rounded Input Text Pill
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp, max = 100.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (userMessageInput.isEmpty()) {
                                        Text(
                                            text = "Ketik pesan...",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp
                                        )
                                    }
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = userMessageInput,
                                        onValueChange = { userMessageInput = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("chat_input_text_field"),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            color = Color(0xFF1E293B),
                                            fontSize = 14.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Blue Circular Send Button
                            IconButton(
                                onClick = { sendUserMessage(userMessageInput) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (userMessageInput.isNotBlank()) Color(0xFF007AFF) else Color(0xFF007AFF).copy(alpha = 0.85f)
                                    )
                                    .testTag("chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Kirim",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Bot Avatar Badge with Blue Border and Wallet Vector Icon matching the mockup
 */
@Composable
fun BotAvatarBadge(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_moneyflow_logo),
        contentDescription = "Asisten MoneyFlow",
        modifier = modifier
            .size(36.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp)),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}

/**
 * Chat Bubble Component supporting regular text bubbles, greeting markdown, and rich financial report cards
 */
@Composable
fun ModernChatBubble(
    message: ChatMessage,
    onReportBannerClick: () -> Unit
) {
    val isUser = message.role == "user"
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Bot Avatar on Left
        if (!isUser) {
            BotAvatarBadge()
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = if (message.isFinancialReport) 360.dp else 290.dp)
        ) {
            if (isUser) {
                // User Message: Vibrant Blue Pill Bubble
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 4.dp
                    ),
                    color = Color(0xFF007AFF),
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = message.content,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Time + Double Checkmarks
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "✓✓",
                        fontSize = 11.sp,
                        color = Color(0xFF007AFF),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (message.isFinancialReport && message.reportData != null) {
                // Bot Message: Rich Interactive Financial Report Card
                FinancialReportCardView(
                    reportData = message.reportData,
                    onBannerClick = onReportBannerClick
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeFormatted,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(start = 6.dp)
                )
            } else {
                // Bot Message: Clean Light Grey/Blue Bubble with formatted markdown bolding
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    ),
                    color = Color(0xFFF1F5F9),
                    shadowElevation = 0.5.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            text = parseFormattedMarkdown(message.content),
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B),
                            lineHeight = 21.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeFormatted,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

/**
 * Rich Financial Report Card Component matching the user's requirements:
 * - Ringkasan Keuangan: Total Pemasukan Bersih & Total Pengeluaran dengan ikon lingkaran berwarna, nominal tebal, dan tren perubahan aktual.
 * - Pengeluaran per Kategori: Kartu visual untuk 20 kategori dengan ikon, total nominal, dan persentase pengeluaran.
 * - Banner Navigasi: "Lihat grafik lengkap di Analitik" terhubung ke halaman Analitik.
 */
@Composable
fun FinancialReportCardView(
    reportData: FinancialReportData,
    onBannerClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Title
            Text(
                text = reportData.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Two Summary Cards Side by Side (Pemasukan & Pengeluaran)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Total Pemasukan Bersih
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF06B6D4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Total Pemasukan Bersih",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = FormatUtils.formatRupiah(reportData.netIncome),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066FF)
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = reportData.incomeChangeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Card 2: Total Pengeluaran
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Total Pengeluaran",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = FormatUtils.formatRupiah(reportData.totalExpense),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066FF)
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = reportData.expenseChangeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (reportData.expenseChangeText.startsWith("↓")) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle: Pengeluaran per Kategori (Menampilkan 20 Kategori)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pengeluaran per Kategori (20 Kategori)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Geser →",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visual Cards for 20 Categories (Scrollable Horizontal Row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reportData.categories.forEach { cat ->
                    Surface(
                        modifier = Modifier.width(76.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (cat.amount > 0) Color(0xFFF8FAFC) else Color(0xFFF1F5F9).copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (cat.amount > 0) Color(0xFFCBD5E1) else Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Circular Category Icon
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(cat.colorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryVectorIcon(cat.iconType),
                                    contentDescription = cat.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = cat.name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = FormatUtils.formatRupiah(cat.amount),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (cat.amount > 0) Color(0xFF0066FF) else Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Percentage Pill Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (cat.amount > 0) Color(0xFFEFF6FF) else Color(0xFFE2E8F0).copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.1f", cat.percentage)}%",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cat.amount > 0) Color(0xFF0066FF) else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Banner Link: "Lihat grafik lengkap di Analitik"
            Surface(
                onClick = onBannerClick,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0F7FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_analytics_navigation")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color(0xFF0066FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Lihat grafik lengkap di Analitik",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0066FF),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF0066FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Returns appropriate ImageVector for all 20 category types
 */
fun getCategoryVectorIcon(iconType: String): ImageVector {
    return when (iconType) {
        "food" -> Icons.Default.Restaurant
        "drink" -> Icons.Default.LocalCafe
        "transport" -> Icons.Default.DirectionsCar
        "entertainment" -> Icons.Default.SportsEsports
        "housing" -> Icons.Default.Home
        "bills" -> Icons.Default.Receipt
        "phone" -> Icons.Default.PhoneAndroid
        "subscription" -> Icons.Default.Star
        "shopping" -> Icons.Default.ShoppingCart
        "health" -> Icons.Default.LocalHospital
        "tax" -> Icons.Default.AccountBalance
        "debt_productive" -> Icons.Default.CreditCard
        "debt_consumptive" -> Icons.Default.Payments
        "charity" -> Icons.Default.Favorite
        "self_care" -> Icons.Default.Face
        "family" -> Icons.Default.People
        "school" -> Icons.Default.School
        "ewallet" -> Icons.Default.AccountBalanceWallet
        "snack" -> Icons.Default.Fastfood
        else -> Icons.Default.Category
    }
}

/**
 * Formats Markdown bold **text** in messages nicely for Compose Text
 */
fun parseFormattedMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}
