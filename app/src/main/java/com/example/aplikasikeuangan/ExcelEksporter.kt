package com.example.aplikasikeuangan

import android.content.Context
import android.os.Environment
import android.widget.Toast
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class Transaction(
    val tanggal: String,
    val tipe: String,
    val kategori: String,
    val nominal: Double,
    val catatan: String
)

// Extension properties for seamless Kotlin property access with Apache POI
var Cell.value: Any?
    get() = when (cellType) {
        CellType.NUMERIC -> numericCellValue
        CellType.BOOLEAN -> booleanCellValue
        else -> stringCellValue
    }
    set(v) {
        when (v) {
            is Number -> setCellValue(v.toDouble())
            is String -> setCellValue(v)
            is Boolean -> setCellValue(v)
            is Date -> setCellValue(v)
            else -> setCellValue(v?.toString() ?: "")
        }
    }

var Cell.alignment: HorizontalAlignment?
    get() = cellStyle?.alignment
    set(v) {
        if (v != null) {
            cellStyle?.alignment = v
        }
    }

var Font.fontHeightInPoints: Int
    get() = this.fontHeightInPoints.toInt()
    set(v) { this.setFontHeightInPoints(v.toShort()) }

object ExcelExporter {

    fun exportToXlsx(
        context: Context,
        transactions: List<Transaction>
    ): File? {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Laporan Keuangan")

            // =========================
            // FONT
            // =========================

            val titleFont = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 16
            }

            val headerFont = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 11
            }

            val normalFont = workbook.createFont().apply {
                fontHeightInPoints = 10
            }

            // =========================
            // STYLE JUDUL
            // =========================

            val titleStyle = workbook.createCellStyle().apply {
                setFont(titleFont)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
            }

            // =========================
            // STYLE HEADER
            // =========================

            val headerStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER

                borderTop = BorderStyle.THIN
                borderBottom = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
            }

            // =========================
            // STYLE DATA
            // =========================

            val dataStyle = workbook.createCellStyle().apply {
                setFont(normalFont)
                verticalAlignment = VerticalAlignment.CENTER

                borderTop = BorderStyle.THIN
                borderBottom = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
            }

            // =========================
            // STYLE NOMINAL
            // =========================

            val nominalStyle = workbook.createCellStyle().apply {
                setFont(normalFont)
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER

                borderTop = BorderStyle.THIN
                borderBottom = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN

                dataFormat = workbook.createDataFormat()
                    .getFormat("\"Rp\" #,##0")
            }

            // =========================
            // JUDUL
            // =========================

            sheet.addMergedRegion(
                org.apache.poi.ss.util.CellRangeAddress(
                    0,
                    0,
                    0,
                    5
                )
            )

            val titleRow = sheet.createRow(0)
            titleRow.heightInPoints = 25f

            val titleCell = titleRow.createCell(0)
            titleCell.value = "LAPORAN KEUANGAN"
            titleCell.cellStyle = titleStyle

            // =========================
            // SUBTITLE
            // =========================

            sheet.addMergedRegion(
                org.apache.poi.ss.util.CellRangeAddress(
                    1,
                    1,
                    0,
                    5
                )
            )

            val subtitleRow = sheet.createRow(1)

            val subtitleCell = subtitleRow.createCell(0)
            subtitleCell.value =
                "Daftar transaksi pemasukan dan pengeluaran"
            subtitleCell.cellStyle = titleStyle

            // =========================
            // HEADER
            // =========================

            val headerRow = sheet.createRow(3)

            val headers = arrayOf(
                "No",
                "Tanggal",
                "Tipe",
                "Kategori",
                "Nominal",
                "Catatan"
            )

            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.value = header
                cell.cellStyle = headerStyle
            }

            // =========================
            // DATA TRANSAKSI
            // =========================

            transactions.forEachIndexed { index, transaction ->

                val row = sheet.createRow(index + 4)

                // No
                row.createCell(0).apply {
                    value = (index + 1).toDouble()
                    cellStyle = dataStyle
                    alignment = HorizontalAlignment.CENTER
                }

                // Tanggal
                row.createCell(1).apply {
                    value = transaction.tanggal
                    cellStyle = dataStyle
                    alignment = HorizontalAlignment.CENTER
                }

                // Tipe
                row.createCell(2).apply {
                    value = transaction.tipe
                    cellStyle = dataStyle
                    alignment = HorizontalAlignment.CENTER
                }

                // Kategori
                row.createCell(3).apply {
                    value = transaction.kategori
                    cellStyle = dataStyle
                }

                // Nominal
                row.createCell(4).apply {
                    value = transaction.nominal
                    cellStyle = nominalStyle
                }

                // Catatan
                row.createCell(5).apply {
                    value = transaction.catatan
                    cellStyle = dataStyle
                }
            }

            // =========================
            // LEBAR KOLOM
            // =========================

            sheet.setColumnWidth(0, 8 * 256)
            sheet.setColumnWidth(1, 16 * 256)
            sheet.setColumnWidth(2, 16 * 256)
            sheet.setColumnWidth(3, 22 * 256)
            sheet.setColumnWidth(4, 20 * 256)
            sheet.setColumnWidth(5, 40 * 256)

            // =========================
            // FILTER
            // =========================

            if (transactions.isNotEmpty()) {
                sheet.setAutoFilter(
                    org.apache.poi.ss.util.CellRangeAddress(
                        3,
                        transactions.size + 3,
                        0,
                        5
                    )
                )
            }

            // Freeze header
            sheet.createFreezePane(0, 4)

            // =========================
            // SIMPAN FILE
            // =========================

            val tanggal =
                SimpleDateFormat(
                    "dd-MM-yyyy_HH-mm",
                    Locale.getDefault()
                ).format(Date())

            val fileName =
                "Laporan_Keuangan_$tanggal.xlsx"

            val directory = File(
                context.getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS
                ),
                "Laporan Keuangan"
            )

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(directory, fileName)

            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }

            workbook.close()

            Toast.makeText(
                context,
                "File berhasil diekspor:\n$fileName",
                Toast.LENGTH_LONG
            ).show()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Gagal mengekspor Excel: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
            return null
        }
    }

    /**
     * Helper function to export Room database transaction entities directly
     */
    fun exportDatabaseTransactions(
        context: Context,
        dbTransactions: List<com.example.data.model.Transaction>
    ): File? {
        val mappedList = dbTransactions.map { t ->
            Transaction(
                tanggal = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(t.date)),
                tipe = if (t.type == "INCOME") "Pemasukan" else "Pengeluaran",
                kategori = t.category,
                nominal = t.amount,
                catatan = t.note
            )
        }
        return exportToXlsx(context, mappedList)
    }
}
