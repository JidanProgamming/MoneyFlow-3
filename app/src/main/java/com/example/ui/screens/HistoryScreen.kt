package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.data.model.Transaction
import com.example.ui.theme.SoftGreenSuccess
import com.example.ui.theme.SoftRedDanger
import com.example.utils.ExportUtils
import com.example.utils.FormatUtils
import com.example.viewmodel.FinanceViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    navController: NavController,
    financeViewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by financeViewModel.transactions.collectAsState()
    val searchQuery by financeViewModel.searchQuery.collectAsState()
    val selectedFilter by financeViewModel.selectedFilter.collectAsState()

    var selectedHistoryTab by remember { mutableStateOf(0) } // 0 = Semua, 1 = Pemasukan, 2 = Pengeluaran
    var showConfirmDeleteAll by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    // Helper to format date headers for grouped transactions
    fun getDateHeader(timestamp: Long): String {
        val calItem = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calNow = Calendar.getInstance()

        val isToday = calItem.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calItem.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

        val calYesterday = (calNow.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = calItem.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
                calItem.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)

        return when {
            isToday -> "Hari Ini (" + SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID")).format(Date(timestamp)) + ")"
            isYesterday -> "Kemarin (" + SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID")).format(Date(timestamp)) + ")"
            else -> SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID")).format(Date(timestamp))
        }
    }

    // Interactive Filter logic supporting Harian, Mingguan, Bulanan, and Semua
    val filteredTransactions = remember(transactions, searchQuery, selectedFilter, selectedHistoryTab) {
        val now = System.currentTimeMillis()

        // Exact date boundaries for Harian, Mingguan, Bulanan
        val (filterStart, filterEnd) = when (selectedFilter) {
            "Harian" -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                val end = start + (24L * 60 * 60 * 1000) - 1
                Pair(start, end)
            }
            "Mingguan" -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            "Bulanan" -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            else -> Pair(0L, Long.MAX_VALUE) // "Semua"
        }

        val baseList = transactions.filter { item ->
            val matchesTime = item.date in filterStart..filterEnd
            val matchesSearch = item.note.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)

            matchesTime && matchesSearch
        }

        when (selectedHistoryTab) {
            1 -> baseList.filter { it.type == "INCOME" }
            2 -> baseList.filter { it.type == "EXPENSE" }
            else -> baseList
        }
    }

    // Grouping transactions by date header
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.sortedByDescending { it.date }
            .groupBy { getDateHeader(it.date) }
    }

    // Calculated summary stats for the current filtered view
    val (summaryIncome, summaryExpense) = remember(filteredTransactions) {
        var inc = 0.0
        var exp = 0.0
        filteredTransactions.forEach {
            if (it.type == "INCOME") inc += it.amount else exp += it.amount
        }
        Pair(inc, exp)
    }

    // Confirmation dialog for deleting all transactions
    if (showConfirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteAll = false },
            title = { Text("Hapus Semua Transaksi?", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus semua data riwayat transaksi? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        financeViewModel.deleteAllTransactions()
                        showConfirmDeleteAll = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Hapus Semua", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteAll = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Daftar Riwayat",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${filteredTransactions.size} transaksi ($selectedFilter)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Export Menu Icon
                Box {
                    IconButton(
                        onClick = { showExportMenu = true },
                        modifier = Modifier.testTag("export_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Ekspor Riwayat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ekspor Excel (.xlsx)") },
                            leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) },
                            onClick = {
                                showExportMenu = false
                                val file = com.example.aplikasikeuangan.ExcelExporter.exportDatabaseTransactions(context, filteredTransactions)
                                if (file != null) {
                                    shareExportedFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ekspor CSV (.csv)") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                            onClick = {
                                showExportMenu = false
                                val file = ExportUtils.exportToCsv(context, filteredTransactions)
                                if (file != null) {
                                    shareExportedFile(context, file, "text/csv")
                                }
                            }
                        )
                    }
                }

                // Delete All Button
                IconButton(
                    onClick = { showConfirmDeleteAll = true },
                    modifier = Modifier.testTag("delete_all_transactions_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Semua Transaksi",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { financeViewModel.setSearchQuery(it) },
            placeholder = { Text("Cari catatan atau kategori...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search Icon",
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { financeViewModel.setSearchQuery("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Hapus Pencarian",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        // Type Filter Tabs (Semua, Pemasukan, Pengeluaran)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("Semua", "Pemasukan", "Pengeluaran")
                tabs.forEachIndexed { index, label ->
                    val isTabSelected = selectedHistoryTab == index
                    val tabBg = if (isTabSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    val tabTextCol = if (isTabSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                    Button(
                        onClick = { selectedHistoryTab = index },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tabBg,
                            contentColor = tabTextCol
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 1. Time Period Filter Chips: Harian, Mingguan, Bulanan, Semua
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val periodChips = listOf(
                "Semua" to "Semua Waktu",
                "Harian" to "Harian",
                "Mingguan" to "Mingguan",
                "Bulanan" to "Bulanan"
            )
            periodChips.forEach { (key, label) ->
                val isSelected = selectedFilter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { financeViewModel.setSelectedFilter(key) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        val icon = when (key) {
                            "Harian" -> Icons.Default.Today
                            "Mingguan" -> Icons.Default.DateRange
                            "Bulanan" -> Icons.Default.CalendarMonth
                            else -> Icons.Default.AllInclusive
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Summary Statistics Card for current filter
        if (filteredTransactions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (selectedFilter) {
                                "Harian" -> "📅 Periode: Hari Ini"
                                "Mingguan" -> "📅 Periode: 7 Hari Terakhir"
                                "Bulanan" -> "📅 Periode: Bulan Ini"
                                else -> "📅 Periode: Semua Waktu"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${filteredTransactions.size} Transaksi",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Masuk", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = FormatUtils.formatRupiah(summaryIncome),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftGreenSuccess,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column {
                            Text("Total Keluar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = FormatUtils.formatRupiah(summaryExpense),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftRedDanger,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val net = summaryIncome - summaryExpense
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Selisih", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = (if (net >= 0) "+" else "") + FormatUtils.formatRupiah(net),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (net >= 0) SoftGreenSuccess else SoftRedDanger,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // List Display with grouped date headers
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Tidak ada transaksi ditemukan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Tidak ada transaksi untuk filter periode $selectedFilter",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                groupedTransactions.forEach { (dateHeader, itemsInDate) ->
                    item(key = "header_$dateHeader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }

                    items(itemsInDate, key = { it.id }) { item ->
                        HistoryItemCard(
                            transaction = item,
                            onClick = {
                                navController.navigate("transaction_detail/${item.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(transaction: Transaction, onClick: () -> Unit) {
    val categoryIcon = getCategoryIcon(transaction.category)
    val colorAccent = if (transaction.type == "INCOME") SoftGreenSuccess else SoftRedDanger
    val displayTitle = transaction.note.ifBlank { transaction.category }

    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = transaction.category,
                    tint = colorAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colorAccent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = transaction.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Wallet badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = transaction.walletAccount,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Date & Time
                    Text(
                        text = SimpleDateFormat("dd MMM, HH:mm", Locale("in", "ID")).format(Date(transaction.date)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = (if (transaction.type == "INCOME") "+" else "-") + FormatUtils.formatRupiah(transaction.amount),
                color = colorAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

private fun shareExportedFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Laporan Keuangan"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
