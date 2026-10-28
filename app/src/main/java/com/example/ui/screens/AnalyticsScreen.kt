package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.theme.SoftGreenSuccess
import com.example.ui.theme.SoftRedDanger
import com.example.utils.FormatUtils
import com.example.viewmodel.FinanceViewModel
import java.util.Calendar

@Composable
fun AnalyticsScreen(financeViewModel: FinanceViewModel) {
    val transactions by financeViewModel.transactions.collectAsState()
    val selectedMonth by financeViewModel.selectedMonth.collectAsState()
    val selectedYear by financeViewModel.selectedYear.collectAsState()
    val targetBudget by financeViewModel.targetBudget.collectAsState()

    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    var showMonthPickerDialog by remember { mutableStateOf(false) }
    var showChangeBudgetDialog by remember { mutableStateOf(false) }
    var tempBudgetString by remember { mutableStateOf("") }

    var selectedPeriodType by remember { mutableStateOf("BULAN") } // "BULAN" or "TAHUN"

    // Calculate analytics metrics filtered by selected month & year or year only
    val (expenseMap, incomeTotal, expenseTotal, maxExpenseCategory) = remember(transactions, selectedMonth, selectedYear, selectedPeriodType) {
        val expenseGroup = mutableMapOf<String, Double>()
        var incomeSum = 0.0
        var expenseSum = 0.0

        val calendar = Calendar.getInstance()
        transactions.forEach { t ->
            calendar.timeInMillis = t.date
            val tMonth = calendar.get(Calendar.MONTH)
            val tYear = calendar.get(Calendar.YEAR)
            
            val isMatch = if (selectedPeriodType == "BULAN") {
                tMonth == selectedMonth && tYear == selectedYear
            } else {
                tYear == selectedYear
            }

            if (isMatch) {
                if (t.type == "INCOME") {
                    // Exclude "Piutang" from actual income calculation because it's not yet paid!
                    if (t.category != "Piutang") {
                        incomeSum += t.amount
                    }
                } else {
                    expenseSum += t.amount
                    expenseGroup[t.category] = (expenseGroup[t.category] ?: 0.0) + t.amount
                }
            }
        }

        val topCategory = expenseGroup.maxByOrNull { it.value }?.key ?: "Belum ada"
        Quadruple(expenseGroup, incomeSum, expenseSum, topCategory)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 100.dp)
    ) {
        Text(
            text = "Insight Keuangan",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Bulanan vs Tahunan Period Type Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedPeriodType == "BULAN",
                onClick = { selectedPeriodType = "BULAN" },
                label = { Text("Bulanan", fontWeight = FontWeight.SemiBold) },
                leadingIcon = if (selectedPeriodType == "BULAN") {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedPeriodType == "TAHUN",
                onClick = { selectedPeriodType = "TAHUN" },
                label = { Text("Tahunan", fontWeight = FontWeight.SemiBold) },
                leadingIcon = if (selectedPeriodType == "TAHUN") {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }

        // Period Selector row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (selectedPeriodType == "BULAN") {
                            var newMonth = selectedMonth - 1
                            var newYear = selectedYear
                            if (newMonth < 0) {
                                newMonth = 11
                                newYear -= 1
                            }
                            financeViewModel.setSelectedMonth(newMonth, newYear)
                        } else {
                            financeViewModel.setSelectedMonth(selectedMonth, selectedYear - 1)
                        }
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Sebelumnya")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { 
                            if (selectedPeriodType == "BULAN") {
                                showMonthPickerDialog = true 
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Kalender", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedPeriodType == "BULAN") "${monthNames[selectedMonth]} $selectedYear" else "Tahun $selectedYear",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (selectedPeriodType == "BULAN") {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                IconButton(
                    onClick = {
                        if (selectedPeriodType == "BULAN") {
                            var newMonth = selectedMonth + 1
                            var newYear = selectedYear
                            if (newMonth > 11) {
                                newMonth = 0
                                newYear += 1
                            }
                            financeViewModel.setSelectedMonth(newMonth, newYear)
                        } else {
                            financeViewModel.setSelectedMonth(selectedMonth, selectedYear + 1)
                        }
                    }
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Berikutnya")
                }
            }
        }

        // Target Anggaran Card
        val activeTargetBudget = if (selectedPeriodType == "BULAN") targetBudget else targetBudget * 12
        val targetLabel = if (selectedPeriodType == "BULAN") "Target Anggaran Bulanan" else "Target Anggaran Tahunan"

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Flag, contentDescription = "Target", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = targetLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    TextButton(
                        onClick = {
                            tempBudgetString = targetBudget.toInt().toString()
                            showChangeBudgetDialog = true
                        }
                    ) {
                        Text("Ubah", fontWeight = FontWeight.Bold)
                    }
                }
                
                Text(
                    text = FormatUtils.formatRupiah(activeTargetBudget),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                val progress = if (activeTargetBudget > 0) (expenseTotal / activeTargetBudget).toFloat().coerceIn(0f, 1f) else 0f
                val progressColor = if (progress >= 0.9f) Color.Red else MaterialTheme.colorScheme.primary
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Terpakai: ${FormatUtils.formatRupiah(expenseTotal)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val remaining = activeTargetBudget - expenseTotal
                    Text(
                        text = if (remaining >= 0) "Sisa: ${FormatUtils.formatRupiah(remaining)}" else "Over: ${FormatUtils.formatRupiah(-remaining)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining >= 0) SoftGreenSuccess else SoftRedDanger
                    )
                }
            }
        }

        // Dana Masuk vs Dana Keluar & Arus Kas Bar Chart
        CashFlowBarChart(income = incomeTotal, expense = expenseTotal)

        // Expense Category Breakdown (Canvas Pie Chart)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Persentase Pengeluaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (expenseMap.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada data pengeluaran untuk dianalisis.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .testTag("pie_chart_canvas")
                        ) {
                            CategoryPieChart(expenseMap = expenseMap, totalExpense = expenseTotal)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            expenseMap.keys.take(5).forEach { category ->
                                val amount = expenseMap[category] ?: 0.0
                                val percent = if (expenseTotal > 0) (amount / expenseTotal * 100).toInt() else 0
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(getCategoryColor(category))
                                    )
                                    Text(
                                        text = "$category ($percent%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Smart AI Insight Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Insight", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rekomendasi Pintar ✨",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val insightText = if (expenseTotal == 0.0) {
                        if (selectedPeriodType == "BULAN") {
                            "Catatan kamu masih bersih bulan ini! Mulailah mencatat pengeluaran untuk mendapatkan analisis keuangan yang mendalam."
                        } else {
                            "Catatan kamu masih bersih tahun ini! Mulailah mencatat pengeluaran untuk mendapatkan analisis keuangan yang mendalam."
                        }
                    } else if (expenseTotal > activeTargetBudget && activeTargetBudget > 0) {
                        if (selectedPeriodType == "BULAN") {
                            "Peringatan: Pengeluaranmu bulan ini telah melebihi target anggaran! Segera batasi pengeluaran non-prioritas untuk menjaga stabilitas keuangan."
                        } else {
                            "Peringatan: Pengeluaranmu tahun ini telah melebihi target anggaran! Segera batasi pengeluaran non-prioritas untuk menjaga stabilitas keuangan."
                        }
                    } else if (expenseTotal > incomeTotal && incomeTotal > 0) {
                        "Peringatan: Pengeluaranmu lebih tinggi dibanding pemasukan! Kurangi pengeluaran hiburan atau kategori non-prioritas segera."
                    } else {
                        val periodLabel = if (selectedPeriodType == "BULAN") "bulan ini" else "tahun ini"
                        val balanceLabel = if (selectedPeriodType == "BULAN") "saldo bulananmu" else "saldo tahunanmu"
                        "Pengeluaran terbesar $periodLabel didominasi oleh kategori **$maxExpenseCategory**. Cobalah menyisihkan dana minimal 15% dari total $balanceLabel ke tabungan di awal."
                    }
                    
                    Text(
                        text = insightText.replace("**", ""),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    // Month Selection dialog
    if (showMonthPickerDialog) {
        AlertDialog(
            onDismissRequest = { showMonthPickerDialog = false },
            title = { Text("Pilih Bulan", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    monthNames.forEachIndexed { idx, name ->
                        val isSelected = selectedMonth == idx
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    financeViewModel.setSelectedMonth(idx, selectedYear)
                                    showMonthPickerDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = name,
                                modifier = Modifier.padding(14.dp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthPickerDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Change target budget dialog
    if (showChangeBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showChangeBudgetDialog = false },
            title = { Text("Ubah Target Anggaran", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempBudgetString,
                    onValueChange = { if (it.all { c -> c.isDigit() }) tempBudgetString = it },
                    label = { Text("Target Baru (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTarget = tempBudgetString.toDoubleOrNull() ?: 0.0
                        financeViewModel.setTargetBudget(newTarget)
                        showChangeBudgetDialog = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeBudgetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun CashFlowBarChart(income: Double, expense: Double) {
    val maxVal = maxOf(income, expense).takeIf { it > 0 } ?: 1.0
    
    val heightDp = 150.dp
    
    val incomeHeightPercent = (income / maxVal).toFloat().coerceIn(0.01f, 1f)
    val expenseHeightPercent = (expense / maxVal).toFloat().coerceIn(0.01f, 1f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Dana Masuk vs Dana Keluar",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Column 1: Pemasukan
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(65.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = formatShortRupiah(income),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(incomeHeightPercent)
                            .width(28.dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(Color(0xFF10B981))
                    )
                }
                
                // Column 2: Pengeluaran
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(65.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = formatShortRupiah(expense),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(expenseHeightPercent)
                            .width(28.dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(Color(0xFFEF4444))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Masuk", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Keluar", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

fun formatShortRupiah(value: Double): String {
    val absVal = Math.abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
        absVal >= 1_000_000 -> {
            val formatted = String.format("%.1f", absVal / 1_000_000)
            "${sign}Rp ${formatted}Jt"
        }
        absVal >= 1_000 -> {
            val formatted = String.format("%.1f", absVal / 1_000)
            "${sign}Rp ${formatted}Rb"
        }
        else -> {
            "${sign}Rp ${absVal.toInt()}"
        }
    }
}

@Composable
fun CategoryPieChart(expenseMap: Map<String, Double>, totalExpense: Double) {
    val categories = expenseMap.keys.toList()
    val total = totalExpense.takeIf { it > 0 } ?: 1.0

    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        
        categories.forEach { cat ->
            val value = expenseMap[cat] ?: 0.0
            val sweepAngle = ((value / total) * 360f).toFloat()
            val color = getCategoryColor(cat)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.width, size.height)
            )
            startAngle += sweepAngle
        }
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Tempat Tinggal" -> Color(0xFF5C6BC0) // Indigo
        "Tagihan Rutin" -> Color(0xFF26C6DA)  // Cyan
        "Pulsa/Data" -> Color(0xFF8D6E63)     // Brown
        "Langganan" -> Color(0xFF78909C)     // Blue Grey
        "Belanja(Shopping)" -> Color(0xFFFF7043) // Coral Orange
        "Kesehatan" -> Color(0xFFEF5350)     // Red
        "Pajak" -> Color(0xFFFFCA28)         // Yellow
        "Cicilan/Hutang", "Cicilan/Hutang(Produktif)" -> Color(0xFFEC407A) // Pink
        "Cicilan/Hutang(Konsumtif)" -> Color(0xFFD81B60) // Magenta/Pink
        "Zakat & Sedekah" -> Color(0xFF26A69A) // Teal
        "Perawatan Diri" -> Color(0xFFAB47BC) // Purple
        "Keluarga" -> Color(0xFF42A5F5)      // Blue
        "Kebutuhan Sekolah" -> Color(0xFF9CCC65) // Light Green
        "Bonus/Insentif" -> Color(0xFF26A69A) // Teal Green
        "Pekerjaan Sampingan(Freelancer)" -> Color(0xFF66BB6A) // Green
        "Penjualan" -> Color(0xFF66BB6A)     // Green
        "Piutang" -> Color(0xFF42A5F5)       // Blue
        "Makanan" -> Color(0xFFFF7043)       // Coral Orange
        "Transportasi" -> Color(0xFF26A69A)  // Teal
        "Hiburan" -> Color(0xFFAB47BC)       // Purple
        "E-Wallet" -> Color(0xFF29B6F6)      // Light Blue
        "Gaji", "Gaji Utama" -> Color(0xFF66BB6A)          // Green
        "Minuman" -> Color(0xFFFFCA28)       // Amber Yellow
        "Jajan" -> Color(0xFFEC407A)         // Pink / Rose
        else -> Color(0xFF90A4AE)            // Grey
    }
}

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
