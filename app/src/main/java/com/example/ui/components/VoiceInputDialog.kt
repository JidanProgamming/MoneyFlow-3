package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.SoftGreenSuccess
import com.example.ui.theme.SoftRedDanger
import com.example.utils.FormatUtils
import com.example.utils.OpenRouterAiService
import com.example.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputDialog(
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

    var isListening by remember { mutableStateOf(false) }
    var recognizedSpokenText by remember { mutableStateOf("") }
    var manualTextInput by remember { mutableStateOf("") }
    var isProcessingAi by remember { mutableStateOf(false) }

    // Parsed transaction fields
    var parsedAmount by remember { mutableStateOf("") }
    var parsedType by remember { mutableStateOf("EXPENSE") }
    var parsedCategory by remember { mutableStateOf("Makanan") }
    var parsedWallet by remember { mutableStateOf("Tunai") }
    var parsedNote by remember { mutableStateOf("") }
    var hasResult by remember { mutableStateOf(false) }

    val categories = listOf(
        "Makanan", "Belanja(Shopping)", "Transportasi", "Tagihan Rutin",
        "Kesehatan", "Pulsa/Data", "Langganan", "Pajak", "Cicilan/Hutang(Produktif)",
        "Cicilan/Hutang(Konsumtif)", "Hiburan", "E-Wallet", "Minuman", "Jajan",
        "Tempat Tinggal", "Lainnya"
    )

    val wallets = listOf("Tunai", "E-Wallet", "BCA", "BRI", "Danamon", "Lainnya")

    // SpeechRecognizer instance
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(Unit) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        onDispose {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    // Process spoken or typed text with AI
    fun processVoiceTextWithAi(text: String) {
        if (text.isBlank()) return
        isProcessingAi = true
        coroutineScope.launch {
            val parsed = OpenRouterAiService.parseVoiceText(
                speechText = text,
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model
            )

            parsedAmount = if (parsed.amount > 0) String.format(Locale.US, "%.0f", parsed.amount) else ""
            parsedType = parsed.type
            parsedCategory = parsed.category
            parsedWallet = parsed.walletAccount
            parsedNote = parsed.note
            hasResult = true
            isProcessingAi = false
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            Toast.makeText(context, "Speech recognition tidak tersedia di perangkat ini. Silakan ketik kalimat transaksi.", Toast.LENGTH_SHORT).show()
            return
        }

        recognizedSpokenText = ""
        isListening = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicarakan transaksi keuangan Anda...")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    recognizedSpokenText = text
                    manualTextInput = text
                    processVoiceTextWithAi(text)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    recognizedSpokenText = matches[0]
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(context, "Tidak dapat memulai pengenalan suara: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(context, "Izin mikrofon diperlukan untuk fitur voice over.", Toast.LENGTH_SHORT).show()
        }
    }

    fun onMicClicked() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                startListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Pulse animation for recording microphone
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = {
            if (!isProcessingAi) {
                speechRecognizer?.stopListening()
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("voice_input_dialog"),
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
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Voice Over Keuangan",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Catat Transaksi dengan Suara",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isProcessingAi
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Microphone pulse button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { onMicClicked() },
                        modifier = Modifier
                            .size(72.dp)
                            .scale(if (isListening) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(if (isListening) SoftRedDanger else MaterialTheme.colorScheme.primary)
                            .testTag("btn_voice_mic")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Bicara",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = if (isListening) "Mendengarkan... Bicaralah sekarang..." else "Tekan mikrofon & sebutkan transaksi (contoh: 'Makan siang 25000 pakai tunai')",
                    fontSize = 12.sp,
                    color = if (isListening) SoftRedDanger else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isListening) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Manual text entry / voice recognition result preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualTextInput,
                        onValueChange = { manualTextInput = it },
                        placeholder = { Text("Atau ketik kalimat suara di sini...") },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { processVoiceTextWithAi(manualTextInput) },
                        modifier = Modifier.height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = manualTextInput.isNotBlank() && !isProcessingAi
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Kirim ke AI")
                    }
                }

                if (isProcessingAi) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI sedang memproses ucapan...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Form content with parsed results
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Type Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = parsedType == "EXPENSE",
                            onClick = { parsedType = "EXPENSE" },
                            label = { Text("Pengeluaran") },
                            leadingIcon = {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = SoftRedDanger, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = parsedType == "INCOME",
                            onClick = { parsedType = "INCOME" },
                            label = { Text("Pemasukan") },
                            leadingIcon = {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = SoftGreenSuccess, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Amount Field
                    OutlinedTextField(
                        value = parsedAmount,
                        onValueChange = { parsedAmount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Nominal Uang (Rp)") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        leadingIcon = {
                            Text("Rp", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Note
                    OutlinedTextField(
                        value = parsedNote,
                        onValueChange = { parsedNote = it },
                        label = { Text("Catatan Transaksi") },
                        placeholder = { Text("Catatan singkat") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selector
                    Text("Kategori:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var catExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = catExpanded,
                        onExpandedChange = { catExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = parsedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        parsedCategory = cat
                                        catExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Wallet Selector
                    Text("Dari / Ke Dompet:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var wExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = wExpanded,
                        onExpandedChange = { wExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = parsedWallet,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = wExpanded,
                            onDismissRequest = { wExpanded = false }
                        ) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w) },
                                    onClick = {
                                        parsedWallet = w
                                        wExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto Save to History button
                Button(
                    onClick = {
                        val amount = parsedAmount.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            Toast.makeText(context, "Silakan sebutkan atau isi nominal uang!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        financeViewModel.addTransaction(
                            amount = amount,
                            type = parsedType,
                            category = parsedCategory,
                            note = parsedNote.ifBlank { "Input Suara Voice Over" },
                            date = System.currentTimeMillis(),
                            walletAccount = parsedWallet
                        )

                        Toast.makeText(context, "Transaksi suara berhasil masuk ke riwayat!", Toast.LENGTH_SHORT).show()
                        onTransactionSaved?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_save_voice_transaction"),
                    shape = RoundedCornerShape(14.dp),
                    enabled = parsedAmount.isNotBlank() && !isProcessingAi
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Otomatis Masuk ke Riwayat", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
