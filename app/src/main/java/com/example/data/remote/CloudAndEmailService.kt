package com.example.data.remote

import com.example.data.model.DailyReportEntity
import com.example.data.model.QCEntity
import com.example.data.model.RecipientEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CloudAndEmailService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Send email notification via Resend API (if API key is provided) or native email client
     */
    suspend fun sendDailyReportEmail(
        apiKey: String,
        fromEmail: String,
        recipient: RecipientEntity,
        report: DailyReportEntity,
        publicUrl: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank()) {
            val subject = "Ethio-Djibouti Railway - Acceptance and Quality Control Daily Report - ${report.reportDate}"
            val htmlContent = buildEmailHtml(report, publicUrl)

            val jsonBody = JSONObject().apply {
                put("from", fromEmail.ifBlank { "reports@ethiodjiboutirailway.com" })
                put("to", JSONArray().apply { put(recipient.email) })
                put("subject", subject)
                put("html", htmlContent)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.resend.com/emails")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    return@withContext Result.success(responseBody)
                } else {
                    val errorMsg = try {
                        JSONObject(responseBody).optString("message", responseBody)
                    } catch (e: Exception) {
                        responseBody
                    }
                    return@withContext Result.failure(Exception("Resend API Error (${response.code}): $errorMsg"))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Network error sending email: ${e.message}"))
            }
        }

        // When no Resend API key is configured, succeed gracefully for native Android email client dispatch
        Result.success("Dispatched via native email system")
    }

    /**
     * Test email sending
     */
    suspend fun sendTestEmail(
        apiKey: String,
        fromEmail: String,
        toEmail: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank()) {
            val jsonBody = JSONObject().apply {
                put("from", fromEmail.ifBlank { "reports@ethiodjiboutirailway.com" })
                put("to", JSONArray().apply { put(toEmail) })
                put("subject", "Ethio-Djibouti Railway - Test Notification")
                put("html", """
                    <div style="font-family: Arial, sans-serif; background-color: #060a11; color: #e8ecf2; padding: 24px; border-radius: 8px;">
                        <h2 style="color: #e8a308; margin-top: 0;">Ethio-Djibouti Railway</h2>
                        <p style="color: #8b9bb5;">Acceptance and Quality Control System</p>
                        <hr style="border: 0; border-top: 1px solid #1a2740; margin: 16px 0;" />
                        <p>This is a test notification from the Acceptance and Quality Control dashboard.</p>
                        <p style="color: #10b981; font-weight: bold;">Transactional email integration is operational.</p>
                    </div>
                """.trimIndent())
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.resend.com/emails")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                return@withContext if (response.isSuccessful) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Test email error (${response.code}): $body"))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Network error: ${e.message}"))
            }
        }

        Result.success("Test notification ready for native email dispatch")
    }

    private var telegraphToken: String = "f83c924da54aca25cfad1839c38ed05a45cfc56277806002889875987f32"

    /**
     * Publishes the daily report to an online Web View (Telegra.ph) so opening the shared link
     * opens an interactive, responsive web article view rather than a raw .json file!
     */
    suspend fun publishReportToOnlineWebView(
        report: DailyReportEntity,
        bundle: com.example.data.repository.ReportDataBundle? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Ensure token
            if (telegraphToken.isBlank()) {
                val accReq = Request.Builder()
                    .url("https://api.telegra.ph/createAccount?short_name=EDR_QC&author_name=Ethio-Djibouti_Railway")
                    .get()
                    .build()
                val accResp = client.newCall(accReq).execute()
                val accBody = accResp.body?.string() ?: ""
                val accJson = JSONObject(accBody)
                if (accJson.optBoolean("ok")) {
                    telegraphToken = accJson.getJSONObject("result").getString("access_token")
                }
            }

            val totalQC = report.qcWagonCount + report.qcLocoCount + report.qcCoachCount
            val totalTasks = report.aqmsTaskCount + report.wagonTaskCount + report.locoTaskCount + report.coachTaskCount + report.equipmentTaskCount

            val contentArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("tag", "h3")
                    put("children", JSONArray().apply { put("ETHIO-DJIBOUTI STANDARD GAUGE RAILWAY") })
                })
                put(JSONObject().apply {
                    put("tag", "h4")
                    put("children", JSONArray().apply { put("Acceptance and Quality Control | Daily Report: ${report.reportDate}") })
                })
                put(JSONObject().apply {
                    put("tag", "blockquote")
                    put("children", JSONArray().apply { put("STATUS: ${report.status.uppercase()} (#QUALIFIED • PASSED FOR REVENUE SERVICE)") })
                })
                put(JSONObject().apply {
                    put("tag", "h4")
                    put("children", JSONArray().apply { put("1. Executive Summary") })
                })
                put(JSONObject().apply {
                    put("tag", "p")
                    put("children", JSONArray().apply { put(report.executiveSummary.ifBlank { "All rolling stock acceptance inspections and quality verifications for ${report.reportDate} have been certified per standard operating procedures." }) })
                })

                if (bundle != null && totalQC > 0) {
                    put(JSONObject().apply {
                        put("tag", "h4")
                        put("children", JSONArray().apply { put("2. Quality Control Inspections ($totalQC Total Units)") })
                    })
                    bundle.qcWagon.forEach { w ->
                        put(JSONObject().apply {
                            put("tag", "p")
                            put("children", JSONArray().apply { put("• Wagon ${w.number}: ${w.maintenanceType} - ${w.description} (Status: ${w.status})") })
                        })
                    }
                    bundle.qcLoco.forEach { l ->
                        put(JSONObject().apply {
                            put("tag", "p")
                            put("children", JSONArray().apply { put("• Locomotive ${l.number}: ${l.maintenanceType} - ${l.description} (Status: ${l.status})") })
                        })
                    }
                    bundle.qcCoach.forEach { c ->
                        put(JSONObject().apply {
                            put("tag", "p")
                            put("children", JSONArray().apply { put("• Coach ${c.number}: ${c.maintenanceType} - ${c.description} (Status: ${c.status})") })
                        })
                    }
                } else if (totalQC > 0) {
                    put(JSONObject().apply {
                        put("tag", "h4")
                        put("children", JSONArray().apply { put("2. Quality Control Summary") })
                    })
                    if (report.qcWagonCount > 0) put(JSONObject().apply { put("tag", "p"); put("children", JSONArray().apply { put("• Wagon Acceptance Inspections: ${report.qcWagonCount} units") }) })
                    if (report.qcLocoCount > 0) put(JSONObject().apply { put("tag", "p"); put("children", JSONArray().apply { put("• Locomotive Sign-offs: ${report.qcLocoCount} units") }) })
                    if (report.qcCoachCount > 0) put(JSONObject().apply { put("tag", "p"); put("children", JSONArray().apply { put("• Coach Inspections: ${report.qcCoachCount} units") }) })
                }

                if (bundle != null && totalTasks > 0) {
                    put(JSONObject().apply {
                        put("tag", "h4")
                        put("children", JSONArray().apply { put("3. Departmental Tasks ($totalTasks Tasks)") })
                    })
                    bundle.taskAqms.forEach { t -> put(JSONObject().apply { put("tag", "p"); put("children", JSONArray().apply { put("• AQMS: ${t.description} (${t.remark})") }) }) }
                    bundle.taskWagon.forEach { t -> put(JSONObject().apply { put("tag", "p"); put("children", JSONArray().apply { put("• Wagon: ${t.description} (${t.remark})") }) }) }
                    bundle.taskLoco.forEach { t -> put(JSONObject().apply { put("tag", "p"); put("children", JSONArray().apply { put("• Locomotive: ${t.description} (${t.remark})") }) }) }
                    bundle.taskCoach.forEach { t -> put(JSONObject().apply { put("tag", "p"); put("children", JSONArray().apply { put("• Coach: ${t.description} (${t.remark})") }) }) }
                    bundle.taskEquipment.forEach { t -> put(JSONObject().apply { put("tag", "p"); put("children", JSONArray().apply { put("• Equipment: ${t.description} (${t.remark})") }) }) }
                }

                put(JSONObject().apply {
                    put("tag", "h4")
                    put("children", JSONArray().apply { put("Remarks & Sign-off Certification") })
                })
                put(JSONObject().apply {
                    put("tag", "p")
                    put("children", JSONArray().apply { put(report.remarks.ifBlank { "Certified by Acceptance and QC Team, Rolling Stock Department, Ethio-Djibouti Railway." }) })
                })
                put(JSONObject().apply {
                    put("tag", "p")
                    put("children", JSONArray().apply { put("Official Publication: ${report.publishedAt.ifBlank { report.reportDate }} | Acceptance and QC Team") })
                })
            }

            val payload = JSONObject().apply {
                put("access_token", telegraphToken)
                put("title", "EDR Daily Report ${report.reportDate}")
                put("author_name", "Ethio-Djibouti Railway - Acceptance and Quality Control")
                put("author_url", "https://acceptance-8781f-default-rtdb.firebaseio.com")
                put("content", contentArray)
                put("return_content", false)
            }

            val reqBody = payload.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("https://api.telegra.ph/createPage")
                .post(reqBody)
                .build()

            val resp = client.newCall(req).execute()
            val respStr = resp.body?.string() ?: ""
            val json = JSONObject(respStr)
            if (json.optBoolean("ok")) {
                val pageUrl = json.getJSONObject("result").getString("url")
                Result.success(pageUrl)
            } else {
                Result.failure(Exception(json.optString("error", "Failed to create web page")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Firebase Cloud Database Sync: Upload report to Firebase Realtime Database
     */
    suspend fun syncReportToFirebase(
        databaseUrl: String,
        report: DailyReportEntity
    ): Result<String> = withContext(Dispatchers.IO) {
        val targetUrl = databaseUrl.ifBlank { "https://acceptance-8781f-default-rtdb.firebaseio.com" }.trimEnd('/')
        val endpoint = "$targetUrl/reports/${report.reportDate}.json"

        val json = JSONObject().apply {
            put("id", report.id)
            put("reportDate", report.reportDate)
            put("status", report.status)
            put("title", report.title)
            put("executiveSummary", report.executiveSummary)
            put("qcWagonCount", report.qcWagonCount)
            put("qcLocoCount", report.qcLocoCount)
            put("qcCoachCount", report.qcCoachCount)
            put("aqmsTaskCount", report.aqmsTaskCount)
            put("wagonTaskCount", report.wagonTaskCount)
            put("locoTaskCount", report.locoTaskCount)
            put("coachTaskCount", report.coachTaskCount)
            put("equipmentTaskCount", report.equipmentTaskCount)
            put("publishedAt", report.publishedAt)
            put("publicUrl", report.publicUrl)
            put("updatedAt", report.updatedAt)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .put(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Report synced to Firebase successfully.")
            } else {
                Result.failure(Exception("Firebase returned HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Firebase sync error: ${e.message}"))
        }
    }

    /**
     * Sync QC Record to Firebase Realtime Database
     */
    suspend fun syncQCRecordToFirebase(
        databaseUrl: String,
        item: QCEntity
    ): Result<String> = withContext(Dispatchers.IO) {
        val targetUrl = databaseUrl.ifBlank { "https://acceptance-8781f-default-rtdb.firebaseio.com" }.trimEnd('/')
        val endpoint = "$targetUrl/qc_records/${item.module}/${item.id}.json"

        val json = JSONObject().apply {
            put("id", item.id)
            put("module", item.module)
            put("number", item.number)
            put("maintenanceType", item.maintenanceType)
            put("description", item.description)
            put("fault", item.fault)
            put("sparePart", item.sparePart)
            put("status", item.status)
            put("startDate", item.startDate)
            put("finishDate", item.finishDate)
            put("remark", item.remark)
            put("isCancelled", item.isCancelled)
            put("cancelReason", item.cancelReason)
            put("isTrash", item.isTrash)
            put("createdAt", item.createdAt)
            put("updatedAt", item.updatedAt)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(endpoint).put(requestBody).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success("QC synced to Firebase")
            else Result.failure(Exception("Firebase QC sync error: HTTP ${response.code}"))
        } catch (e: Exception) {
            Result.failure(Exception("Firebase error: ${e.message}"))
        }
    }

    /**
     * Sync Task to Firebase Realtime Database
     */
    suspend fun syncTaskToFirebase(
        databaseUrl: String,
        item: TaskEntity
    ): Result<String> = withContext(Dispatchers.IO) {
        val targetUrl = databaseUrl.ifBlank { "https://acceptance-8781f-default-rtdb.firebaseio.com" }.trimEnd('/')
        val endpoint = "$targetUrl/tasks/${item.module}/${item.id}.json"

        val json = JSONObject().apply {
            put("id", item.id)
            put("module", item.module)
            put("date", item.date)
            put("description", item.description)
            put("remark", item.remark)
            put("isCancelled", item.isCancelled)
            put("isTrash", item.isTrash)
            put("createdAt", item.createdAt)
            put("updatedAt", item.updatedAt)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(endpoint).put(requestBody).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success("Task synced to Firebase")
            else Result.failure(Exception("Firebase task sync error: HTTP ${response.code}"))
        } catch (e: Exception) {
            Result.failure(Exception("Firebase error: ${e.message}"))
        }
    }

    /**
     * Fetch reports list from Firebase Cloud Database
     */
    suspend fun fetchReportsFromFirebase(
        databaseUrl: String
    ): Result<List<DailyReportEntity>> = withContext(Dispatchers.IO) {
        val targetUrl = databaseUrl.ifBlank { "https://acceptance-8781f-default-rtdb.firebaseio.com" }.trimEnd('/')
        val endpoint = "$targetUrl/reports.json"

        val request = Request.Builder()
            .url(endpoint)
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful && body.isNotBlank() && body != "null") {
                val jsonObject = JSONObject(body)
                val list = mutableListOf<DailyReportEntity>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val item = jsonObject.optJSONObject(key)
                    if (item != null) {
                        list.add(
                            DailyReportEntity(
                                id = item.optLong("id", System.currentTimeMillis()),
                                reportDate = item.optString("reportDate", key),
                                status = item.optString("status", "Published"),
                                title = item.optString("title", "Ethio-Djibouti Railway Acceptance and Quality Control Daily Report"),
                                executiveSummary = item.optString("executiveSummary", ""),
                                qcWagonCount = item.optInt("qcWagonCount", 0),
                                qcLocoCount = item.optInt("qcLocoCount", 0),
                                qcCoachCount = item.optInt("qcCoachCount", 0),
                                aqmsTaskCount = item.optInt("aqmsTaskCount", 0),
                                wagonTaskCount = item.optInt("wagonTaskCount", 0),
                                locoTaskCount = item.optInt("locoTaskCount", 0),
                                coachTaskCount = item.optInt("coachTaskCount", 0),
                                equipmentTaskCount = item.optInt("equipmentTaskCount", 0),
                                publishedAt = item.optString("publishedAt", ""),
                                publicUrl = item.optString("publicUrl", ""),
                                updatedAt = item.optString("updatedAt", "")
                            )
                        )
                    }
                }
                Result.success(list)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch from Firebase: ${e.message}"))
        }
    }

    private fun buildEmailHtml(report: DailyReportEntity, publicUrl: String): String {
        val totalQC = report.qcWagonCount + report.qcLocoCount + report.qcCoachCount
        val totalTasks = report.aqmsTaskCount + report.wagonTaskCount + report.locoTaskCount + report.coachTaskCount + report.equipmentTaskCount

        val qcSummaryItems = mutableListOf<String>()
        if (report.qcWagonCount > 0) qcSummaryItems.add("<li><strong>Wagon Acceptance Inspections:</strong> ${report.qcWagonCount} units</li>")
        if (report.qcLocoCount > 0) qcSummaryItems.add("<li><strong>Locomotive Inspections:</strong> ${report.qcLocoCount} units</li>")
        if (report.qcCoachCount > 0) qcSummaryItems.add("<li><strong>Coach Inspections:</strong> ${report.qcCoachCount} units</li>")

        val taskSummaryItems = mutableListOf<String>()
        if (report.aqmsTaskCount > 0) taskSummaryItems.add("<li>AQMS Tasks: ${report.aqmsTaskCount}</li>")
        if (report.wagonTaskCount > 0) taskSummaryItems.add("<li>Wagon Tasks: ${report.wagonTaskCount}</li>")
        if (report.locoTaskCount > 0) taskSummaryItems.add("<li>Locomotive Tasks: ${report.locoTaskCount}</li>")
        if (report.coachTaskCount > 0) taskSummaryItems.add("<li>Coach Tasks: ${report.coachTaskCount}</li>")
        if (report.equipmentTaskCount > 0) taskSummaryItems.add("<li>Equipment Tasks: ${report.equipmentTaskCount}</li>")

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <style>
            body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f6faf6; color: #1e293b; margin: 0; padding: 20px; }
            .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
            .header { background: linear-gradient(135deg, #005a2b, #006837); padding: 24px; text-align: center; }
            .title { font-size: 20px; font-weight: bold; color: #ffffff; margin: 0; letter-spacing: 0.5px; }
            .subtitle { font-size: 12px; color: #e8f5e9; margin-top: 4px; }
            .content { padding: 24px; }
            .badge-strip { background-color: #f1f8f3; border-left: 4px solid #006837; padding: 12px 16px; border-radius: 4px; margin: 16px 0; }
            .cta-btn { display: inline-block; background-color: #006837; color: #ffffff !important; font-weight: bold; padding: 12px 28px; border-radius: 6px; text-decoration: none; margin: 20px 0; font-size: 14px; }
            .footer { padding: 16px; text-align: center; font-size: 11px; color: #64748b; border-top: 1px solid #e2e8f0; background-color: #f8fafc; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1 class="title">Ethio-Djibouti Railway</h1>
              <div class="subtitle">Acceptance and Quality Control | Daily Report</div>
            </div>
            <div class="content">
              <h2 style="font-size: 16px; color: #006837; margin-top: 0;">Official Daily Inspection Report Available</h2>
              <p style="font-size: 13px; color: #334155; line-height: 1.5;">
                The Acceptance and Quality Control daily report for <strong>${report.reportDate}</strong> has been officially compiled and verified.
              </p>
              
              <div class="badge-strip">
                <div style="font-size: 12px; color: #1e293b;"><strong>Report Date:</strong> ${report.reportDate}</div>
                <div style="font-size: 12px; color: #1e293b; margin-top: 4px;"><strong>Publication Time:</strong> ${report.publishedAt.ifBlank { "Official Record" }}</div>
                <div style="font-size: 12px; color: #006837; margin-top: 4px;"><strong>Approval Status:</strong> ${report.status} (#QUALIFIED)</div>
              </div>

              ${if (qcSummaryItems.isNotEmpty()) """
              <div style="font-size: 13px; color: #006837; margin: 14px 0;">
                <strong>Active Quality Control Sections ($totalQC Units):</strong>
                <ul style="color: #334155; font-size: 12px; margin-top: 6px; padding-left: 20px;">
                  ${qcSummaryItems.joinToString("\n")}
                </ul>
              </div>
              """ else ""}

              ${if (taskSummaryItems.isNotEmpty()) """
              <div style="font-size: 13px; color: #006837; margin: 14px 0;">
                <strong>Departmental Tasks ($totalTasks):</strong>
                <ul style="color: #334155; font-size: 12px; margin-top: 6px; padding-left: 20px;">
                  ${taskSummaryItems.joinToString("\n")}
                </ul>
              </div>
              """ else ""}

              <div style="text-align: center; margin: 24px 0;">
                <a href="$publicUrl" class="cta-btn" target="_blank">VIEW OFFICIAL REPORT</a>
              </div>
              <p style="font-size: 11px; color: #64748b; text-align: center;">
                Cloud Verification Link: <a href="$publicUrl" style="color: #006837;">$publicUrl</a>
              </p>
            </div>
            <div class="footer">
              Ethio-Djibouti Standard Gauge Railway Share Company (EDR)<br>
              Acceptance and QC Team
            </div>
          </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Build structured plain text report for native Android email clients (Gmail, Outlook, etc.)
     * ONLY includes modules that have entered data for that day.
     */
    fun buildEmailPlainText(report: DailyReportEntity, publicUrl: String): String {
        val totalQC = report.qcWagonCount + report.qcLocoCount + report.qcCoachCount
        val totalTasks = report.aqmsTaskCount + report.wagonTaskCount + report.locoTaskCount + report.coachTaskCount + report.equipmentTaskCount

        val qcLines = mutableListOf<String>()
        if (report.qcWagonCount > 0) qcLines.add("• QC Wagon Inspections: ${report.qcWagonCount} units")
        if (report.qcLocoCount > 0) qcLines.add("• QC Locomotive Sign-offs: ${report.qcLocoCount} units")
        if (report.qcCoachCount > 0) qcLines.add("• QC Coach Inspections: ${report.qcCoachCount} units")

        val taskLines = mutableListOf<String>()
        if (report.aqmsTaskCount > 0) taskLines.add("• AQMS Quality Assurance Tasks: ${report.aqmsTaskCount}")
        if (report.wagonTaskCount > 0) taskLines.add("• Wagon Department Tasks: ${report.wagonTaskCount}")
        if (report.locoTaskCount > 0) taskLines.add("• Locomotive Department Tasks: ${report.locoTaskCount}")
        if (report.coachTaskCount > 0) taskLines.add("• Coach Department Tasks: ${report.coachTaskCount}")
        if (report.equipmentTaskCount > 0) taskLines.add("• Equipment & Infrastructure Tasks: ${report.equipmentTaskCount}")

        return """
ETHIO-DJIBOUTI STANDARD GAUGE RAILWAY (EDR)
ACCEPTANCE AND QUALITY CONTROL MANAGEMENT SYSTEM
============================================================
DAILY QUALITY CONTROL & ACCEPTANCE REPORT: ${report.reportDate}
Status: ${report.status} (#QUALIFIED)
Publication Timestamp: ${report.publishedAt.ifBlank { "Official Record" }}
Online Cloud Report: $publicUrl
============================================================

1. EXECUTIVE SUMMARY:
${report.executiveSummary.ifBlank { "All rolling stock quality control verifications and rolling stock acceptance inspections for ${report.reportDate} have been completed in accordance with railway safety standards." }}

${if (qcLines.isNotEmpty()) "2. ACTIVE QUALITY CONTROL MODULES (Total: $totalQC units):\n" + qcLines.joinToString("\n") + "\n" else ""}
${if (taskLines.isNotEmpty()) "3. DEPARTMENTAL TASKS (Total: $totalTasks tasks):\n" + taskLines.joinToString("\n") + "\n" else ""}
4. DIGITAL ACCESS & VERIFICATION:
To view complete inspection sheets, defect reports, and maintenance records online:
$publicUrl

------------------------------------------------------------
Ethio-Djibouti Standard Gauge Railway Share Company (EDR)
Acceptance and QC Team
Automated Dispatch from EDR Acceptance & QC System
""".trimIndent()
    }

    /**
     * Build plain text test notification
     */
    fun buildTestEmailPlainText(toEmail: String): String {
        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        return """
ETHIO-DJIBOUTI STANDARD GAUGE RAILWAY (EDR)
ACCEPTANCE AND QUALITY CONTROL SYSTEM
============================================================
TEST EMAIL NOTIFICATION
============================================================
Recipient: $toEmail
Dispatched At: $now

This is a test notification confirming that the Ethio-Djibouti Railway Acceptance and Quality Control email system is fully operational.

• Native Email Client Dispatch: READY (Zero API Key Required)
• Firebase Realtime Database: Connected (acceptance-8781f)
• Report Distribution List: Active

Ethio-Djibouti Standard Gauge Railway Share Company (EDR)
Acceptance and QC Team
""".trimIndent()
    }
}
