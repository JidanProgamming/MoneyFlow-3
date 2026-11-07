package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
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

    // Recording and audio states
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableStateOf(0) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    var audioDurationMs by remember { mutableStateOf(0) }
    var audioCurrentMs by remember { mutableStateOf(0) }

    // MediaRecorder & MediaPlayer instances
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // SpeechRecognizer
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var transcribedText by remember { mutableStateOf("") }
    var isProcessingAi by remember { mutableStateOf(false) }

    // Parsed editable fields
    var parsedAmount by remember { mutableStateOf("") }
    var parsedType by remember { mutableStateOf("EXPENSE") }
    var parsedCategory by remember { mutableStateOf("Makanan") }
    var parsedWallet by remember { mutableStateOf("Tunai") }
    var parsedNote by remember { mutableStateOf("") }
    var parsedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var hasResult by remember { mutableStateOf(false) }

    val categories = listOf(
        "Makanan", "Belanja(Shopping)", "Transportasi", "Tagihan Rutin",
        "Kesehatan", "Pulsa/Data", "Langganan", "Pajak", "Cicilan/Hutang(Produktif)",
        "Cicilan/Hutang(Konsumtif)", "Hiburan", "E-Wallet", "Minuman", "Jajan",
        "Tempat Tinggal", "Lainnya"
    )

    val wallets = listOf("Tunai", "E-Wallet", "BCA", "BRI", "Danamon", "Lainnya")

    // Timer for recording duration
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSec = 0
            while (isRecording) {
                delay(1000)
                recordingDurationSec += 1
            }
        }
    }

    // Playback progress loop
    LaunchedEffect(isPlayingAudio) {
        if (isPlayingAudio) {
            while (isPlayingAudio) {
                try {
                    audioCurrentMs = mediaPlayer?.currentPosition ?: 0
                } catch (e: Exception) {
                    isPlayingAudio = false
                }
                delay(200)
            }
        }
    }

    // Cleanup audio resources on dispose
    DisposableEffect(Unit) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        onDispose {
            try {
                if (isRecording) {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                }
            } catch (e: Exception) {
                // ignore
            }
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                // ignore
            }
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    // AI Analysis function
    fun runAiAnalysis(text: String) {
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
            parsedDate = parsed.date
            hasResult = true
            isProcessingAi = false
        }
    }

    // Stop recording and process
    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        speechRecognizer?.stopListening()

        // Prepare player if audio file exists
        audioFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                try {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        prepare()
                        audioDurationMs = duration
                        setOnCompletionListener {
                            isPlayingAudio = false
                            audioCurrentMs = 0
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Run AI parsing
        if (transcribedText.isNotBlank()) {
            runAiAnalysis(transcribedText)
        }
    }

    // Start recording function
    fun startRecording() {
        try {
            // Setup local audio recording file
            val file = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
            audioFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
            hasResult = false
            transcribedText = ""

            // Start live speech recognizer
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {}
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            transcribedText = text
                            runAiAnalysis(text)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            transcribedText = matches[0]
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer?.startListening(intent)
            }
        } catch (e: Exception) {
            isRecording = false
            Toast.makeText(context, "Gagal memulai rekaman suara: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(context, "Izin mikrofon diperlukan untuk merekam suara.", Toast.LENGTH_SHORT).show()
        }
    }

    // Pulsating animation for recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.22f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = {
            if (!isProcessingAi && !isRecording) {
                mediaPlayer?.stop()
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(26.dp))
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0066FF).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Voice Note Keuangan",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Rekam, Putar Ulang, & Analisis AI",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (!isRecording) {
                                mediaPlayer?.stop()
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Record / Stop Control Section
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isRecording) Color(0xFFFFF1F2) else Color(0xFFF0F7FF),
                        border = BorderStroke(1.dp, if (isRecording) SoftRedDanger.copy(alpha = 0.4f) else Color(0xFF0066FF).copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Circular Mic Button
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(
                                        if (isRecording) SoftRedDanger else Color(0xFF0066FF)
                                    )
                                    .clickable {
                                        if (isRecording) {
                                            stopRecording()
                                        } else {
                                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) {
                                                startRecording()
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }
                                    .testTag("btn_mic_record_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "Hentikan Rekaman" else "Mulai Rekam",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (isRecording) "Merekam Suara: ${String.format(Locale.getDefault(), "%02d:%02d", recordingDurationSec / 60, recordingDurationSec % 60)}"
                                else if (audioFile != null) "Rekaman Selesai (${String.format(Locale.getDefault(), "%02d:%02d", (audioDurationMs / 1000) / 60, (audioDurationMs / 1000) % 60)})"
                                else "Tekan tombol untuk mulai merekam",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) SoftRedDanger else Color(0xFF0066FF)
                            )

                            Text(
                                text = if (isRecording) "Tekan ikon Stop untuk mengakhiri rekaman"
                                else "Sebutkan contoh: 'Beli bensin 50 ribu pakai tunai' atau 'Gaji masuk 5 juta'",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )

                            // Playback Section (Putar Ulang Rekaman)
                            if (audioFile != null && !isRecording) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color(0xFF0066FF).copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Play / Pause Button
                                    FilledTonalIconButton(
                                        onClick = {
                                            try {
                                                if (isPlayingAudio) {
                                                    mediaPlayer?.pause()
                                                    isPlayingAudio = false
                                                } else {
                                                    mediaPlayer?.start()
                                                    isPlayingAudio = true
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlayingAudio) "Jeda" else "Putar Ulang",
                                            tint = Color(0xFF0066FF)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isPlayingAudio) "Sedang Memutar Ulang..." else "Putar Ulang Rekaman Suara",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0066FF)
                                        )
                                        LinearProgressIndicator(
                                            progress = {
                                                if (audioDurationMs > 0) (audioCurrentMs.toFloat() / audioDurationMs.toFloat()).coerceIn(0f, 1f)
                                                else 0f
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = Color(0xFF0066FF),
                                            trackColor = Color(0xFFE2E8F0)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Re-record button
                                    OutlinedButton(
                                        onClick = {
                                            mediaPlayer?.stop()
                                            audioFile = null
                                            hasResult = false
                                            transcribedText = ""
                                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) startRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ulang", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Speech Transcribed Text or Manual Input Field
                    OutlinedTextField(
                        value = transcribedText,
                        onValueChange = {
                            transcribedText = it
                            if (it.length > 5) runAiAnalysis(it)
                        },
                        label = { Text("Teks Suara / Kalimat Transaksi") },
                        placeholder = { Text("Ucapkan atau ketik transaksi Anda...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_transcribed_input"),
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            if (transcribedText.isNotBlank()) {
                                IconButton(onClick = { runAiAnalysis(transcribedText) }) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Analisis AI", tint = Color(0xFF0066FF))
                                }
                            }
                        }
                    )

                    // Loading State for AI parsing
                    if (isProcessingAi) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp, color = Color(0xFF0066FF))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("AI sedang menganalisis nominal, kategori, dan tanggal...", fontSize = 12.sp, color = Color(0xFF0066FF), fontWeight = FontWeight.Medium)
                        }
                    }

                    // Editable Results Form (User can edit before saving)
                    if (hasResult) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hasil Analisis AI (Dapat Diedit)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (parsedType == "INCOME") SoftGreenSuccess.copy(alpha = 0.15f) else SoftRedDanger.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (parsedType == "INCOME") "Pemasukan" else "Pengeluaran",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (parsedType == "INCOME") SoftGreenSuccess else SoftRedDanger,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Type Toggle (Pemasukan / Pengeluaran)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = parsedType == "EXPENSE",
                                        onClick = { parsedType = "EXPENSE" },
                                        label = { Text("Pengeluaran") },
                                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = parsedType == "INCOME",
                                        onClick = { parsedType = "INCOME" },
                                        label = { Text("Pemasukan") },
                                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Nominal Input Field
                                OutlinedTextField(
                                    value = parsedAmount,
                                    onValueChange = { parsedAmount = it.filter { char -> char.isDigit() } },
                                    label = { Text("Nominal Uang (Rp)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    supportingText = {
                                        val amt = parsedAmount.toDoubleOrNull() ?: 0.0
                                        Text(FormatUtils.formatRupiah(amt), fontWeight = FontWeight.Bold, color = Color(0xFF0066FF))
                                    }
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Category Dropdown Picker
                                var categoryMenuExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = categoryMenuExpanded,
                                    onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = parsedCategory,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Kategori") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = categoryMenuExpanded,
                                        onDismissRequest = { categoryMenuExpanded = false }
                                    ) {
                                        categories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat) },
                                                onClick = {
                                                    parsedCategory = cat
                                                    categoryMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Wallet Dropdown Picker
                                var walletMenuExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = walletMenuExpanded,
                                    onExpandedChange = { walletMenuExpanded = !walletMenuExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = parsedWallet,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Sumber Dompet / Rekening") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletMenuExpanded) },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = walletMenuExpanded,
                                        onDismissRequest = { walletMenuExpanded = false }
                                    ) {
                                        wallets.forEach { w ->
                                            DropdownMenuItem(
                                                text = { Text(w) },
                                                onClick = {
                                                    parsedWallet = w
                                                    walletMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Note / Description Field
                                OutlinedTextField(
                                    value = parsedNote,
                                    onValueChange = { parsedNote = it },
                                    label = { Text("Catatan / Keterangan Transaksi") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Button: Save to Riwayat
                Button(
                    onClick = {
                        val amount = parsedAmount.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            Toast.makeText(context, "Silakan isi atau ucapkan nominal uang!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Save transaction with source tag [Suara]
                        financeViewModel.addTransaction(
                            amount = amount,
                            type = parsedType,
                            category = parsedCategory,
                            note = "[Suara] ${parsedNote.ifBlank { "Transaksi Suara" }}",
                            date = parsedDate,
                            walletAccount = parsedWallet
                        )

                        Toast.makeText(context, "Transaksi berhasil disimpan ke Riwayat!", Toast.LENGTH_SHORT).show()
                        onTransactionSaved?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_confirm_save_voice_transaction"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                    enabled = parsedAmount.isNotBlank() && !isProcessingAi && !isRecording
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konfirmasi & Simpan ke Riwayat", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
