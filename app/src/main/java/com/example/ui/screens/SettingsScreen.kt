package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.horizontalScroll
import com.example.ui.theme.SoftGreenSuccess
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    navController: NavController,
    financeViewModel: FinanceViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()

    val isDarkMode by financeViewModel.isDarkMode.collectAsState()
    val isAutoSync by financeViewModel.isAutoSync.collectAsState()
    val isDailyReminder by financeViewModel.isDailyReminder.collectAsState()
    val firebaseSyncStatus by financeViewModel.firebaseSyncStatus.collectAsState()
    val transactions by financeViewModel.transactions.collectAsState()

    val initialCash by financeViewModel.initialCash.collectAsState()
    val initialEWallet by financeViewModel.initialEWallet.collectAsState()
    val initialBca by financeViewModel.initialBca.collectAsState()
    val initialBri by financeViewModel.initialBri.collectAsState()
    val initialDanamon by financeViewModel.initialDanamon.collectAsState()
    val initialOther by financeViewModel.initialOther.collectAsState()

    val openRouterBaseUrl by financeViewModel.openRouterBaseUrl.collectAsState()
    val openRouterApiKey by financeViewModel.openRouterApiKey.collectAsState()
    val openRouterModel by financeViewModel.openRouterModel.collectAsState()

    var inputBaseUrl by remember(openRouterBaseUrl) { mutableStateOf(openRouterBaseUrl) }
    var inputApiKey by remember(openRouterApiKey) { mutableStateOf(openRouterApiKey) }
    var inputModel by remember(openRouterModel) { mutableStateOf(openRouterModel) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    // Trigger toast alerts from UI message stream
    LaunchedEffect(key1 = true) {
        financeViewModel.uiMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .padding(bottom = 100.dp)
    ) {
        Text(
            text = "Pengaturan & Profil",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // User profile summary card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Pic",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = currentUser?.fullName ?: "Pengguna Offline",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = currentUser?.email ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    
                    val dateFormatted = remember(currentUser) {
                        currentUser?.createdAt?.let {
                            SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID")).format(Date(it))
                        } ?: ""
                    }
                    if (dateFormatted.isNotBlank()) {
                        Text(
                            text = "Joined: $dateFormatted",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Section header
        SettingSectionHeader(title = "Tampilan")

        // Dark Mode Toggle row
        SettingToggleRow(
            icon = Icons.Default.Brightness4,
            title = "Mode Gelap (Dark Mode)",
            subtitle = "Maksimalkan visibilitas malam hari",
            checked = isDarkMode,
            onCheckedChange = { financeViewModel.setDarkMode(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))



        // Section header
        SettingSectionHeader(title = "Keamanan & Sinkronisasi")

        SettingToggleRow(
            icon = Icons.Default.Sync,
            title = "Sinkronisasi Otomatis",
            subtitle = "Sinkronkan dengan Firestore Cloud",
            checked = isAutoSync,
            onCheckedChange = { financeViewModel.setAutoSync(it) }
        )

        // Cloud Backup simulation box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Backup Cloud Firebase",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = firebaseSyncStatus,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Button(
                        onClick = { financeViewModel.simulateFirebaseSync() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("Backup", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        SettingToggleRow(
            icon = Icons.Default.NotificationsActive,
            title = "Notifikasi Pengingat",
            subtitle = "Pengingat pencatatan harian harian",
            checked = isDailyReminder,
            onCheckedChange = { financeViewModel.setDailyReminder(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Section header: Exports
        SettingSectionHeader(title = "Ekspor Laporan")

        SettingClickableRow(
            icon = Icons.Default.Analytics,
            title = "Ekspor Format Excel (.xlsx)",
            subtitle = "Unduh seluruh rekap riwayat format spreadsheet Excel",
            onClick = {
                val file = com.example.utils.ExportUtils.exportToXlsx(context, transactions)
                if (file != null && file.exists()) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan MoneyFlow (.xlsx)")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Ekspor Laporan Excel (.xlsx) Via"))
                } else {
                    Toast.makeText(context, "Gagal membuat laporan Excel (.xlsx)", Toast.LENGTH_SHORT).show()
                }
            }
        )

        SettingClickableRow(
            icon = Icons.Default.Description,
            title = "Ekspor Format CSV",
            subtitle = "Cetak seluruh rekap riwayat format CSV",
            onClick = {
                val file = com.example.utils.ExportUtils.exportToCsv(context, transactions)
                if (file != null && file.exists()) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan MoneyFlow (.csv)")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Ekspor Laporan CSV (.csv) Via"))
                } else {
                    Toast.makeText(context, "Gagal membuat laporan CSV (.csv)", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // OpenRouter Custom AI API Configuration Section
        SettingSectionHeader(title = "KONEKSI AI OPENROUTER (CUSTOM API)")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Kecerdasan Buatan (OpenRouter AI)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (openRouterApiKey.isNotBlank()) "Status: Terhubung & Aktif" else "Status: Mode Lokal (Kunci belum diisi)",
                                fontSize = 10.sp,
                                color = if (openRouterApiKey.isNotBlank()) SoftGreenSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Digunakan untuk Scan Struk OCR, Voice Over pencatatan suara, dan Asisten AI Keuangan di kolom Chat.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Base URL
                OutlinedTextField(
                    value = inputBaseUrl,
                    onValueChange = { inputBaseUrl = it },
                    label = { Text("Base URL API") },
                    placeholder = { Text("https://openrouter.ai/api/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // API Key
                OutlinedTextField(
                    value = inputApiKey,
                    onValueChange = { inputApiKey = it },
                    label = { Text("OpenRouter API Key (sk-or-v1-...)") },
                    placeholder = { Text("Masukkan API Key OpenRouter") },
                    singleLine = true,
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isApiKeyVisible) "Sembunyikan" else "Tampilkan"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Model Selection
                OutlinedTextField(
                    value = inputModel,
                    onValueChange = { inputModel = it },
                    label = { Text("Model AI Chat & Vision (https://openrouter.ai/models)") },
                    placeholder = { Text("google/gemini-2.0-flash-001") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick model selector chips
                Text("Pilihan Model Cepat:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val quickModels = listOf(
                    "google/gemini-2.0-flash-001",
                    "openai/gpt-4o-mini",
                    "meta-llama/llama-3.3-70b-instruct:free",
                    "deepseek/deepseek-chat"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickModels.forEach { m ->
                        FilterChip(
                            selected = inputModel == m,
                            onClick = { inputModel = m },
                            label = { Text(m.substringAfter("/"), fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        financeViewModel.setOpenRouterConfig(
                            baseUrl = inputBaseUrl.ifBlank { "https://openrouter.ai/api/v1" },
                            apiKey = inputApiKey,
                            model = inputModel.ifBlank { "google/gemini-2.0-flash-001" }
                        )
                        Toast.makeText(context, "Konfigurasi OpenRouter berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_save_openrouter_settings"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Konfigurasi AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout action
        Button(
            onClick = {
                authViewModel.logout()
                Toast.makeText(context, "Berhasil keluar!", Toast.LENGTH_SHORT).show()
                navController.navigate("login") {
                    popUpTo("main") { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Logout")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout dari Akun", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Arrow right",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}
