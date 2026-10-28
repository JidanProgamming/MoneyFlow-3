package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.Transaction
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportUtils {

    /**
     * Generates a fully compliant modern Excel (.xlsx) file as bytes
     * without any heavy external dependencies, allowing safe, offline usage.
     */
    fun exportToXlsx(context: Context, transactions: List<Transaction>): File? {
        try {
            val headers = listOf("No", "Tanggal", "Tipe", "Kategori", "Nominal", "Catatan")
            val rows = transactions.mapIndexed { index, t ->
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(t.date))
                listOf(
                    (index + 1).toDouble(),
                    dateStr,
                    t.type,
                    t.category,
                    t.amount,
                    t.note
                )
            }

            val file = File(context.cacheDir, "MoneyFlow_Laporan_Transaksi.xlsx")
            val fos = FileOutputStream(file)
            val xlsxBytes = generateXlsxBytes(headers, rows)
            fos.write(xlsxBytes)
            fos.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun generateXlsxBytes(headers: List<String>, rows: List<List<Any>>): ByteArray {
        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)

        // 1. [Content_Types].xml
        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        zos.write("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
        """.trimIndent().toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 2. _rels/.rels
        zos.putNextEntry(ZipEntry("_rels/.rels"))
        zos.write("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent().toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 3. xl/workbook.xml
        zos.putNextEntry(ZipEntry("xl/workbook.xml"))
        zos.write("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets>
                <sheet name="Laporan Transaksi" sheetId="1" r:id="rId1"/>
              </sheets>
            </workbook>
        """.trimIndent().toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 4. xl/_rels/workbook.xml.rels
        zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zos.write("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            </Relationships>
        """.trimIndent().toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 5. xl/worksheets/sheet1.xml
        zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<sheetData>""")

        // Row 1: Headers
        sb.append("""<row r="1">""")
        for (colIdx in headers.indices) {
            val ref = getColumnLabel(colIdx) + "1"
            val headerVal = escapeXml(headers[colIdx])
            sb.append("""<c r="$ref" t="inlineStr"><is><t>$headerVal</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 2+: Transactions Table Data
        for (rowIdx in rows.indices) {
            val excelRowNum = rowIdx + 2
            sb.append("""<row r="$excelRowNum">""")
            val rowData = rows[rowIdx]
            for (colIdx in rowData.indices) {
                val ref = getColumnLabel(colIdx) + excelRowNum
                val value = rowData[colIdx]
                if (value is Number) {
                    sb.append("""<c r="$ref" t="n"><v>$value</v></c>""")
                } else {
                    val strVal = escapeXml(value.toString())
                    sb.append("""<c r="$ref" t="inlineStr"><is><t>$strVal</t></is></c>""")
                }
            }
            sb.append("""</row>""")
        }

        sb.append("""</sheetData>""")
        sb.append("""</worksheet>""")

        zos.write(sb.toString().toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        zos.close()
        return bos.toByteArray()
    }

    private fun getColumnLabel(colIndex: Int): String {
        var temp = colIndex
        val sb = java.lang.StringBuilder()
        while (temp >= 0) {
            sb.insert(0, ('A'.code + (temp % 26)).toChar())
            temp = (temp / 26) - 1
        }
        return sb.toString()
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Generates a standard comma-separated values (CSV) file as bytes
     * for easy import into Excel, Sheets, or other tools.
     */
    fun exportToCsv(context: Context, transactions: List<Transaction>): File? {
        try {
            val file = File(context.cacheDir, "MoneyFlow_Laporan_Keuangan.csv")
            val outputStream = FileOutputStream(file)
            val sb = java.lang.StringBuilder()
            
            // CSV Headers
            sb.append("No,Tanggal,Tipe,Kategori,Nominal,Catatan\n")
            
            transactions.forEachIndexed { index, t ->
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(t.date))
                val no = index + 1
                val type = escapeCsv(t.type)
                val category = escapeCsv(t.category)
                val amount = t.amount
                val note = escapeCsv(t.note)
                sb.append("$no,$dateStr,$type,$category,$amount,$note\n")
            }
            
            outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
            outputStream.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun escapeCsv(s: String): String {
        var clean = s.replace("\"", "\"\"")
        if (clean.contains(",") || clean.contains("\n") || clean.contains("\r") || clean.contains("\"")) {
            clean = "\"$clean\""
        }
        return clean
    }
}
