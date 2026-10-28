package com.example.utils

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ParsedReceiptTransaction(
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val category: String,
    val walletAccount: String,
    val note: String,
    val date: Long
)

data class ChatMessage(
    val role: String, // "user" or "assistant" or "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

object OpenRouterAiService {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

    private fun normalizeBaseUrl(baseUrl: String): String {
        var url = baseUrl.trim().trimEnd('/')
        if (!url.endsWith("/v1")) {
            url = "$url/v1"
        }
        return url
    }

    /**
     * Parses raw OCR receipt text into structured financial transaction.
     */
    suspend fun parseReceiptText(
        ocrText: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): ParsedReceiptTransaction = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank()) {
            try {
                val normalizedUrl = normalizeBaseUrl(baseUrl)
                val fullUrl = "$normalizedUrl/chat/completions"

                val prompt = """
                    Kamu adalah sistem analisis bukti transaksi, nota, dan struk belanja.
                    Analisis teks hasil OCR struk berikut dan ekstrak data transaksi keuangan dalam format JSON murni tanpa markdown/backticks.
                    
                    Format JSON yang wajib dihasilkan:
                    {
                      "type": "EXPENSE" atau "INCOME",
                      "amount": nominal angka total (contoh: 45000),
                      "category": salah satu dari: "Makanan", "Transportasi", "Belanja(Shopping)", "Tagihan Rutin", "Kesehatan", "Pulsa/Data", "Langganan", "Pajak", "Cicilan/Hutang(Produktif)", "Cicilan/Hutang(Konsumtif)", "Hiburan", "E-Wallet", "Minuman", "Jajan", "Tempat Tinggal", "Lainnya",
                      "walletAccount": salah satu dari: "Tunai", "E-Wallet", "BCA", "BRI", "Danamon", "Lainnya",
                      "note": ringkasan singkat nama toko/barang/keterangan struk,
                      "dateFormatted": format "yyyy-MM-dd" jika tanggal tertera di struk, atau kosongkan "" jika tidak ada
                    }

                    Teks OCR Struk:
                    $ocrText
                """.trimIndent()

                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "Kamu adalah parser struk akurat. Jawab hanya dengan JSON tunggal.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                }

                val requestJson = JSONObject().apply {
                    put("model", model.ifBlank { "google/gemini-2.0-flash-001" })
                    put("messages", messages)
                    put("temperature", 0.1)
                }

                val request = Request.Builder()
                    .url(fullUrl)
                    .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                    .addHeader("HTTP-Referer", "https://moneyflow.local")
                    .addHeader("X-Title", "MoneyFlow Android")
                    .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val root = JSONObject(responseBody)
                    val choices = root.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val messageObj = choices.getJSONObject(0).optJSONObject("message")
                        val content = messageObj?.optString("content") ?: ""
                        val parsed = extractJsonFromResponse(content)
                        if (parsed != null) {
                            return@withContext parsed
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Robust Local Fallback Parser if API is unavailable, offline, or returns error
        return@withContext parseReceiptLocally(ocrText)
    }

    /**
     * Parses voice input text into structured transaction.
     */
    suspend fun parseVoiceText(
        speechText: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): ParsedReceiptTransaction = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank()) {
            try {
                val normalizedUrl = normalizeBaseUrl(baseUrl)
                val fullUrl = "$normalizedUrl/chat/completions"

                val prompt = """
                    Kamu adalah asisten voice over pencatat keuangan.
                    Pengguna mengucapkan kalimat transaksi keuangan berikut:
                    "${speechText}"

                    Ekstrak data transaksi ini ke dalam format JSON murni tanpa tanda markdown/backticks:
                    {
                      "type": "EXPENSE" atau "INCOME",
                      "amount": nominal angka rupiah (contoh: 50000 jika ada kata lima puluh ribu atau 50rb),
                      "category": salah satu dari: "Makanan", "Transportasi", "Belanja(Shopping)", "Tagihan Rutin", "Kesehatan", "Pulsa/Data", "Langganan", "Pajak", "Cicilan/Hutang(Produktif)", "Cicilan/Hutang(Konsumtif)", "Hiburan", "E-Wallet", "Minuman", "Jajan", "Tempat Tinggal", "Lainnya",
                      "walletAccount": salah satu dari: "Tunai", "E-Wallet", "BCA", "BRI", "Danamon", "Lainnya",
                      "note": catatan transaksi singkat yang bermakna
                    }
                """.trimIndent()

                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "Kamu adalah parser suara keuangan pintar. Jawab hanya dengan JSON tunggal.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                }

                val requestJson = JSONObject().apply {
                    put("model", model.ifBlank { "google/gemini-2.0-flash-001" })
                    put("messages", messages)
                    put("temperature", 0.1)
                }

                val request = Request.Builder()
                    .url(fullUrl)
                    .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                    .addHeader("HTTP-Referer", "https://moneyflow.local")
                    .addHeader("X-Title", "MoneyFlow Voice Over")
                    .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val root = JSONObject(responseBody)
                    val choices = root.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val messageObj = choices.getJSONObject(0).optJSONObject("message")
                        val content = messageObj?.optString("content") ?: ""
                        val parsed = extractJsonFromResponse(content)
                        if (parsed != null) {
                            return@withContext parsed
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext parseVoiceLocally(speechText)
    }

    /**
     * AI Financial Assistant chat using database & app context.
     */
    suspend fun chatWithAssistant(
        userQuery: String,
        contextData: String,
        history: List<ChatMessage>,
        baseUrl: String,
        apiKey: String,
        model: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext generateLocalAssistantResponse(userQuery, contextData)
        }

        try {
            val normalizedUrl = normalizeBaseUrl(baseUrl)
            val fullUrl = "$normalizedUrl/chat/completions"

            val systemPrompt = """
                Kamu adalah 'Asisten Keuangan MoneyFlow' cerdas, ramah, dan solutif.
                Tugasmu adalah membantu pengguna memahami kondisi keuangannya, menjawab pertanyaan berdasarkan data real aplikasi, serta memberikan saran dan insight keuangan yang bijak.

                Konteks Data Aplikasi & Database Pengguna Saat Ini:
                $contextData

                Aturan:
                1. Jawab dalam Bahasa Indonesia yang santun, jelas, dan menyemangati.
                2. Gunakan data keuangan di atas saat pengguna bertanya tentang saldo, pengeluaran, pemasukan, kategori, dompet, atau aturan 50/30/20.
                3. Format angka rupiah dengan baik (contoh: Rp 50.000).
                4. Jika ditanya hal umum seputar keuangan/budgeting, berikan tips praktis.
            """.trimIndent()

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })

                // Include last 6 conversation turns
                val recentHistory = history.takeLast(6)
                for (msg in recentHistory) {
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }

                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userQuery)
                })
            }

            val requestJson = JSONObject().apply {
                put("model", model.ifBlank { "google/gemini-2.0-flash-001" })
                put("messages", messages)
                put("temperature", 0.7)
            }

            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .addHeader("HTTP-Referer", "https://moneyflow.local")
                .addHeader("X-Title", "MoneyFlow AI Assistant")
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val root = JSONObject(responseBody)
                val choices = root.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val messageObj = choices.getJSONObject(0).optJSONObject("message")
                    val answer = messageObj?.optString("content")
                    if (!answer.isNullOrBlank()) {
                        return@withContext answer.trim()
                    }
                }
            } else {
                val errMessage = responseBody ?: "Status ${response.code}"
                return@withContext "Koneksi ke OpenRouter gagal (${response.code}). Silakan periksa kembali API Key dan Model di Pengaturan/Profil. \n\n*Pesan offline:* " +
                        generateLocalAssistantResponse(userQuery, contextData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Terjadi kendala jaringan saat menghubungi AI: ${e.localizedMessage}. \n\nBerikut ringkasan berdasarkan data lokalmu:\n" +
                    generateLocalAssistantResponse(userQuery, contextData)
        }

        return@withContext generateLocalAssistantResponse(userQuery, contextData)
    }

    private fun extractJsonFromResponse(rawContent: String): ParsedReceiptTransaction? {
        try {
            val trimmed = rawContent.trim()
            val startIdx = trimmed.indexOf('{')
            val endIdx = trimmed.lastIndexOf('}')
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                val jsonString = trimmed.substring(startIdx, endIdx + 1)
                val json = JSONObject(jsonString)

                val amount = json.optDouble("amount", 0.0)
                val type = json.optString("type", "EXPENSE").uppercase()
                val category = json.optString("category", "Belanja(Shopping)")
                val wallet = json.optString("walletAccount", "Tunai")
                val note = json.optString("note", "Struk Pembelian")
                val dateStr = json.optString("dateFormatted", "")

                var timestamp = System.currentTimeMillis()
                if (dateStr.isNotBlank()) {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val parsedDate = sdf.parse(dateStr)
                        if (parsedDate != null) {
                            timestamp = parsedDate.time
                        }
                    } catch (e: Exception) {
                        // ignore and use current time
                    }
                }

                return ParsedReceiptTransaction(
                    amount = if (amount > 0) amount else 0.0,
                    type = if (type == "INCOME") "INCOME" else "EXPENSE",
                    category = category.ifBlank { "Lainnya" },
                    walletAccount = wallet.ifBlank { "Tunai" },
                    note = note.ifBlank { "Transaksi Struk" },
                    date = timestamp
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Local parser for OCR receipt text when offline or without API key.
     */
    fun parseReceiptLocally(ocrText: String): ParsedReceiptTransaction {
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var maxAmount = 0.0
        var foundDate = System.currentTimeMillis()
        var type = "EXPENSE"
        var category = "Belanja(Shopping)"
        var wallet = "Tunai"
        var note = "Struk Pembelian"

        // Pattern for amounts like 45.000, Rp 45,000, Total: 45000, etc.
        val amountPattern = Pattern.compile("""(?:rp|total|subtotal|tagihan|bayar|tunai)?\s*[:=]?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)""", Pattern.CASE_INSENSITIVE)

        for (line in lines) {
            val lower = line.lowercase()

            // Check type
            if (lower.contains("transfer masuk") || lower.contains("penerimaan") || lower.contains("gaji") || lower.contains("topup masuk")) {
                type = "INCOME"
            }

            // Check wallet
            if (lower.contains("bca")) wallet = "BCA"
            else if (lower.contains("bri")) wallet = "BRI"
            else if (lower.contains("danamon")) wallet = "Danamon"
            else if (lower.contains("gopay") || lower.contains("ovo") || lower.contains("dana") || lower.contains("shopeepay") || lower.contains("qris")) {
                wallet = "E-Wallet"
            }

            // Check category
            if (lower.contains("resto") || lower.contains("cafe") || lower.contains("makan") || lower.contains("kopi") || lower.contains("food") || lower.contains("bakso") || lower.contains("mie")) {
                category = "Makanan"
            } else if (lower.contains("indomaret") || lower.contains("alfamart") || lower.contains("supermarket") || lower.contains("mart")) {
                category = "Belanja(Shopping)"
            } else if (lower.contains("pln") || lower.contains("listrik") || lower.contains("pdam") || lower.contains("wifi") || lower.contains("indihome")) {
                category = "Tagihan Rutin"
            } else if (lower.contains("spbu") || lower.contains("pertamina") || lower.contains("bensin") || lower.contains("pertalite") || lower.contains("pertamax") || lower.contains("grab") || lower.contains("gojek")) {
                category = "Transportasi"
            } else if (lower.contains("apotek") || lower.contains("obat") || lower.contains("klinik") || lower.contains("dokter")) {
                category = "Kesehatan"
            }

            // Check amount
            val matcher = amountPattern.matcher(line)
            while (matcher.find()) {
                val numStr = matcher.group(1)?.replace(".", "")?.replace(",", "")
                val parsed = numStr?.toDoubleOrNull() ?: 0.0
                if (parsed > maxAmount && parsed < 1000000000) {
                    maxAmount = parsed
                }
            }
        }

        if (lines.isNotEmpty()) {
            note = lines.firstOrNull { it.length in 4..30 } ?: "Struk Belanja"
        }

        return ParsedReceiptTransaction(
            amount = maxAmount,
            type = type,
            category = category,
            walletAccount = wallet,
            note = note,
            date = foundDate
        )
    }

    /**
     * Local parser for voice input.
     */
    fun parseVoiceLocally(speechText: String): ParsedReceiptTransaction {
        val lower = speechText.lowercase().trim()
        var type = if (lower.contains("masuk") || lower.contains("gaji") || lower.contains("dapat uang") || lower.contains("terima") || lower.contains("transfer dari")) "INCOME" else "EXPENSE"

        var wallet = "Tunai"
        if (lower.contains("bca")) wallet = "BCA"
        else if (lower.contains("bri")) wallet = "BRI"
        else if (lower.contains("danamon")) wallet = "Danamon"
        else if (lower.contains("gopay") || lower.contains("ovo") || lower.contains("dana") || lower.contains("ewallet") || lower.contains("e-wallet") || lower.contains("shopeepay")) wallet = "E-Wallet"

        var category = "Lainnya"
        if (lower.contains("makan") || lower.contains("kopi") || lower.contains("nasi") || lower.contains("jajan") || lower.contains("resto") || lower.contains("sarapan")) category = "Makanan"
        else if (lower.contains("bensin") || lower.contains("parkir") || lower.contains("gojek") || lower.contains("grab") || lower.contains("transportasi")) category = "Transportasi"
        else if (lower.contains("belanja") || lower.contains("baju") || lower.contains("shopping") || lower.contains("toko")) category = "Belanja(Shopping)"
        else if (lower.contains("listrik") || lower.contains("pulsa") || lower.contains("kuota") || lower.contains("data") || lower.contains("wifi") || lower.contains("air")) category = "Tagihan Rutin"
        else if (lower.contains("obat") || lower.contains("dokter") || lower.contains("rumah sakit")) category = "Kesehatan"
        else if (lower.contains("gaji") || lower.contains("bonus") || lower.contains("penjualan")) category = "Lainnya"

        // Extract number like 50000, 50 ribu, 50rb, 1 juta
        var amount = 0.0
        val regexK = Regex("""(\d+)\s*(?:rb|ribu)""")
        val matchK = regexK.find(lower)
        if (matchK != null) {
            val num = matchK.groupValues[1].toDoubleOrNull() ?: 0.0
            amount = num * 1000
        } else {
            val regexJuta = Regex("""(\d+(?:[.,]\d+)?)\s*(?:jt|juta)""")
            val matchJuta = regexJuta.find(lower)
            if (matchJuta != null) {
                val num = matchJuta.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
                amount = num * 1000000
            } else {
                val regexDigits = Regex("""\b(\d{3,10})\b""")
                val matchDigits = regexDigits.find(lower)
                if (matchDigits != null) {
                    amount = matchDigits.groupValues[1].toDoubleOrNull() ?: 0.0
                }
            }
        }

        val note = if (speechText.length > 50) speechText.take(50) else speechText

        return ParsedReceiptTransaction(
            amount = amount,
            type = type,
            category = category,
            walletAccount = wallet,
            note = note.ifBlank { "Input Suara" },
            date = System.currentTimeMillis()
        )
    }

    private fun generateLocalAssistantResponse(query: String, contextData: String): String {
        val q = query.lowercase()
        return when {
            q.contains("saldo") -> {
                "Berdasarkan catatan aplikasi:\n$contextData\n\nPastikan selalu memantau alokasi saldo pada dompet dan rekening utama Anda."
            }
            q.contains("50/30/20") || q.contains("aturan") || q.contains("sehat") -> {
                "Menurut metode keuangan 50/30/20:\n- **Kebutuhan (50%)**: Makanan pokok, tagihan rutin, transportasi, tempat tinggal.\n- **Keinginan (30%)**: Hiburan, belanja pakaian, nongkrong santai.\n- **Tabungan & Investasi (20%)**: Dana darurat, aset emas, tabungan masa depan.\n\nSimak grafik pada tab Insight untuk melihat apakah pengeluaranmu sudah seimbang."
            }
            q.contains("boros") || q.contains("terbesar") || q.contains("pengeluaran") -> {
                "Berikut rangkuman keuangan terkini dari aplikasi:\n$contextData\n\nTips: Periksa kategori pengeluaran terbesar dan coba kurangi pengeluaran impulsif selama 30 hari ke depan."
            }
            else -> {
                "Halo! Saya Asisten Keuangan MoneyFlow. Berikut data keuangan terkini Anda:\n$contextData\n\nAda yang ingin Anda tanyakan seputar saldo, anggaran 50/30/20, atau riwayat transaksi?"
            }
        }
    }
}
