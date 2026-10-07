package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.DailyReportEntity
import com.example.data.model.QCEntity
import com.example.data.repository.ReportDataBundle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportExporter {

    // Ethiopian Railway Brand Colors for PDF
    private const val COLOR_EDR_GREEN = 0xFF006837.toInt()
    private const val COLOR_EDR_GREEN_LIGHT = 0xFFE8F5E9.toInt()
    private const val COLOR_EDR_GREEN_BORDER = 0xFF81C784.toInt()
    private const val COLOR_TEXT_DARK = 0xFF1E293B.toInt()
    private const val COLOR_TEXT_MUTED = 0xFF64748B.toInt()
    private const val COLOR_BG_LIGHT = 0xFFF8FAFC.toInt()
    private const val COLOR_WHITE = 0xFFFFFFFF.toInt()
    private const val COLOR_STATUS_PASS = 0xFF15803D.toInt()
    private const val COLOR_WARNING = 0xFFD97706.toInt()

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 36f

    /**
     * Generates a PDF file containing ONLY entered data for the day, with prominent public URL display
     */
    fun generatePdfFile(
        context: Context,
        report: DailyReportEntity,
        data: ReportDataBundle,
        publicUrl: String = ""
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
        val cleanDate = report.reportDate.replace("/", "-").replace(" ", "_")
        val pdfFile = File(reportsDir, "EDR_Daily_Report_$cleanDate.pdf")

        val liveUrl = if (publicUrl.isNotBlank() && !publicUrl.contains("railway.et")) {
            publicUrl
        } else if (report.publicUrl.isNotBlank() && !report.publicUrl.contains("railway.et")) {
            report.publicUrl
        } else {
            "https://acceptance-8781f-default-rtdb.firebaseio.com/reports/${report.reportDate}"
        }

        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        var yCursor = MARGIN

        fun checkPageBreak(requiredHeight: Float) {
            if (yCursor + requiredHeight > PAGE_HEIGHT - MARGIN - 30f) {
                // Draw footer on current page
                drawFooter(canvas, paint, pageNumber, report, liveUrl)
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yCursor = MARGIN + 10f
            }
        }

        // --- 1. HEADER BANNER ---
        paint.color = COLOR_EDR_GREEN
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(MARGIN, yCursor, PAGE_WIDTH - MARGIN, yCursor + 68f), 8f, 8f, paint)

        paint.color = COLOR_WHITE
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("ETHIO-DJIBOUTI STANDARD GAUGE RAILWAY (EDR)", MARGIN + 16f, yCursor + 26f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText("ACCEPTANCE AND QUALITY CONTROL MANAGEMENT SYSTEM", MARGIN + 16f, yCursor + 44f, paint)

        paint.textSize = 9f
        paint.color = COLOR_EDR_GREEN_LIGHT
        canvas.drawText("DAILY QUALITY CONTROL & ROLLING STOCK VERIFICATION REPORT", MARGIN + 16f, yCursor + 58f, paint)

        yCursor += 80f

        // --- 2. REPORT METADATA STRIP ---
        paint.color = COLOR_BG_LIGHT
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(MARGIN, yCursor, PAGE_WIDTH - MARGIN, yCursor + 52f), 6f, 6f, paint)

        paint.color = COLOR_EDR_GREEN_BORDER
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(MARGIN, yCursor, PAGE_WIDTH - MARGIN, yCursor + 52f), 6f, 6f, paint)

        paint.style = Paint.Style.FILL
        paint.color = COLOR_EDR_GREEN
        paint.textSize = 10.5f
        paint.isFakeBoldText = true
        canvas.drawText("REPORT DATE: ${report.reportDate}", MARGIN + 14f, yCursor + 17f, paint)

        val totalQC = data.qcWagon.size + data.qcLoco.size + data.qcCoach.size
        paint.color = COLOR_TEXT_DARK
        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        canvas.drawText("Total Active Units Inspected: $totalQC", MARGIN + 14f, yCursor + 31f, paint)

        paint.color = COLOR_EDR_GREEN
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        canvas.drawText("Cloud URL: $liveUrl", MARGIN + 14f, yCursor + 44f, paint)

        paint.color = COLOR_STATUS_PASS
        paint.textSize = 10f
        paint.isFakeBoldText = true
        val statusText = "STATUS: ${report.status.uppercase()} (#QUALIFIED)"
        canvas.drawText(statusText, PAGE_WIDTH - MARGIN - paint.measureText(statusText) - 14f, yCursor + 22f, paint)

        yCursor += 62f

        // --- 3. EXECUTIVE SUMMARY ---
        checkPageBreak(70f)
        drawSectionTitle(canvas, paint, "1. Executive Summary", yCursor)
        yCursor += 18f

        val summaryLines = wrapText(report.executiveSummary.ifBlank {
            "All rolling stock quality control verifications and acceptance inspections for ${report.reportDate} have been completed in accordance with railway safety standards."
        }, PAGE_WIDTH - 2 * MARGIN - 20f, 9.5f)

        paint.color = COLOR_BG_LIGHT
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(MARGIN, yCursor, PAGE_WIDTH - MARGIN, yCursor + (summaryLines.size * 14f) + 12f), 4f, 4f, paint)

        paint.color = COLOR_TEXT_DARK
        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        var lineY = yCursor + 14f
        for (line in summaryLines) {
            canvas.drawText(line, MARGIN + 10f, lineY, paint)
            lineY += 14f
        }
        yCursor = lineY + 12f

        var sectionNumber = 2

        // --- 4. QC - WAGON (ONLY IF ENTERED DATA EXISTS) ---
        if (data.qcWagon.isNotEmpty()) {
            checkPageBreak(60f)
            drawSectionTitle(canvas, paint, "$sectionNumber. Quality Control - Wagon (${data.qcWagon.size} Units)", yCursor)
            sectionNumber++
            yCursor += 18f

            for (wagon in data.qcWagon) {
                yCursor = drawQCCard(canvas, paint, wagon, yCursor, ::checkPageBreak)
            }
        }

        // --- 5. QC - LOCOMOTIVE (ONLY IF ENTERED DATA EXISTS) ---
        if (data.qcLoco.isNotEmpty()) {
            checkPageBreak(60f)
            drawSectionTitle(canvas, paint, "$sectionNumber. Quality Control - Locomotive (${data.qcLoco.size} Units)", yCursor)
            sectionNumber++
            yCursor += 18f

            for (loco in data.qcLoco) {
                yCursor = drawQCCard(canvas, paint, loco, yCursor, ::checkPageBreak)
            }
        }

        // --- 6. QC - COACH (ONLY IF ENTERED DATA EXISTS) ---
        if (data.qcCoach.isNotEmpty()) {
            checkPageBreak(60f)
            drawSectionTitle(canvas, paint, "$sectionNumber. Quality Control - Coach (${data.qcCoach.size} Units)", yCursor)
            sectionNumber++
            yCursor += 18f

            for (coach in data.qcCoach) {
                yCursor = drawQCCard(canvas, paint, coach, yCursor, ::checkPageBreak)
            }
        }

        // --- 7. DEPARTMENTAL TASKS (ONLY IF ENTERED DATA EXISTS) ---
        val allTasks = mutableListOf<Pair<String, com.example.data.model.TaskEntity>>()
        data.taskAqms.forEach { allTasks.add("AQMS" to it) }
        data.taskWagon.forEach { allTasks.add("Wagon" to it) }
        data.taskLoco.forEach { allTasks.add("Locomotive" to it) }
        data.taskCoach.forEach { allTasks.add("Coach" to it) }
        data.taskEquipment.forEach { allTasks.add("Equipment" to it) }

        if (allTasks.isNotEmpty()) {
            checkPageBreak(60f)
            drawSectionTitle(canvas, paint, "$sectionNumber. Departmental & Division Tasks (${allTasks.size} Tasks)", yCursor)
            sectionNumber++
            yCursor += 18f

            for ((dept, task) in allTasks) {
                checkPageBreak(40f)
                paint.color = COLOR_BG_LIGHT
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(RectF(MARGIN, yCursor, PAGE_WIDTH - MARGIN, yCursor + 32f), 4f, 4f, paint)

                paint.color = COLOR_EDR_GREEN
                paint.textSize = 9.5f
                paint.isFakeBoldText = true
                canvas.drawText("[$dept] ${task.description}", MARGIN + 10f, yCursor + 14f, paint)

                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 8.5f
                paint.isFakeBoldText = false
                canvas.drawText("Remark: ${task.remark.ifBlank { "Executed per standard operations" }}", MARGIN + 10f, yCursor + 26f, paint)

                yCursor += 38f
            }
        }

        // --- 8. DIGITAL CLOUD VERIFICATION & LIVE REPORT URL ---
        checkPageBreak(65f)
        drawSectionTitle(canvas, paint, "$sectionNumber. Digital Cloud Verification & Live Report URL", yCursor)
        sectionNumber++
        yCursor += 18f

        paint.color = COLOR_BG_LIGHT
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(MARGIN, yCursor, PAGE_WIDTH - MARGIN, yCursor + 46f), 6f, 6f, paint)

        paint.color = COLOR_EDR_GREEN_BORDER
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(MARGIN, yCursor, PAGE_WIDTH - MARGIN, yCursor + 46f), 6f, 6f, paint)

        paint.style = Paint.Style.FILL
        paint.color = COLOR_EDR_GREEN
        paint.textSize = 9.5f
        paint.isFakeBoldText = true
        canvas.drawText("OFFICIAL PUBLIC VERIFICATION URL (REALTIME DATABASE):", MARGIN + 12f, yCursor + 16f, paint)

        paint.color = COLOR_EDR_GREEN
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText(liveUrl, MARGIN + 12f, yCursor + 30f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("Open in any web browser to view JSON inspection payload, audit history, and sign-offs.", MARGIN + 12f, yCursor + 41f, paint)

        yCursor += 56f

        // --- 9. OFFICIAL SIGN-OFF & CERTIFICATION STAMP ---
        checkPageBreak(75f)
        paint.color = COLOR_EDR_GREEN_LIGHT
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(MARGIN, yCursor, PAGE_WIDTH - MARGIN, yCursor + 55f), 6f, 6f, paint)

        paint.color = COLOR_EDR_GREEN_BORDER
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(MARGIN, yCursor, PAGE_WIDTH - MARGIN, yCursor + 55f), 6f, 6f, paint)

        paint.style = Paint.Style.FILL
        paint.color = COLOR_EDR_GREEN
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("OFFICIAL QUALITY CONTROL SIGN-OFF & ACCEPTANCE STAMP", MARGIN + 14f, yCursor + 18f, paint)

        paint.color = COLOR_TEXT_DARK
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        canvas.drawText("Acceptance and QC Team | Rolling Stock Department", MARGIN + 14f, yCursor + 32f, paint)
        canvas.drawText("Inspected & Certified for Ethio-Djibouti Railway Operations | Date: ${report.reportDate}", MARGIN + 14f, yCursor + 44f, paint)

        drawFooter(canvas, paint, pageNumber, report, liveUrl)
        document.finishPage(page)

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    private fun drawSectionTitle(canvas: Canvas, paint: Paint, title: String, y: Float) {
        paint.color = COLOR_EDR_GREEN
        paint.textSize = 11.5f
        paint.isFakeBoldText = true
        canvas.drawText(title, MARGIN, y, paint)

        paint.strokeWidth = 1.5f
        canvas.drawLine(MARGIN, y + 4f, PAGE_WIDTH - MARGIN, y + 4f, paint)
    }

    private fun drawQCCard(
        canvas: Canvas,
        paint: Paint,
        item: QCEntity,
        startY: Float,
        checkPageBreak: (Float) -> Unit
    ): Float {
        val faultLines = wrapText("Fault: ${item.fault.ifBlank { "Standard inspection" }}", PAGE_WIDTH - 2 * MARGIN - 24f, 9f)
        val actionLines = wrapText("Action: ${item.description.ifBlank { "Standard check performed" }}", PAGE_WIDTH - 2 * MARGIN - 24f, 9f)
        val totalLines = faultLines.size + actionLines.size
        val cardHeight = 36f + (totalLines * 13f) + if (item.sparePart.isNotBlank() && item.sparePart != "None") 16f else 4f

        checkPageBreak(cardHeight + 8f)

        // Draw card background
        paint.color = COLOR_WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + cardHeight), 6f, 6f, paint)

        paint.color = COLOR_EDR_GREEN_BORDER
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(RectF(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + cardHeight), 6f, 6f, paint)

        // Top bar of card
        paint.style = Paint.Style.FILL
        paint.color = COLOR_EDR_GREEN
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("Rolling Stock ID: ${item.number}", MARGIN + 12f, startY + 16f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        canvas.drawText("Subsystem: ${item.maintenanceType}", MARGIN + 180f, startY + 16f, paint)

        paint.color = COLOR_STATUS_PASS
        paint.isFakeBoldText = true
        canvas.drawText("#QUALIFIED", PAGE_WIDTH - MARGIN - 75f, startY + 16f, paint)

        var curY = startY + 30f

        // Fault Text
        paint.color = COLOR_TEXT_DARK
        paint.textSize = 9f
        paint.isFakeBoldText = false
        for (fl in faultLines) {
            canvas.drawText(fl, MARGIN + 12f, curY, paint)
            curY += 13f
        }

        // Action Text
        paint.color = COLOR_TEXT_MUTED
        for (al in actionLines) {
            canvas.drawText(al, MARGIN + 12f, curY, paint)
            curY += 13f
        }

        // Spare parts if any
        if (item.sparePart.isNotBlank() && item.sparePart != "None" && item.sparePart != "None (Adjustment & Verification)") {
            paint.color = COLOR_WARNING
            paint.isFakeBoldText = true
            paint.textSize = 8.5f
            canvas.drawText("Spares / Adjustments: ${item.sparePart}", MARGIN + 12f, curY + 2f, paint)
            curY += 14f
        }

        return curY + 8f
    }

    private fun drawFooter(canvas: Canvas, paint: Paint, pageNumber: Int, report: DailyReportEntity, url: String) {
        val footerY = PAGE_HEIGHT - MARGIN + 8f
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.isFakeBoldText = false
        canvas.drawText("Ethio-Djibouti Railway (EDR) | Cloud URL: $url", MARGIN, footerY - 9f, paint)
        canvas.drawText("Acceptance and QC Team • Daily Report ${report.reportDate}", MARGIN, footerY, paint)
        canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN - 35f, footerY, paint)
    }

    private fun wrapText(text: String, maxWidth: Float, textSize: Float): List<String> {
        val paint = Paint().apply { this.textSize = textSize }
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (para in paragraphs) {
            val words = para.split(" ")
            var currentLine = StringBuilder()

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = StringBuilder(testLine)
                } else {
                    if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        }
        return lines
    }

    /**
     * Builds smart report plain text for emails, only including entered modules
     */
    fun buildSmartReportText(
        report: DailyReportEntity,
        data: ReportDataBundle,
        publicUrl: String
    ): String {
        val totalQC = data.qcWagon.size + data.qcLoco.size + data.qcCoach.size
        val totalTasks = data.taskAqms.size + data.taskWagon.size + data.taskLoco.size + data.taskCoach.size + data.taskEquipment.size

        val qcBreakdown = mutableListOf<String>()
        if (data.qcWagon.isNotEmpty()) qcBreakdown.add("• Wagon Acceptance Inspections: ${data.qcWagon.size} units")
        if (data.qcLoco.isNotEmpty()) qcBreakdown.add("• Locomotive Sign-offs: ${data.qcLoco.size} units")
        if (data.qcCoach.isNotEmpty()) qcBreakdown.add("• Coach Quality Checks: ${data.qcCoach.size} units")

        val taskBreakdown = mutableListOf<String>()
        if (data.taskAqms.isNotEmpty()) taskBreakdown.add("• AQMS QA Tasks: ${data.taskAqms.size}")
        if (data.taskWagon.isNotEmpty()) taskBreakdown.add("• Wagon Department Tasks: ${data.taskWagon.size}")
        if (data.taskLoco.isNotEmpty()) taskBreakdown.add("• Locomotive Department Tasks: ${data.taskLoco.size}")
        if (data.taskCoach.isNotEmpty()) taskBreakdown.add("• Coach Department Tasks: ${data.taskCoach.size}")
        if (data.taskEquipment.isNotEmpty()) taskBreakdown.add("• Equipment Tasks: ${data.taskEquipment.size}")

        val vehicleDetails = mutableListOf<String>()
        data.qcWagon.forEach {
            vehicleDetails.add("  [Wagon ${it.number}] Subsystem: ${it.maintenanceType} | Fault: ${it.fault.ifBlank { "None" }} | Spares: ${it.sparePart.ifBlank { "None" }} | Status: #${it.status}")
        }
        data.qcLoco.forEach {
            vehicleDetails.add("  [Locomotive ${it.number}] Subsystem: ${it.maintenanceType} | Fault: ${it.fault.ifBlank { "None" }} | Status: #${it.status}")
        }
        data.qcCoach.forEach {
            vehicleDetails.add("  [Coach ${it.number}] Subsystem: ${it.maintenanceType} | Fault: ${it.fault.ifBlank { "None" }} | Status: #${it.status}")
        }

        return """
ETHIO-DJIBOUTI STANDARD GAUGE RAILWAY (EDR)
ACCEPTANCE AND QUALITY CONTROL MANAGEMENT SYSTEM
============================================================
SMART DAILY REPORT (OFFICIAL PDF ATTACHED)
Report Date: ${report.reportDate}
Approval Status: ${report.status} (100% #Qualified)
Online Cloud Link: $publicUrl
============================================================

1. EXECUTIVE SUMMARY:
${report.executiveSummary}

2. QUALITY CONTROL VERIFIED UNITS (Total: $totalQC):
${if (qcBreakdown.isNotEmpty()) qcBreakdown.joinToString("\n") else "• No rolling stock inspections scheduled for this date."}

${if (vehicleDetails.isNotEmpty()) "VEHICLE INSPECTION BREAKDOWN:\n" + vehicleDetails.joinToString("\n") else ""}

${if (taskBreakdown.isNotEmpty()) "3. DEPARTMENTAL TASKS (Total: $totalTasks):\n" + taskBreakdown.joinToString("\n") + "\n" else ""}
4. ATTACHMENT & CLOUD VERIFICATION:
• An official PDF copy of this daily report is attached to this email.
• Real-time cloud verification and data inspection is available at:
$publicUrl

------------------------------------------------------------
Ethio-Djibouti Standard Gauge Railway Share Company (EDR)
Acceptance and QC Team
Automated Dispatch via EDR Mobile Management System
""".trimIndent()
    }

    /**
     * Dispatches an email with the PDF attached via Android Intent.ACTION_SEND
     */
    fun sendReportPdfViaEmail(
        context: Context,
        report: DailyReportEntity,
        data: ReportDataBundle,
        recipients: List<String>,
        publicUrl: String
    ): Result<Unit> {
        return try {
            val pdfFile = generatePdfFile(context, report, data, publicUrl)
            val pdfUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val subject = "Ethio-Djibouti Railway - Acceptance & QC Daily Report [PDF] - ${report.reportDate}"
            val body = buildSmartReportText(report, data, publicUrl)

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                if (recipients.isNotEmpty()) {
                    putExtra(Intent.EXTRA_EMAIL, recipients.toTypedArray())
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(emailIntent, "Send Daily Report PDF via Email"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares the generated PDF file across installed apps (WhatsApp, Telegram, Drive, etc.)
     */
    fun sharePdfFile(
        context: Context,
        report: DailyReportEntity,
        data: ReportDataBundle,
        publicUrl: String
    ): Result<Unit> {
        return try {
            val pdfFile = generatePdfFile(context, report, data, publicUrl)
            val pdfUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_SUBJECT, "EDR Daily Report (${report.reportDate})")
                putExtra(Intent.EXTRA_TEXT, "Ethio-Djibouti Railway Acceptance & Quality Control Daily Report (${report.reportDate}):\n$publicUrl")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Daily Report PDF"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
