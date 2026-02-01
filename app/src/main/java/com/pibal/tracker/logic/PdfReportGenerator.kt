package com.pibal.tracker.logic

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PdfReportGenerator(private val context: Context) {

    private val TAG = "PdfReportGenerator"

    /**
     * Generates a PDF report from the given wind results.
     * The results are expected to be already sorted by altitude.
     */
    fun generateReport(windResults: List<WindResult>): String? {
        if (windResults.isEmpty()) return null

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            isFakeBoldText = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        val linePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 1f
        }

        var y = 50f
        val xMargin = 50f

        // Title
        canvas.drawText("PiBal Tracker Measurement Report", xMargin, y, titlePaint)
        y += 40f

        // Date and Time
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date())
        canvas.drawText("Generated on: $timestamp", xMargin, y, textPaint)
        y += 40f

        // Table Header
        val colWidths = floatArrayOf(150f, 150f, 150f)
        val headers = arrayOf("Altitude (m)", "Wind Speed (m/s)", "Wind Direction (°)")

        var currentX = xMargin
        for (i in headers.indices) {
            canvas.drawText(headers[i], currentX, y, headerPaint)
            currentX += colWidths[i]
        }
        y += 10f
        canvas.drawLine(xMargin, y, 595f - xMargin, y, linePaint)
        y += 20f

        // Table Content
        for (result in windResults) {
            // Check if we need a new page
            if (y > 800) {
                pdfDocument.finishPage(page)
                // In a real app, you'd handle multi-page logic here.
                // For now, let's keep it simple as PiBal data is usually not that huge.
                break 
            }

            currentX = xMargin
            canvas.drawText(result.heightMeters.toInt().toString(), currentX, y, textPaint)
            currentX += colWidths[0]
            
            canvas.drawText(String.format(Locale.US, "%.1f", result.windSpeed), currentX, y, textPaint)
            currentX += colWidths[1]
            
            canvas.drawText(result.windDirection.toInt().toString(), currentX, y, textPaint)
            
            y += 20f
        }

        pdfDocument.finishPage(page)

        // Save the document
        val fileName = "PiBal_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(directory, fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Log.d(TAG, "PDF generated at: ${file.absolutePath}")
            return file.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Error writing PDF", e)
        } finally {
            pdfDocument.close()
        }

        return null
    }
}
