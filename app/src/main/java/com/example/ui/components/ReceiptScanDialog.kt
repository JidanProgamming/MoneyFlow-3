package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Transaction
import com.example.ui.theme.SoftGreenSuccess
import com.example.ui.theme.SoftRedDanger
import com.example.utils.FormatUtils
import com.example.utils.OpenRouterAiService
import com.example.utils.ScannedReceiptItem
import com.example.viewmodel.FinanceViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
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

    // Multiple Images state
    val selectedBitmaps = remember { mutableStateListOf<Bitmap>() }
    var isProcessing by remember { mutableStateOf(false) }
    var processingStep by remember { mutableStateOf("") }

    // Extracted items list
    val extractedItems = remember { mutableStateListOf<ScannedReceiptItem>() }

    val categories = listOf(
        "Makanan", "Belanja(Shopping)", "Transportasi", "Tagihan Rutin",
        "Kesehatan", "Pulsa/Data", "Langganan", "Pajak", "Cicilan/Hutang(Produktif)",
        "Cicilan/Hutang(Konsumtif)", "Hiburan", "E-Wallet", "Minuman", "Jajan",
        "Tempat Tinggal", "Lainnya"
    )

    val wallets = listOf("Tunai", "E-Wallet", "BCA", "BRI", "Danamon", "Lainnya")

    // OCR & AI extraction for all selected bitmaps
    fun processAllBitmaps() {
        if (selectedBitmaps.isEmpty()) {
            Toast.makeText(context, "Silakan ambil foto atau pilih gambar struk terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessing = true
        processingStep = "Membaca teks dari ${selectedBitmaps.size} struk..."

        coroutineScope.launch {
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val ocrTexts = mutableListOf<String>()

                for ((index, bitmap) in selectedBitmaps.withIndex()) {
                    processingStep = "Membaca OCR struk ${index + 1} dari ${selectedBitmaps.size}..."
                    val image = InputImage.fromBitmap(bitmap, 0)
                    try {
                        val text = suspendCancellableCoroutine<String> { cont ->
                            recognizer.process(image)
                                .addOnSuccessListener { visionText ->
                                    if (cont.isActive) cont.resume(visionText.text)
                                }
                                .addOnFailureListener {
                                    if (cont.isActive) cont.resume("")
                                }
                        }
                        if (text.isNotBlank()) {
                            ocrTexts.add(text)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (ocrTexts.isEmpty()) {
                    isProcessing = false
                    processingStep = ""
                    Toast.makeText(context, "Tidak ada teks yang terdeteksi pada struk.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                processingStep = "Menganalisis nama barang & nominal dengan AI..."
                val items = OpenRouterAiService.parseMultipleReceipts(
                    ocrTexts = ocrTexts,
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    model = model
                )

                extractedItems.clear()
                if (items.isNotEmpty()) {
                    extractedItems.addAll(items)
                    Toast.makeText(context, "Ditemukan ${items.size} item transaksi!", Toast.LENGTH_SHORT).show()
                } else {
                    // Fallback create 1 manual placeholder item
                    extractedItems.add(
                        ScannedReceiptItem(
                            name = "Struk Pembelian",
                            amount = 0.0,
                            category = "Belanja(Shopping)",
                            walletAccount = "Tunai",
                            date = System.currentTimeMillis()
                        )
                    )
                    Toast.makeText(context, "Struk diproses, silakan periksa nominalnya.", Toast.LENGTH_SHORT).show()
                }

                isProcessing = false
                processingStep = ""
            } catch (e: Exception) {
                isProcessing = false
                processingStep = ""
                Toast.makeText(context, "Gagal memproses struk: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedBitmaps.add(bitmap)
            processAllBitmaps()
        }
    }

    // Camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membuka kamera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk memotret struk.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membuka kamera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Multiple Photo Picker Launcher
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
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
                    selectedBitmaps.add(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (selectedBitmaps.isNotEmpty()) {
                processAllBitmaps()
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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("receipt_scan_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0066FF).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pindai Struk (Multi-Struk)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kamera & Unggah Beberapa Gambar Sekaligus",
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

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons to pick Camera or Multiple Files
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { openCamera() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_capture_camera"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Foto Struk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            multiplePhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_pick_gallery_multiple"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pilih Banyak File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal list of uploaded images / thumbnails
                if (selectedBitmaps.isNotEmpty()) {
                    Text(
                        text = "Struk Terpilih (${selectedBitmaps.size} Foto):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(selectedBitmaps) { idx, bmp ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF0066FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Struk ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Delete badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .clickable {
                                            selectedBitmaps.removeAt(idx)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Processing indicator
                if (isProcessing) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF0066FF),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = processingStep,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0066FF)
                            )
                        }
                    }
                }

                // Scrollable Form content with Extracted Items Preview and Edit
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedBitmaps.isEmpty() && extractedItems.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFF0066FF)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Belum Ada Struk yang Dipilih",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Ambil foto atau pilih beberapa file nota/struk sekaligus untuk mulai memindai.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (extractedItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daftar Item Hasil Pindai (${extractedItems.size} Item):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(
                                onClick = {
                                    extractedItems.add(
                                        ScannedReceiptItem(
                                            name = "Item Baru",
                                            amount = 0.0,
                                            category = "Belanja(Shopping)",
                                            walletAccount = "Tunai",
                                            date = System.currentTimeMillis()
                                        )
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Tambah Item", fontSize = 12.sp)
                            }
                        }

                        extractedItems.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Item #${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0066FF)
                                        )
                                        IconButton(
                                            onClick = { extractedItems.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Item",
                                                tint = SoftRedDanger,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Item Name
                                    OutlinedTextField(
                                        value = item.name,
                                        onValueChange = { newName ->
                                            extractedItems[index] = item.copy(name = newName)
                                        },
                                        label = { Text("Nama Barang / Pengeluaran") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Item Amount
                                    var rawAmountStr by remember(item.amount) {
                                        mutableStateOf(if (item.amount > 0) String.format(Locale.US, "%.0f", item.amount) else "")
                                    }
                                    OutlinedTextField(
                                        value = rawAmountStr,
                                        onValueChange = { newStr ->
                                            val digits = newStr.filter { it.isDigit() }
                                            rawAmountStr = digits
                                            val amt = digits.toDoubleOrNull() ?: 0.0
                                            extractedItems[index] = item.copy(amount = amt)
                                        },
                                        label = { Text("Nominal Uang (Rp)") },
                                        leadingIcon = { Text("Rp", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Category & Wallet Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Category
                                        var catExpanded by remember { mutableStateOf(false) }
                                        ExposedDropdownMenuBox(
                                            expanded = catExpanded,
                                            onExpandedChange = { catExpanded = it },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            OutlinedTextField(
                                                value = item.category,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Kategori", fontSize = 11.sp) },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .menuAnchor(),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = catExpanded,
                                                onDismissRequest = { catExpanded = false }
                                            ) {
                                                categories.forEach { c ->
                                                    DropdownMenuItem(
                                                        text = { Text(c, fontSize = 12.sp) },
                                                        onClick = {
                                                            extractedItems[index] = item.copy(category = c)
                                                            catExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Wallet
                                        var walExpanded by remember { mutableStateOf(false) }
                                        ExposedDropdownMenuBox(
                                            expanded = walExpanded,
                                            onExpandedChange = { walExpanded = it },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            OutlinedTextField(
                                                value = item.walletAccount,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Dompet", fontSize = 11.sp) },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walExpanded) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .menuAnchor(),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = walExpanded,
                                                onDismissRequest = { walExpanded = false }
                                            ) {
                                                wallets.forEach { w ->
                                                    DropdownMenuItem(
                                                        text = { Text(w, fontSize = 12.sp) },
                                                        onClick = {
                                                            extractedItems[index] = item.copy(walletAccount = w)
                                                            walExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Summary Total
                        val totalSum = extractedItems.sumOf { it.amount }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total ${extractedItems.size} Transaksi:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0066FF))
                                Text(FormatUtils.formatRupiah(totalSum), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0066FF))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Save Action
                Button(
                    onClick = {
                        val validItems = extractedItems.filter { it.amount > 0 }
                        if (validItems.isEmpty()) {
                            Toast.makeText(context, "Silakan masukkan minimal satu item dengan nominal yang valid!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Convert to separate Transaction objects with [Struk] source tag
                        val transactionsToSave = validItems.map { item ->
                            Transaction(
                                amount = item.amount,
                                type = item.type,
                                category = item.category,
                                note = "[Struk] ${item.name.ifBlank { "Belanja Struk" }}",
                                date = item.date,
                                walletAccount = item.walletAccount
                            )
                        }

                        financeViewModel.addMultipleTransactions(transactionsToSave)
                        Toast.makeText(context, "${transactionsToSave.size} transaksi struk otomatis masuk ke Riwayat!", Toast.LENGTH_SHORT).show()
                        onTransactionSaved?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_save_scanned_receipt"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                    enabled = !isProcessing && extractedItems.any { it.amount > 0 }
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konfirmasi & Simpan Semua ke Riwayat", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
