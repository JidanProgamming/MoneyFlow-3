package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.components.ReceiptScanDialog
import com.example.ui.components.VoiceInputDialog
import com.example.ui.theme.SoftGreenSuccess
import com.example.ui.theme.SoftRedDanger
import com.example.utils.ExportUtils
import com.example.utils.FormatUtils
import com.example.viewmodel.FinanceViewModel
import java.io.File
import java.util.Calendar

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
    var showScanDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    // Interactive Filter logic
    val filteredTransactions = remember(transactions, searchQuery, selectedFilter, selectedHistoryTab) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // Start date calculations depending on chips
        val filterTimeBoundary: Long = when (selectedFilter) {
            "Harian" -> {
                calendar.apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            "Mingguan" -> {
                calendar.apply {
                    timeInMillis = now
                    add(Calendar.DAY_OF_YEAR, -7)
                }.timeInMillis
            }
            "Bulanan" -> {
                calendar.apply {
                    timeInMillis = now
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            else -> 0L // "Semua"
        }

        val baseList = transactions.filter { item ->
            val matchesFilter = item.date >= filterTimeBoundary
            val matchesSearch = item.note.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }

        when (selectedHistoryTab) {
            1 -> baseList.filter { it.type == "INCOME" }
            2 -> baseList.filter { it.type == "EXPENSE" }
            else -> baseList
        }
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
                    text = "${filteredTransactions.size} transaksi tercatat",
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

                // Camera / Scan Receipt Button
                IconButton(
                    onClick = { showScanDialog = true },
                    modifier = Modifier.testTag("scan_receipt_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Pindai Struk / Bukti Bayar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Voice Over Button
                IconButton(
                    onClick = { showVoiceDialog = true },
                    modifier = Modifier.testTag("voice_input_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Catat dengan Suara (Voice Over)",
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                .padding(bottom = 12.dp)
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
                .padding(bottom = 12.dp),
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
                            .height(36.dp),
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

        // Time Period Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf("Semua", "Harian", "Mingguan", "Bulanan")
            chips.forEach { item ->
                val isSelected = selectedFilter == item
                FilterChip(
                    selected = isSelected,
                    onClick = { financeViewModel.setSelectedFilter(item) },
                    label = {
                        Text(
                            text = item,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Summary Statistics Card for current filter
        if (filteredTransactions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
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

        // List Display
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
                        text = "Coba ubah kata kunci pencarian atau filter Anda",
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
                items(filteredTransactions, key = { it.id }) { item ->
                    TransactionRowItem(
                        transaction = item,
                        onClick = {
                            navController.navigate("transaction_detail/${item.id}")
                        }
                    )
                }
            }
        }
    }

    ReceiptScanDialog(
        isOpen = showScanDialog,
        onDismiss = { showScanDialog = false },
        financeViewModel = financeViewModel
    )

    VoiceInputDialog(
        isOpen = showVoiceDialog,
        onDismiss = { showVoiceDialog = false },
        financeViewModel = financeViewModel
    )
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
