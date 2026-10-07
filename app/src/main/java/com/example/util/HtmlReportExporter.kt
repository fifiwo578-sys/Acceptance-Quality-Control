package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.DailyReportEntity
import com.example.data.repository.ReportDataBundle
import java.io.File
import java.io.FileOutputStream

object HtmlReportExporter {

    /**
     * Builds a responsive, modern HTML document matching the EDR app daily report page design.
     */
    fun buildStyledHtmlReport(
        report: DailyReportEntity,
        data: ReportDataBundle,
        publicUrl: String
    ): String {
        val totalQC = data.qcWagon.size + data.qcLoco.size + data.qcCoach.size
        val totalTasks = data.taskAqms.size + data.taskWagon.size + data.taskLoco.size + data.taskCoach.size + data.taskEquipment.size

        val qcWagonRows = data.qcWagon.joinToString("") {
            """
            <tr>
              <td><span class="badge badge-wagon">Wagon</span></td>
              <td><strong>${it.number}</strong></td>
              <td>${it.maintenanceType}</td>
              <td>${it.description}</td>
              <td><span class="badge badge-pass">${it.status}</span></td>
            </tr>
            """.trimIndent()
        }

        val qcLocoRows = data.qcLoco.joinToString("") {
            """
            <tr>
              <td><span class="badge badge-loco">Loco</span></td>
              <td><strong>${it.number}</strong></td>
              <td>${it.maintenanceType}</td>
              <td>${it.description}</td>
              <td><span class="badge badge-pass">${it.status}</span></td>
            </tr>
            """.trimIndent()
        }

        val qcCoachRows = data.qcCoach.joinToString("") {
            """
            <tr>
              <td><span class="badge badge-coach">Coach</span></td>
              <td><strong>${it.number}</strong></td>
              <td>${it.maintenanceType}</td>
              <td>${it.description}</td>
              <td><span class="badge badge-pass">${it.status}</span></td>
            </tr>
            """.trimIndent()
        }

        val taskRows = buildString {
            if (data.taskAqms.isNotEmpty()) {
                append(data.taskAqms.joinToString("") { "<tr><td><span class=\"badge badge-dept\">AQMS</span></td><td>${it.description}</td><td>${it.remark.ifBlank { "Executed per standard" }}</td></tr>" })
            }
            if (data.taskWagon.isNotEmpty()) {
                append(data.taskWagon.joinToString("") { "<tr><td><span class=\"badge badge-dept\">Wagon</span></td><td>${it.description}</td><td>${it.remark.ifBlank { "Executed per standard" }}</td></tr>" })
            }
            if (data.taskLoco.isNotEmpty()) {
                append(data.taskLoco.joinToString("") { "<tr><td><span class=\"badge badge-dept\">Locomotive</span></td><td>${it.description}</td><td>${it.remark.ifBlank { "Executed per standard" }}</td></tr>" })
            }
            if (data.taskCoach.isNotEmpty()) {
                append(data.taskCoach.joinToString("") { "<tr><td><span class=\"badge badge-dept\">Coach</span></td><td>${it.description}</td><td>${it.remark.ifBlank { "Executed per standard" }}</td></tr>" })
            }
            if (data.taskEquipment.isNotEmpty()) {
                append(data.taskEquipment.joinToString("") { "<tr><td><span class=\"badge badge-dept\">Equipment</span></td><td>${it.description}</td><td>${it.remark.ifBlank { "Executed per standard" }}</td></tr>" })
            }
        }

        var secNum = 1

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>EDR Daily Report - ${report.reportDate}</title>
          <style>
            :root {
              --edr-green: #006837;
              --edr-green-dark: #005a2b;
              --edr-green-light: #e8f5e9;
              --edr-gold: #e8a308;
              --text-dark: #0f172a;
              --text-muted: #64748b;
              --border: #e2e8f0;
              --card-bg: #ffffff;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
              background-color: #f1f5f9;
              color: var(--text-dark);
              line-height: 1.5;
              padding: 16px;
            }
            .page-container {
              max-width: 860px;
              margin: 0 auto;
              background: var(--card-bg);
              border-radius: 12px;
              box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
              overflow: hidden;
              border: 1px solid var(--border);
            }
            .header-banner {
              background: linear-gradient(135deg, var(--edr-green-dark), var(--edr-green));
              color: white;
              padding: 28px 24px;
              text-align: center;
              position: relative;
            }
            .header-banner h1 {
              font-size: 22px;
              font-weight: 800;
              letter-spacing: 0.5px;
              text-transform: uppercase;
              margin-bottom: 4px;
            }
            .header-banner h2 {
              font-size: 14px;
              font-weight: 500;
              opacity: 0.9;
              margin-bottom: 2px;
            }
            .header-banner h3 {
              font-size: 16px;
              font-weight: 700;
              color: #fef08a;
              margin-top: 6px;
            }
            .meta-strip {
              display: flex;
              flex-wrap: wrap;
              justify-content: space-between;
              align-items: center;
              background: #f8fafc;
              border-bottom: 1px solid var(--border);
              padding: 12px 24px;
              gap: 10px;
            }
            .meta-item {
              font-size: 12px;
              font-weight: 600;
              color: var(--edr-green);
            }
            .badge-qualified {
              background: #dcfce7;
              color: #15803d;
              font-weight: 700;
              font-size: 11px;
              padding: 4px 10px;
              border-radius: 9999px;
              border: 1px solid #86efac;
            }
            .content-body {
              padding: 24px;
            }
            .section {
              margin-bottom: 24px;
            }
            .section-header {
              display: flex;
              align-items: center;
              gap: 8px;
              font-size: 14px;
              font-weight: 700;
              color: var(--edr-green);
              text-transform: uppercase;
              letter-spacing: 0.5px;
              padding-bottom: 6px;
              border-bottom: 2px solid var(--edr-green);
              margin-bottom: 12px;
            }
            .summary-box {
              background: #f8fafc;
              border: 1px solid var(--border);
              border-left: 4px solid var(--edr-green);
              padding: 14px 16px;
              border-radius: 6px;
              font-size: 13px;
              color: #334155;
            }
            table {
              width: 100%;
              border-collapse: collapse;
              margin-top: 8px;
              font-size: 12px;
            }
            th, td {
              padding: 10px 12px;
              border: 1px solid var(--border);
              text-align: left;
            }
            th {
              background-color: var(--edr-green-light);
              color: var(--edr-green-dark);
              font-weight: 700;
            }
            tr:nth-child(even) { background-color: #fafbfc; }
            .badge {
              display: inline-block;
              font-size: 10px;
              font-weight: 700;
              padding: 2px 8px;
              border-radius: 4px;
              text-transform: uppercase;
            }
            .badge-wagon { background: #f3e8ff; color: #7e22ce; }
            .badge-loco { background: #cffafe; color: #0e7490; }
            .badge-coach { background: #dcfce7; color: #15803d; }
            .badge-dept { background: #fef3c7; color: #b45309; }
            .badge-pass { background: #dcfce7; color: #15803d; }
            .url-card {
              background: #f0fdf4;
              border: 1.5px solid #86efac;
              border-radius: 8px;
              padding: 16px;
              margin: 20px 0;
            }
            .url-card-title {
              font-size: 12px;
              font-weight: 700;
              color: var(--edr-green-dark);
              margin-bottom: 6px;
            }
            .url-card-link {
              font-size: 12px;
              color: var(--edr-green);
              word-break: break-all;
              font-weight: 700;
              font-family: monospace;
              text-decoration: underline;
            }
            .url-card-desc {
              font-size: 11px;
              color: var(--text-muted);
              margin-top: 6px;
            }
            .sign-off-stamp {
              background: var(--edr-green-light);
              border: 1.5px solid #86efac;
              border-radius: 8px;
              padding: 16px;
              text-align: center;
              margin-top: 24px;
            }
            .stamp-title {
              color: var(--edr-green-dark);
              font-weight: 800;
              font-size: 13px;
              letter-spacing: 0.5px;
            }
            .stamp-sub {
              font-size: 11px;
              color: #334155;
              margin-top: 4px;
            }
            .footer-info {
              font-size: 11px;
              color: var(--text-muted);
              text-align: center;
              padding: 16px 24px;
              background: #f8fafc;
              border-top: 1px solid var(--border);
            }
            @media print {
              body { background: white; padding: 0; }
              .page-container { box-shadow: none; border: none; }
            }
          </style>
        </head>
        <body>
          <div class="page-container">
            <div class="header-banner">
              <h1>Ethio-Djibouti Standard Gauge Railway</h1>
              <h2>Acceptance and QC Team</h2>
              <h3>DAILY QUALITY CONTROL & VERIFICATION REPORT</h3>
            </div>
            
            <div class="meta-strip">
              <div class="meta-item">REPORT DATE: ${report.reportDate}</div>
              <div class="meta-item">TOTAL UNITS INSPECTED: $totalQC</div>
              <div><span class="badge-qualified">STATUS: ${report.status.uppercase()} (#QUALIFIED)</span></div>
            </div>

            <div class="content-body">
              <!-- Executive Summary -->
              <div class="section">
                <div class="section-header">${secNum++}. Executive Summary</div>
                <div class="summary-box">
                  ${report.executiveSummary.ifBlank { "All rolling stock acceptance inspections and quality control verifications for ${report.reportDate} have been certified per EDR safety procedures." }}
                </div>
              </div>

              <!-- Rolling Stock Inspections (Only entered sections) -->
              ${if (totalQC > 0) """
              <div class="section">
                <div class="section-header">${secNum++}. Rolling Stock Quality Control ($totalQC Units Inspected)</div>
                <table>
                  <thead>
                    <tr>
                      <th style="width: 80px;">Category</th>
                      <th style="width: 120px;">Unit Number</th>
                      <th>Maintenance / Job Type</th>
                      <th>Work Description</th>
                      <th style="width: 90px;">QC Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    $qcWagonRows
                    $qcLocoRows
                    $qcCoachRows
                  </tbody>
                </table>
              </div>
              """ else ""}

              <!-- Departmental Tasks (Only entered sections) -->
              ${if (totalTasks > 0) """
              <div class="section">
                <div class="section-header">${secNum++}. Departmental Additional Tasks ($totalTasks Tasks)</div>
                <table>
                  <thead>
                    <tr>
                      <th style="width: 110px;">Department</th>
                      <th>Task Description</th>
                      <th>Remark</th>
                    </tr>
                  </thead>
                  <tbody>
                    $taskRows
                  </tbody>
                </table>
              </div>
              """ else ""}

              <!-- Online Cloud Verification Box -->
              <div class="url-card">
                <div class="url-card-title">OFFICIAL PUBLIC VERIFICATION & CLOUD ACCESS URL:</div>
                <a href="${publicUrl}" target="_blank" class="url-card-link">${publicUrl}</a>
                <div class="url-card-desc">This report is cloud-verified on the Ethio-Djibouti Railway realtime infrastructure. Open this URL in any browser or client to verify audit logs and database integrity.</div>
              </div>

              <!-- Remarks -->
              <div class="section">
                <div class="section-header">${secNum++}. Operational Remarks</div>
                <div class="summary-box">
                  ${report.remarks.ifBlank { "All rolling stock quality inspections and measurements completed per Ethio-Djibouti Railway operating manual." }}
                </div>
              </div>

              <!-- Official Sign-off Stamp -->
              <div class="sign-off-stamp">
                <div class="stamp-title">OFFICIAL ACCEPTANCE & QUALITY CONTROL CERTIFICATION</div>
                <div class="stamp-sub">Acceptance and QC Team | Rolling Stock Department</div>
                <div class="stamp-sub">Inspected & Verified for Ethio-Djibouti Railway Operations | Date: ${report.reportDate}</div>
              </div>
            </div>

            <div class="footer-info">
              Ethio-Djibouti Standard Gauge Railway Share Company (EDR) • Acceptance and QC Team
            </div>
          </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Generates or retrieves the cached HTML web view file for this report
     */
    fun getOrGenerateHtmlFile(
        context: Context,
        report: DailyReportEntity,
        data: ReportDataBundle,
        publicUrl: String
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
        val cleanDate = report.reportDate.replace("/", "-").replace(" ", "_")
        val htmlFile = File(reportsDir, "EDR_Daily_Report_$cleanDate.html")
        val htmlContent = buildStyledHtmlReport(report, data, publicUrl)
        FileOutputStream(htmlFile).use { out ->
            out.write(htmlContent.toByteArray(Charsets.UTF_8))
        }
        return htmlFile
    }

    /**
     * Shares the formatted Web View HTML file and summary text across installed applications
     */
    fun shareHtmlReportFile(
        context: Context,
        report: DailyReportEntity,
        data: ReportDataBundle,
        publicUrl: String,
        summaryText: String
    ): Result<Unit> {
        return try {
            val htmlFile = getOrGenerateHtmlFile(context, report, data, publicUrl)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                htmlFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_SUBJECT, "Ethio-Djibouti Railway - Acceptance & QC Report (${report.reportDate})")
                putExtra(Intent.EXTRA_TEXT, summaryText)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Share Report (Web View)"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a cached HTML file and launches it in the system browser so it renders exactly like the app.
     */
    fun openHtmlReportInBrowser(
        context: Context,
        report: DailyReportEntity,
        data: ReportDataBundle,
        publicUrl: String
    ): Result<Unit> {
        return try {
            val reportsDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
            val cleanDate = report.reportDate.replace("/", "-").replace(" ", "_")
            val htmlFile = File(reportsDir, "EDR_Daily_Report_$cleanDate.html")

            val htmlContent = buildStyledHtmlReport(report, data, publicUrl)
            FileOutputStream(htmlFile).use { out ->
                out.write(htmlContent.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                htmlFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/html")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Open EDR Daily Report Web Page"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
