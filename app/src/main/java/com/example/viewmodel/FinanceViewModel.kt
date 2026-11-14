package com.example.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Transaction
import com.example.data.repository.AuthRepository
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository,
    context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("moneyflow_settings", Context.MODE_PRIVATE)

    // Screen and global state
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isAutoSync = MutableStateFlow(prefs.getBoolean("auto_sync", true))
    val isAutoSync: StateFlow<Boolean> = _isAutoSync.asStateFlow()

    private val _isDailyReminder = MutableStateFlow(prefs.getBoolean("daily_reminder", true))
    val isDailyReminder: StateFlow<Boolean> = _isDailyReminder.asStateFlow()

    private val _firebaseSyncStatus = MutableStateFlow(prefs.getString("fb_sync_status", "Tersinkronisasi (Lokal)") ?: "Tersinkronisasi (Lokal)")
    val firebaseSyncStatus: StateFlow<String> = _firebaseSyncStatus.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    // Target Anggaran (Budget Target) & Periode (Month Calendar)
    private val _targetBudget = MutableStateFlow(prefs.getFloat("target_budget", 5000000f).toDouble())
    val targetBudget: StateFlow<Double> = _targetBudget.asStateFlow()

    private val _selectedMonth = MutableStateFlow(prefs.getInt("selected_month", Calendar.getInstance().get(Calendar.MONTH)))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(prefs.getInt("selected_year", Calendar.getInstance().get(Calendar.YEAR)))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    // Additional Portfolio: Hutang Saya, Piutang Orang, Emas, and Initial Balances for accounts
    private val _myDebt = MutableStateFlow(prefs.getFloat("my_debt", 0f).toDouble())
    val myDebt: StateFlow<Double> = _myDebt.asStateFlow()

    private val _receivables = MutableStateFlow(prefs.getFloat("receivables", 0f).toDouble())
    val receivables: StateFlow<Double> = _receivables.asStateFlow()

    private val _goldValue = MutableStateFlow(prefs.getFloat("gold_value", 0f).toDouble())
    val goldValue: StateFlow<Double> = _goldValue.asStateFlow()

    // Initial balances for account types to make it realistic
    private val _initialCash = MutableStateFlow(prefs.getFloat("initial_cash", 0f).toDouble())
    val initialCash: StateFlow<Double> = _initialCash.asStateFlow()

    private val _initialEWallet = MutableStateFlow(prefs.getFloat("initial_ewallet", 0f).toDouble())
    val initialEWallet: StateFlow<Double> = _initialEWallet.asStateFlow()

    private val _initialBca = MutableStateFlow(prefs.getFloat("initial_bca", 0f).toDouble())
    val initialBca: StateFlow<Double> = _initialBca.asStateFlow()

    private val _initialBri = MutableStateFlow(prefs.getFloat("initial_bri", 0f).toDouble())
    val initialBri: StateFlow<Double> = _initialBri.asStateFlow()

    private val _initialDanamon = MutableStateFlow(prefs.getFloat("initial_danamon", 0f).toDouble())
    val initialDanamon: StateFlow<Double> = _initialDanamon.asStateFlow()

    private val _initialOther = MutableStateFlow(prefs.getFloat("initial_other", 0f).toDouble())
    val initialOther: StateFlow<Double> = _initialOther.asStateFlow()

    // OpenRouter AI API configuration
    private val _openRouterBaseUrl = MutableStateFlow(prefs.getString("openrouter_base_url", "https://openrouter.ai/api/v1") ?: "https://openrouter.ai/api/v1")
    val openRouterBaseUrl: StateFlow<String> = _openRouterBaseUrl.asStateFlow()

    private val _openRouterApiKey = MutableStateFlow(prefs.getString("openrouter_api_key", "") ?: "")
    val openRouterApiKey: StateFlow<String> = _openRouterApiKey.asStateFlow()

    private val _openRouterModel = MutableStateFlow(prefs.getString("openrouter_model", "google/gemini-2.0-flash-001") ?: "google/gemini-2.0-flash-001")
    val openRouterModel: StateFlow<String> = _openRouterModel.asStateFlow()

    // Filter and search on history screen
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("Semua") // "Harian", "Mingguan", "Bulanan", "Semua"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    private var activeUserId: Int = 1

    init {
        // Collect logged in user changes
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                val userId = user?.id ?: authRepository.getLoggedUserId()
                activeUserId = if (userId != -1) userId else 1
                loadTransactionsForUser(activeUserId)
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun setAutoSync(enabled: Boolean) {
        _isAutoSync.value = enabled
        prefs.edit().putBoolean("auto_sync", enabled).apply()
    }

    fun setDailyReminder(enabled: Boolean) {
        _isDailyReminder.value = enabled
        prefs.edit().putBoolean("daily_reminder", enabled).apply()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setOpenRouterConfig(baseUrl: String, apiKey: String, model: String) {
        val trimmedUrl = baseUrl.trim()
        val trimmedKey = apiKey.trim()
        val trimmedModel = model.trim()

        _openRouterBaseUrl.value = trimmedUrl
        _openRouterApiKey.value = trimmedKey
        _openRouterModel.value = trimmedModel

        prefs.edit()
            .putString("openrouter_base_url", trimmedUrl)
            .putString("openrouter_api_key", trimmedKey)
            .putString("openrouter_model", trimmedModel)
            .apply()

        viewModelScope.launch {
            _uiMessage.emit("Konfigurasi OpenRouter AI berhasil disimpan!")
        }
    }

    fun buildDatabaseContextForAi(): String {
        val allTrans = _transactions.value
        val totalIncome = allTrans.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = allTrans.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalBalance = totalIncome - totalExpense

        // Calculate balances per wallet
        fun getWalletBalance(walletName: String, initial: Double): Double {
            val inc = allTrans.filter { it.walletAccount.equals(walletName, ignoreCase = true) && it.type == "INCOME" }.sumOf { it.amount }
            val exp = allTrans.filter { it.walletAccount.equals(walletName, ignoreCase = true) && it.type == "EXPENSE" }.sumOf { it.amount }
            return initial + inc - exp
        }

        val balCash = getWalletBalance("Tunai", _initialCash.value)
        val balEWallet = getWalletBalance("E-Wallet", _initialEWallet.value)
        val balBca = getWalletBalance("BCA", _initialBca.value)
        val balBri = getWalletBalance("BRI", _initialBri.value)
        val balDanamon = getWalletBalance("Danamon", _initialDanamon.value)
        val balOther = getWalletBalance("Lainnya", _initialOther.value)

        // Category breakdown for expenses
        val expenseByCategory = allTrans.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val categorySummary = if (expenseByCategory.isEmpty()) "Belum ada pengeluaran" else {
            expenseByCategory.joinToString(", ") { "${it.first}: Rp ${String.format("%,.0f", it.second)}" }
        }

        // 50/30/20 breakdown
        val needsCategories = setOf("Makanan", "Transportasi", "Tagihan Rutin", "Kesehatan", "Tempat Tinggal", "Pajak", "Kebutuhan Sekolah")
        val wantsCategories = setOf("Belanja(Shopping)", "Hiburan", "Minuman", "Jajan", "Perawatan Diri", "Langganan")
        val savingsCategories = setOf("Cicilan/Hutang(Produktif)", "Zakat & Sedekah")

        val needsTotal = allTrans.filter { it.type == "EXPENSE" && it.category in needsCategories }.sumOf { it.amount }
        val wantsTotal = allTrans.filter { it.type == "EXPENSE" && it.category in wantsCategories }.sumOf { it.amount }
        val savingsTotal = allTrans.filter { it.type == "EXPENSE" && it.category in savingsCategories }.sumOf { it.amount }

        // Recent 5 transactions
        val recentList = allTrans.take(5).joinToString("\n") { t ->
            val dStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(t.date))
            "- [${t.type}] Rp ${String.format("%,.0f", t.amount)} (${t.category}, ${t.walletAccount}) - ${t.note} ($dStr)"
        }

        return """
            Ringkasan Keuangan Pengguna Saat Ini:
            - Total Pemasukan: Rp ${String.format("%,.0f", totalIncome)}
            - Total Pengeluaran: Rp ${String.format("%,.0f", totalExpense)}
            - Sisa Saldo Bersih: Rp ${String.format("%,.0f", totalBalance)}
            
            Saldo per Dompet / Rekening:
            - Tunai: Rp ${String.format("%,.0f", balCash)}
            - E-Wallet: Rp ${String.format("%,.0f", balEWallet)}
            - Bank BCA: Rp ${String.format("%,.0f", balBca)}
            - Bank BRI: Rp ${String.format("%,.0f", balBri)}
            - Bank Danamon: Rp ${String.format("%,.0f", balDanamon)}
            - Rekening Lainnya: Rp ${String.format("%,.0f", balOther)}

            Alokasi Aturan 50/30/20:
            - Kebutuhan (Needs - 50%): Rp ${String.format("%,.0f", needsTotal)}
            - Keinginan (Wants - 30%): Rp ${String.format("%,.0f", wantsTotal)}
            - Tabungan & Investasi (Savings - 20%): Rp ${String.format("%,.0f", savingsTotal)}

            Portofolio Tambahan:
            - Hutang Saya: Rp ${String.format("%,.0f", _myDebt.value)}
            - Piutang Orang: Rp ${String.format("%,.0f", _receivables.value)}
            - Nilai Aset Emas: Rp ${String.format("%,.0f", _goldValue.value)}

            Top 5 Kategori Pengeluaran:
            $categorySummary

            5 Transaksi Terakhir:
            ${if (recentList.isBlank()) "(Belum ada transaksi)" else recentList}
        """.trimIndent()
    }

    private fun loadTransactionsForUser(userId: Int) {
        viewModelScope.launch {
            transactionRepository.getAllTransactions(userId).collect { list ->
                _transactions.value = list
            }
        }
    }

    fun addTransaction(amount: Double, type: String, category: String, note: String, date: Long, walletAccount: String = "Tunai") {
        viewModelScope.launch {
            val user = authRepository.loadSavedUser()
            val userId = user?.id ?: authRepository.getLoggedUserId()
            val finalUserId = if (userId != -1) userId else 1

            val transaction = Transaction(
                amount = amount,
                type = type,
                category = category,
                note = note,
                date = date,
                userId = finalUserId,
                walletAccount = walletAccount
            )
            transactionRepository.insertTransaction(transaction)
            _uiMessage.emit("Transaksi berhasil disimpan!")
            
            // Trigger Firebase sync simulation if enabled
            if (_isAutoSync.value) {
                simulateFirebaseSync()
            }
        }
    }

    fun addTransaction(transaction: Transaction) {
        addTransaction(
            amount = transaction.amount,
            type = transaction.type,
            category = transaction.category,
            note = transaction.note,
            date = transaction.date,
            walletAccount = transaction.walletAccount
        )
    }

    fun addMultipleTransactions(transactions: List<Transaction>) {
        viewModelScope.launch {
            val user = authRepository.loadSavedUser()
            val userId = user?.id ?: authRepository.getLoggedUserId()
            val finalUserId = if (userId != -1) userId else 1

            transactions.forEach { t ->
                val finalTransaction = t.copy(userId = finalUserId)
                transactionRepository.insertTransaction(finalTransaction)
            }
            _uiMessage.emit("${transactions.size} transaksi berhasil direkap!")

            if (_isAutoSync.value) {
                simulateFirebaseSync()
            }
        }
    }

    fun setTargetBudget(amount: Double) {
        _targetBudget.value = amount
        prefs.edit().putFloat("target_budget", amount.toFloat()).apply()
    }

    fun setSelectedMonth(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
        prefs.edit().putInt("selected_month", month).putInt("selected_year", year).apply()
    }

    fun updatePortfolio(debt: Double, receivables: Double, gold: Double) {
        _myDebt.value = debt
        _receivables.value = receivables
        _goldValue.value = gold
        prefs.edit()
            .putFloat("my_debt", debt.toFloat())
            .putFloat("receivables", receivables.toFloat())
            .putFloat("gold_value", gold.toFloat())
            .apply()
    }

    fun updateInitialBalances(cash: Double, eWallet: Double, bca: Double, bri: Double, danamon: Double, other: Double) {
        _initialCash.value = cash
        _initialEWallet.value = eWallet
        _initialBca.value = bca
        _initialBri.value = bri
        _initialDanamon.value = danamon
        _initialOther.value = other
        prefs.edit()
            .putFloat("initial_cash", cash.toFloat())
            .putFloat("initial_ewallet", eWallet.toFloat())
            .putFloat("initial_bca", bca.toFloat())
            .putFloat("initial_bri", bri.toFloat())
            .putFloat("initial_danamon", danamon.toFloat())
            .putFloat("initial_other", other.toFloat())
            .apply()
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
            _uiMessage.emit("Transaksi berhasil diperbarui!")
            if (_isAutoSync.value) {
                simulateFirebaseSync()
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
            _uiMessage.emit("Transaksi berhasil dihapus!")
            if (_isAutoSync.value) {
                simulateFirebaseSync()
            }
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            transactionRepository.deleteAllTransactions(activeUserId)
            _uiMessage.emit("Semua transaksi berhasil dihapus!")
            if (_isAutoSync.value) {
                simulateFirebaseSync()
            }
        }
    }

    fun simulateFirebaseSync() {
        viewModelScope.launch {
            _firebaseSyncStatus.value = "Menyinkronkan data..."
            kotlinx.coroutines.delay(1200) // Startup loading delay simulation
            val currentTimestamp = Calendar.getInstance().time.toString().substring(0, 19)
            val message = "Cloud synced: $currentTimestamp"
            _firebaseSyncStatus.value = message
            prefs.edit().putString("fb_sync_status", message).apply()
            _uiMessage.emit("Tersinkronisasi dengan Firebase Cloud!")
        }
    }

    fun exportData(format: String): String {
        val header = "ID,Tipe,Nominal,Kategori,Catatan,Tanggal\n"
        val rows = _transactions.value.joinToString("\n") { t ->
            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(t.date))
            "${t.id},${t.type},${t.amount},${t.category},\"${t.note}\",${dateStr}"
        }
        val csvContent = header + rows
        viewModelScope.launch {
            _uiMessage.emit("Ekspor data $format berhasil dibagikan!")
        }
        return csvContent
    }
}
