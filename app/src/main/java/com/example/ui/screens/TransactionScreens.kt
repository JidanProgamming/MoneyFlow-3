package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.model.Transaction
import com.example.ui.theme.SoftGreenSuccess
import com.example.ui.theme.SoftRedDanger
import com.example.utils.FormatUtils
import com.example.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(navController: NavController, financeViewModel: FinanceViewModel) {
    val context = LocalContext.current

    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // INCOME or EXPENSE
    var selectedCategory by remember { mutableStateOf("Tempat Tinggal") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedWallet by remember { mutableStateOf("Tunai") }
    var customBankName by remember { mutableStateOf("") }

    val categories = listOf(
        "Tempat Tinggal", "Tagihan Rutin", "Pulsa/Data", "Langganan", 
        "Belanja(Shopping)", "Kesehatan", "Pajak", "Cicilan/Hutang(Produktif)", "Cicilan/Hutang(Konsumtif)", 
        "Zakat & Sedekah", "Perawatan Diri", "Keluarga", "Kebutuhan Sekolah",
        "Makanan", "Transportasi", "Hiburan", "E-Wallet", "Minuman", "Jajan", "Lainnya"
    )

    val dateString = remember(selectedDate) {
        SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID")).format(Date(selectedDate))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Transaksi") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nominal Amount Input field
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Nominal Uang (Rp)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { input ->
                            // numerical digits filters
                            if (input.all { it.isDigit() }) {
                                amountStr = input
                            }
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        placeholder = { Text("0", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_field")
                    )
                }
            }

            // Segmented buttons tab selectors: Pemasukan vs Pengeluaran
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { 
                        selectedType = "EXPENSE" 
                        if (selectedCategory in listOf("Gaji Utama", "Bonus/Insentif", "Pekerjaan Sampingan(Freelancer)", "Penjualan", "Gaji")) {
                            selectedCategory = "Tempat Tinggal"
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == "EXPENSE") SoftRedDanger else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedType == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Pengeluaran", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { 
                        selectedType = "INCOME" 
                        selectedCategory = "Gaji Utama"
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == "INCOME") SoftGreenSuccess else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedType == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Pemasukan", fontWeight = FontWeight.SemiBold)
                }
            }

            // Live 50/30/20 allocation for income
            val typedAmount = amountStr.toDoubleOrNull() ?: 0.0
            if (selectedType == "INCOME" && typedAmount > 0.0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("allocation_50_30_20_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Metode 50/30/20",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "💡 Rencana Alokasi Metode 50/30/20",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Row 1: Kebutuhan (50%)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("50%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Kebutuhan (Needs)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Makanan, Transportasi, Cicilan, Kos", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                text = FormatUtils.formatRupiah(typedAmount * 0.5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        
                        // Row 2: Keinginan (30%)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFB300).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("30%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD48A00))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Keinginan (Wants)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Hiburan, Kopi, Jajan, Gadget, dll.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                text = FormatUtils.formatRupiah(typedAmount * 0.3),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD48A00)
                            )
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        
                        // Row 3: Tabungan (20%)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(SoftGreenSuccess.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("20%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SoftGreenSuccess)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Tabungan & Investasi (Savings)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Dana Darurat, Tabungan, Emas, Saham", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                text = FormatUtils.formatRupiah(typedAmount * 0.2),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftGreenSuccess
                            )
                        }
                    }
                }
            }

            // Category Picker Grid Drawer
            Text(
                text = "Pilih Kategori",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filter categories: restrict 'Gaji' only under INCOME, others mostly under EXPENSE
                val filteredCats = if (selectedType == "INCOME") {
                    listOf("Gaji Utama", "Bonus/Insentif", "Pekerjaan Sampingan(Freelancer)", "Penjualan", "Lainnya")
                } else {
                    categories.filter { it != "Gaji Utama" && it != "Bonus/Insentif" && it != "Pekerjaan Sampingan(Freelancer)" && it != "Penjualan" && it != "Piutang" }
                }

                items(filteredCats) { cat ->
                    val isSelected = selectedCategory == cat
                    val iconColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else getCategoryColor(cat)
                    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                    Card(
                        modifier = Modifier
                            .clickable { selectedCategory = cat }
                            .testTag("cat_chip_$cat"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(cat),
                                contentDescription = cat,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Form: Notes text input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Tambah Catatan") },
                placeholder = { Text("e.g. Beli kopi susu senja") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input"),
                singleLine = true
            )

            // Form: Wallet selector
            Text(
                text = "Dari/Ke Dompet",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val wallets = listOf("Tunai", "E-Wallet", "Rekening 1", "Rekening 2", "Rekening 3", "Lainnya")
                        wallets.forEach { wallet ->
                            val isSelected = selectedWallet == wallet
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedWallet = wallet },
                                label = { Text(wallet, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    if (selectedWallet == "Lainnya") {
                        OutlinedTextField(
                            value = customBankName,
                            onValueChange = { customBankName = it },
                            label = { Text("Nama Bank Lainnya (Ketik Manual)") },
                            placeholder = { Text("e.g. Bank Mandiri, CIMB Niaga") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Form: Date Picker Row clickable
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val selected = Calendar.getInstance()
                                selected.set(year, month, day)
                                selectedDate = selected.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = "Tanggal", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tanggal Transaksi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(text = dateString, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save transaction CTA Button
            Button(
                onClick = {
                    val nominal = amountStr.toDoubleOrNull() ?: 0.0
                    if (nominal <= 0.0) {
                        Toast.makeText(context, "Nominal nominal harus lebih besar dari Rp 0", Toast.LENGTH_SHORT).show()
                    } else {
                        val finalWallet = if (selectedWallet == "Lainnya") {
                            if (customBankName.isNotBlank()) customBankName else "Lainnya"
                        } else {
                            selectedWallet
                        }
                        financeViewModel.addTransaction(
                            amount = nominal,
                            type = selectedType,
                            category = selectedCategory,
                            note = note,
                            date = selectedDate,
                            walletAccount = finalWallet
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan Transaksi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Int,
    navController: NavController,
    financeViewModel: FinanceViewModel
) {
    val context = LocalContext.current
    val transactions by financeViewModel.transactions.collectAsState()

    val transaction = remember(transactions) {
        transactions.find { it.id == transactionId }
    }

    if (transaction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Transaksi tidak ditemukan")
        }
        return
    }

    val typeColor = if (transaction.type == "INCOME") SoftGreenSuccess else SoftRedDanger
    val iconColor = typeColor
    val categoryIcon = getCategoryIcon(transaction.category)

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Transaksi") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("edit_transaction/${transaction.id}") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Transaksi", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus Transaksi", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Circle symbol icon of the Category type
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = transaction.category,
                    tint = iconColor,
                    modifier = Modifier.size(38.dp)
                )
            }

            Text(
                text = if (transaction.type == "INCOME") "Dana Masuk" else "Dana Keluar",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Text(
                text = (if (transaction.type == "INCOME") "+" else "-") + FormatUtils.formatRupiah(transaction.amount),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = typeColor
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Sub details fields card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailRow(label = "Kategori", value = transaction.category)
                    DetailRow(label = "Dari/Ke Dompet", value = transaction.walletAccount.ifBlank { "Tunai" })
                    DetailRow(label = "Catatan", value = transaction.note.ifBlank { "-" })
                    
                    val transDate = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("in", "ID")).format(Date(transaction.date))
                    DetailRow(label = "Waktu", value = transDate)
                }
            }
        }
    }

    // Interactive Delete Dialogue box validation popup
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Transaksi?") },
            text = { Text("Tindakan ini permanen dan tidak bisa dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        financeViewModel.deleteTransaction(transaction)
                        navController.popBackStack()
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    transactionId: Int,
    navController: NavController,
    financeViewModel: FinanceViewModel
) {
    val context = LocalContext.current
    val transactions by financeViewModel.transactions.collectAsState()

    val transaction = remember(transactions) {
        transactions.find { it.id == transactionId }
    }

    if (transaction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Transaksi tidak ditemukan")
        }
        return
    }

    val initialWallet = transaction.walletAccount
    val displayWallet = when(initialWallet) {
        "BCA" -> "Rekening 1"
        "BRI" -> "Rekening 2"
        "Danamon" -> "Rekening 3"
        else -> initialWallet
    }
    val isKnownWallet = displayWallet in listOf("Tunai", "E-Wallet", "Rekening 1", "Rekening 2", "Rekening 3")

    var amountStr by remember { mutableStateOf(transaction.amount.toInt().toString()) }
    var note by remember { mutableStateOf(transaction.note) }
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var selectedDate by remember { mutableStateOf(transaction.date) }
    var selectedWallet by remember { mutableStateOf(if (isKnownWallet) displayWallet else if (displayWallet.isNotBlank()) "Lainnya" else "Tunai") }
    var customBankName by remember { mutableStateOf(if (!isKnownWallet) displayWallet else "") }

    val categories = listOf(
        "Tempat Tinggal", "Tagihan Rutin", "Pulsa/Data", "Langganan", 
        "Belanja(Shopping)", "Kesehatan", "Pajak", "Cicilan/Hutang(Produktif)", "Cicilan/Hutang(Konsumtif)", 
        "Zakat & Sedekah", "Perawatan Diri", "Keluarga", "Kebutuhan Sekolah",
        "Makanan", "Transportasi", "Hiburan", "E-Wallet", "Minuman", "Jajan", "Lainnya"
    )

    val dateString = remember(selectedDate) {
        SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID")).format(Date(selectedDate))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Transaksi") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Nominal Uang (Rp)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                amountStr = input
                            }
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        placeholder = { Text("0", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_field")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { 
                        selectedType = "EXPENSE" 
                        if (selectedCategory in listOf("Gaji Utama", "Bonus/Insentif", "Pekerjaan Sampingan(Freelancer)", "Penjualan", "Gaji")) {
                            selectedCategory = "Tempat Tinggal"
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == "EXPENSE") SoftRedDanger else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedType == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Pengeluaran", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { 
                        selectedType = "INCOME" 
                        selectedCategory = "Gaji Utama"
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == "INCOME") SoftGreenSuccess else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedType == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Pemasukan", fontWeight = FontWeight.SemiBold)
                }
            }

            // Live 50/30/20 allocation for income (Edit)
            val typedAmount = amountStr.toDoubleOrNull() ?: 0.0
            if (selectedType == "INCOME" && typedAmount > 0.0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("allocation_50_30_20_card_edit"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Metode 50/30/20",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "💡 Alokasi Metode 50/30/20",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Row 1: Kebutuhan (50%)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("50%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Kebutuhan (Needs)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Makanan, Transportasi, Cicilan, Kos", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                text = FormatUtils.formatRupiah(typedAmount * 0.5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        
                        // Row 2: Keinginan (30%)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFB300).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("30%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD48A00))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Keinginan (Wants)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Hiburan, Kopi, Jajan, Gadget, dll.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                text = FormatUtils.formatRupiah(typedAmount * 0.3),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD48A00)
                            )
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        
                        // Row 3: Tabungan (20%)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(SoftGreenSuccess.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("20%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SoftGreenSuccess)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Tabungan & Investasi (Savings)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Dana Darurat, Tabungan, Emas, Saham", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                text = FormatUtils.formatRupiah(typedAmount * 0.2),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftGreenSuccess
                            )
                        }
                    }
                }
            }

            Text(
                text = "Pilih Kategori",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filteredCats = if (selectedType == "INCOME") {
                    listOf("Gaji Utama", "Bonus/Insentif", "Pekerjaan Sampingan(Freelancer)", "Penjualan", "Lainnya")
                } else {
                    categories.filter { it != "Gaji Utama" && it != "Bonus/Insentif" && it != "Pekerjaan Sampingan(Freelancer)" && it != "Penjualan" && it != "Piutang" }
                }

                items(filteredCats) { cat ->
                    val isSelected = selectedCategory == cat
                    val iconColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else getCategoryColor(cat)
                    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                    Card(
                        modifier = Modifier
                            .clickable { selectedCategory = cat }
                            .testTag("cat_chip_$cat"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(cat),
                                contentDescription = cat,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Tambah Catatan") },
                placeholder = { Text("Beli kopi susu...") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input"),
                singleLine = true
            )

            // Form: Wallet selector
            Text(
                text = "Dari/Ke Dompet",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val wallets = listOf("Tunai", "E-Wallet", "Rekening 1", "Rekening 2", "Rekening 3", "Lainnya")
                        wallets.forEach { wallet ->
                            val isSelected = selectedWallet == wallet
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedWallet = wallet },
                                label = { Text(wallet, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    if (selectedWallet == "Lainnya") {
                        OutlinedTextField(
                            value = customBankName,
                            onValueChange = { customBankName = it },
                            label = { Text("Nama Bank Lainnya (Ketik Manual)") },
                            placeholder = { Text("e.g. Bank Mandiri, CIMB Niaga") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val selected = Calendar.getInstance()
                                selected.set(year, month, day)
                                selectedDate = selected.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = "Tanggal", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tanggal Transaksi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(text = dateString, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val nominal = amountStr.toDoubleOrNull() ?: 0.0
                    if (nominal <= 0.0) {
                        Toast.makeText(context, "Nominal nominal harus lebih besar dari Rp 0", Toast.LENGTH_SHORT).show()
                    } else {
                        val finalWallet = if (selectedWallet == "Lainnya") {
                            if (customBankName.isNotBlank()) customBankName else "Lainnya"
                        } else {
                            selectedWallet
                        }
                        val updated = transaction.copy(
                            amount = nominal,
                            type = selectedType,
                            category = selectedCategory,
                            note = note,
                            date = selectedDate,
                            walletAccount = finalWallet
                        )
                        financeViewModel.updateTransaction(updated)
                        // pop back detailed screen too so it displays updated values in the list
                        navController.navigate("main") {
                            popUpTo("main") { inclusive = false }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Perbarui Transaksi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
