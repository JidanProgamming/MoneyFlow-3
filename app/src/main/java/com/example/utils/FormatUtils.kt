package com.example.utils

import java.text.NumberFormat
import java.util.Locale

object FormatUtils {
    fun formatRupiah(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            val formatted = format.format(amount)
            // Trim decimal cents if any and ensure space after Rp
            formatted.replace("Rp", "Rp ").substringBefore(",")
        } catch (e: Exception) {
            "Rp " + String.format("%,.0f", amount)
        }
    }
}
