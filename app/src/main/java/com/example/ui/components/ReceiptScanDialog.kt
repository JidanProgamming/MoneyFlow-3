package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SoftGreenSuccess
import com.example.ui.theme.SoftRedDanger
import com.example.utils.FormatUtils
import com.example.utils.OpenRouterAiService
import com.example.viewmodel.FinanceViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    financeViewModel: FinanceViewModel,
    onTransactionSaved: (() -> Unit)? = null
) {
    if (!isOpen) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val baseUrl by financeViewModel.openRouterBaseUrl.collectAsState()
    val apiKey by financeViewModel.openRouterApiKey.collectAsState()
    val model by financeViewModel.openRouterModel.collectAsState()

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingStep by remember { mutableStateOf("") }

    // Extracted Fields for transaction
    var amountStr by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("EXPENSE") } // EXPENSE or INCOME
    var selectedCategory by remember { mutableStateOf("Belanja(Shopping)") }
    var selectedWallet by remember { mutableStateOf("Tunai") }
    var noteStr by remember { mutableStateOf("") }
    var transactionDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var ocrRawText by remember { mutableStateOf("") }

    val categories = listOf(
        "Makanan", "Belanja(Shopping)", "Transportasi", "Tagihan Rutin",
        "Kesehatan", "Pulsa/Data", "Langganan", "Pajak", "Cicilan/Hutang(Produktif)",
        "Cicilan/Hutang(Konsumtif)", "Hiburan", "E-Wallet", "Minuman", "Jajan",
        "Tempat Tinggal", "Lainnya"
    )

    val wallets = listOf("Tunai", "E-Wallet", "BCA", "BRI", "Danamon", "Lainnya")

    // Process bitmap through ML Kit OCR and then OpenRouter AI
    fun processImage(bitmap: Bitmap) {
        selectedBitmap = bitmap
        isProcessing = true
        processingStep = "Membaca teks struk (OCR)..."

        coroutineScope.launch {
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text
                        ocrRawText = text
                        processingStep = "Menganalisis dengan AI OpenRouter..."

                        coroutineScope.launch {
                            val parsed = OpenRouterAiService.parseReceiptText(
                                ocrText = text,
                                baseUrl = baseUrl,
                                apiKey = apiKey,
                                model = model
                            )

                            amountStr = if (parsed.amount > 0) String.format(Locale.US, "%.0f", parsed.amount) else ""
                            transactionType = parsed.type
                            selectedCategory = parsed.category
                            selectedWallet = parsed.walletAccount
                            noteStr = parsed.note
                            transactionDate = parsed.date

                            isProcessing = false
                            processingStep = ""
                            Toast.makeText(context, "Struk berhasil dianalisis AI!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        isProcessing = false
                        processingStep = ""
                        Toast.makeText(context, "Gagal membaca struk: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                isProcessing = false
                processingStep = ""
                Toast.makeText(context, "Kesalahan OCR: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            processImage(bitmap)
        }
    }

    // Photo picker launcher (Gallery / Files)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                processImage(bitmap)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat gambar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isProcessing) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("receipt_scan_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pindai Struk / Bukti Bayar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "OCR Pintar + AI OpenRouter",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons to pick Camera or File/Gallery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_capture_camera"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kamera", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_pick_gallery"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("File / Galeri", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Form content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Image Preview / Placeholder
                    if (selectedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Foto Struk",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isProcessing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(processingStep, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ambil foto dari kamera atau pilih file nota struk",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Type Selector (Pemasukan / Pengeluaran)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = transactionType == "EXPENSE",
                            onClick = { transactionType = "EXPENSE" },
                            label = { Text("Pengeluaran") },
                            leadingIcon = {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = SoftRedDanger, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = transactionType == "INCOME",
                            onClick = { transactionType = "INCOME" },
                            label = { Text("Pemasukan") },
                            leadingIcon = {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = SoftGreenSuccess, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Amount Field
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Nominal Uang (Rp)") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        leadingIcon = {
                            Text("Rp", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Note / Merchant
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("Catatan / Nama Toko / Keterangan") },
                        placeholder = { Text("Struk Belanja") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selector
                    Text("Kategori:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var categoryExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Wallet Selector
                    Text("Dari / Ke Dompet:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var walletExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = walletExpanded,
                        onExpandedChange = { walletExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedWallet,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = walletExpanded,
                            onDismissRequest = { walletExpanded = false }
                        ) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w) },
                                    onClick = {
                                        selectedWallet = w
                                        walletExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Date display
                    val dateFormatted = remember(transactionDate) {
                        SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID")).format(Date(transactionDate))
                    }
                    Text("Tanggal Transaksi: $dateFormatted", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save button
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            Toast.makeText(context, "Silakan masukkan nominal uang yang valid!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        financeViewModel.addTransaction(
                            amount = amount,
                            type = transactionType,
                            category = selectedCategory,
                            note = noteStr.ifBlank { "Struk Belanja" },
                            date = transactionDate,
                            walletAccount = selectedWallet
                        )

                        Toast.makeText(context, "Struk berhasil disimpan ke riwayat!", Toast.LENGTH_SHORT).show()
                        onTransactionSaved?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_save_scanned_receipt"),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isProcessing && amountStr.isNotBlank()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Otomatis Masuk ke Riwayat", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
