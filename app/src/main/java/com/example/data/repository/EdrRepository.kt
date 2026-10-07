package com.example.data.repository

import com.example.data.db.EdrDao
import com.example.data.model.*
import com.example.data.remote.CloudAndEmailService
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class EdrRepository(
    private val dao: EdrDao,
    private val cloudService: CloudAndEmailService = CloudAndEmailService()
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // === FLOWS ===
    val allQCRecords: Flow<List<QCEntity>> = dao.getAllQCRecords()
    val allTasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    val allReports: Flow<List<DailyReportEntity>> = dao.getAllReports()
    val allRecipients: Flow<List<RecipientEntity>> = dao.getAllRecipients()
    val allNotificationLogs: Flow<List<NotificationLogEntity>> = dao.getAllNotificationLogs()

    val trashQC: Flow<List<QCEntity>> = dao.getTrashQC()
    val trashTasks: Flow<List<TaskEntity>> = dao.getTrashTasks()

    val cancelledQC: Flow<List<QCEntity>> = dao.getCancelledQC()
    val cancelledTasks: Flow<List<TaskEntity>> = dao.getCancelledTasks()

    fun getQCByModule(module: String): Flow<List<QCEntity>> = dao.getQCRecords(module)
    fun getTasksByModule(module: String): Flow<List<TaskEntity>> = dao.getTasks(module)

    // === SEED INITIAL SAMPLES IF EMPTY ===
    suspend fun seedIfEmpty() {
        val cfg = dao.getCloudConfig()
        if (cfg == null || cfg.firebaseDatabaseUrl != "https://acceptance-8781f-default-rtdb.firebaseio.com" || cfg.appPublicUrl.contains("railway.et")) {
            dao.saveCloudConfig(
                (cfg ?: CloudConfigEntity()).copy(
                    id = 1,
                    firebaseProjectId = "acceptance-8781f",
                    firebaseDatabaseUrl = "https://acceptance-8781f-default-rtdb.firebaseio.com",
                    reportFromEmail = cfg?.reportFromEmail?.ifBlank { "reports@ethiodjiboutirailway.com" } ?: "reports@ethiodjiboutirailway.com",
                    appPublicUrl = "https://acceptance-8781f-default-rtdb.firebaseio.com"
                )
            )
        }

        // Clean up any historical reports with invalid dummy URLs (e.g. railway.et that causes NXDOMAIN)
        val reports = dao.getReportList()
        for (r in reports) {
            if (r.publicUrl.isBlank() || r.publicUrl.contains("railway.et")) {
                val validUrl = "https://acceptance-8781f-default-rtdb.firebaseio.com/reports/${r.reportDate}.json"
                dao.updateReportUrl(r.id, validUrl)
            }
        }
        // Seed default recipients
        val activeRecipients = dao.getActiveRecipients()
        if (activeRecipients.isEmpty()) {
            dao.insertRecipients(
                listOf(
                    RecipientEntity(email = "quality.director@railway.et", name = "Director of Acceptance & QC", isActive = true, addedAt = nowStr()),
                    RecipientEntity(email = "chief.inspector@railway.et", name = "Chief Quality Inspector", isActive = true, addedAt = nowStr()),
                    RecipientEntity(email = "operations@ethiodjibouti.com", name = "Railway Operations Control", isActive = true, addedAt = nowStr())
                )
            )
        }
    }

    // === DAILY REPORT AGGREGATION & CREATION ===
    suspend fun generateDailyReportForDate(date: String): DailyReportEntity {
        val qcWagon = dao.getQCByDate("wagon", date)
        val qcLoco = dao.getQCByDate("locomotive", date)
        val qcCoach = dao.getQCByDate("coach", date)
        val taskAqms = dao.getTasksByDate("aqms", date)
        val taskWagon = dao.getTasksByDate("wagon", date)
        val taskLoco = dao.getTasksByDate("locomotive", date)
        val taskCoach = dao.getTasksByDate("coach", date)
        val taskEquipment = dao.getTasksByDate("equipment", date)

        val totalQC = qcWagon.size + qcLoco.size + qcCoach.size
        val totalTasks = taskAqms.size + taskWagon.size + taskLoco.size + taskCoach.size + taskEquipment.size
        val totalRecords = totalQC + totalTasks

        val enteredQCParts = mutableListOf<String>()
        if (qcWagon.isNotEmpty()) enteredQCParts.add("Wagon: ${qcWagon.size}")
        if (qcLoco.isNotEmpty()) enteredQCParts.add("Locomotive: ${qcLoco.size}")
        if (qcCoach.isNotEmpty()) enteredQCParts.add("Coach: ${qcCoach.size}")

        val enteredTaskParts = mutableListOf<String>()
        if (taskAqms.isNotEmpty()) enteredTaskParts.add("AQMS: ${taskAqms.size}")
        if (taskWagon.isNotEmpty()) enteredTaskParts.add("Wagon: ${taskWagon.size}")
        if (taskLoco.isNotEmpty()) enteredTaskParts.add("Locomotive: ${taskLoco.size}")
        if (taskCoach.isNotEmpty()) enteredTaskParts.add("Coach: ${taskCoach.size}")
        if (taskEquipment.isNotEmpty()) enteredTaskParts.add("Equipment: ${taskEquipment.size}")

        val summaryText = if (totalRecords == 0) {
            "All rolling stock quality control parameters and acceptance verification checklists operated nominally for $date. No technical non-conformances identified."
        } else {
            val qcInfo = if (enteredQCParts.isNotEmpty()) "verified $totalQC quality control maintenance sign-offs (${enteredQCParts.joinToString(", ")})" else ""
            val taskInfo = if (enteredTaskParts.isNotEmpty()) "and $totalTasks departmental tasks (${enteredTaskParts.joinToString(", ")})" else ""
            "Comprehensive Acceptance and Quality Control review for $date: ${listOf(qcInfo, taskInfo).filter { it.isNotBlank() }.joinToString(" ")}."
        }

        val existingReport = dao.getReportByDate(date)
        val report = DailyReportEntity(
            id = existingReport?.id ?: 0,
            reportDate = date,
            status = existingReport?.status ?: "Draft",
            title = "Ethio-Djibouti Railway Acceptance and Quality Control Daily Report",
            executiveSummary = summaryText,
            qcWagonCount = qcWagon.size,
            qcLocoCount = qcLoco.size,
            qcCoachCount = qcCoach.size,
            aqmsTaskCount = taskAqms.size,
            wagonTaskCount = taskWagon.size,
            locoTaskCount = taskLoco.size,
            coachTaskCount = taskCoach.size,
            equipmentTaskCount = taskEquipment.size,
            statsJson = JSONObject().apply {
                put("totalRecords", totalRecords)
                put("totalQC", totalQC)
                put("completedQC", (qcWagon + qcLoco + qcCoach).count { it.status == "Completed" })
            }.toString(),
            remarks = "Standard daily sign-off by EDR Acceptance and QC Team.",
            createdBy = "Administrator",
            publishedAt = existingReport?.publishedAt ?: "",
            publicUrl = if (existingReport?.publicUrl.isNullOrBlank() || existingReport?.publicUrl?.endsWith(".json") == true) {
                ""
            } else {
                existingReport!!.publicUrl
            },
            notificationSentAt = existingReport?.notificationSentAt ?: "",
            createdAt = existingReport?.createdAt ?: nowStr(),
            updatedAt = nowStr()
        )

        val id = dao.insertReport(report)
        return report.copy(id = if (report.id == 0L) id else report.id)
    }

    suspend fun getOrCreateWebViewUrl(report: DailyReportEntity, bundle: ReportDataBundle? = null): String {
        if (report.publicUrl.isNotBlank() && !report.publicUrl.endsWith(".json") && !report.publicUrl.contains("firebaseio.com")) {
            return report.publicUrl
        }
        val fullBundle = bundle ?: getFullReportData(report.reportDate)
        val res = cloudService.publishReportToOnlineWebView(report, fullBundle)
        if (res.isSuccess) {
            val webUrl = res.getOrThrow()
            val updated = report.copy(publicUrl = webUrl)
            dao.insertReport(updated)
            val cfg = dao.getCloudConfig()
            if (cfg != null) {
                cloudService.syncReportToFirebase(cfg.firebaseDatabaseUrl, updated)
            }
            return webUrl
        }
        val config = dao.getCloudConfig()
        val baseUrl = if (config?.appPublicUrl.isNullOrBlank() || config?.appPublicUrl?.contains("railway.et") == true) {
            "https://acceptance-8781f-default-rtdb.firebaseio.com"
        } else {
            config!!.appPublicUrl
        }
        return "${baseUrl.trimEnd('/')}/reports/${report.reportDate}"
    }

    suspend fun publishReport(reportId: Long): DailyReportEntity? {
        val report = dao.getReportById(reportId) ?: return null
        val bundle = getFullReportData(report.reportDate)
        val config = dao.getCloudConfig()

        // Publish to online Web View so shared link opens in Web View rather than .json file
        val webViewRes = cloudService.publishReportToOnlineWebView(report, bundle)
        val publicUrl = if (webViewRes.isSuccess) {
            webViewRes.getOrThrow()
        } else {
            val baseUrl = if (config?.appPublicUrl.isNullOrBlank() || config?.appPublicUrl?.contains("railway.et") == true) {
                "https://acceptance-8781f-default-rtdb.firebaseio.com"
            } else {
                config!!.appPublicUrl
            }
            "${baseUrl.trimEnd('/')}/reports/${report.reportDate}"
        }

        val updated = report.copy(
            status = "Published",
            publishedAt = nowStr(),
            publicUrl = publicUrl,
            updatedAt = nowStr()
        )
        dao.updateReport(updated)

        // Sync with Firebase Cloud Database
        if (config != null) {
            cloudService.syncReportToFirebase(config.firebaseDatabaseUrl, updated)
        }

        return updated
    }

    suspend fun sendReportNotifications(reportId: Long): Pair<Int, String> {
        val report = dao.getReportById(reportId) ?: return Pair(0, "Report not found")
        val config = dao.getCloudConfig() ?: return Pair(0, "Email configuration missing")
        val recipients = dao.getActiveRecipients()

        if (recipients.isEmpty()) {
            return Pair(0, "No active recipients configured in Email Notifications.")
        }

        var successCount = 0
        var lastError = ""

        recipients.forEach { recipient ->
            val result = cloudService.sendDailyReportEmail(
                apiKey = config.resendApiKey,
                fromEmail = config.reportFromEmail,
                recipient = recipient,
                report = report,
                publicUrl = if (report.publicUrl.isBlank() || report.publicUrl.contains("railway.et")) {
                    "https://acceptance-8781f-default-rtdb.firebaseio.com/reports/${report.reportDate}.json"
                } else {
                    report.publicUrl
                }
            )

            val log = NotificationLogEntity(
                reportId = report.id,
                reportDate = report.reportDate,
                recipientEmail = recipient.email,
                sentAt = nowStr(),
                status = if (result.isSuccess) {
                    if (config.resendApiKey.isNotBlank()) "Sent" else "Dispatched via Mail App"
                } else "Failed",
                errorMessage = result.exceptionOrNull()?.message ?: ""
            )
            dao.insertNotificationLog(log)

            if (result.isSuccess) {
                successCount++
            } else {
                lastError = result.exceptionOrNull()?.message ?: "Send failure"
            }
        }

        if (successCount > 0) {
            dao.updateReport(
                report.copy(
                    status = "Notification Sent",
                    notificationSentAt = nowStr()
                )
            )
            val msg = if (config.resendApiKey.isNotBlank()) {
                "Sent $successCount emails successfully via Cloud API."
            } else {
                "Dispatched notifications for $successCount active recipients."
            }
            return Pair(successCount, msg)
        } else {
            return Pair(0, "Email send attempt finished. Detail: $lastError")
        }
    }

    // === DATA DETAILS FOR REPORT VIEW ===
    suspend fun getFullReportData(date: String): ReportDataBundle {
        return ReportDataBundle(
            date = date,
            qcWagon = dao.getQCByDate("wagon", date),
            qcLoco = dao.getQCByDate("locomotive", date),
            qcCoach = dao.getQCByDate("coach", date),
            taskAqms = dao.getTasksByDate("aqms", date),
            taskWagon = dao.getTasksByDate("wagon", date),
            taskLoco = dao.getTasksByDate("locomotive", date),
            taskCoach = dao.getTasksByDate("coach", date),
            taskEquipment = dao.getTasksByDate("equipment", date)
        )
    }

    // === FIREBASE CLOUD SYNC ===
    suspend fun syncAllWithFirebase(): Pair<Boolean, String> {
        val config = dao.getCloudConfig() ?: return Pair(false, "Config missing")
        val result = cloudService.fetchReportsFromFirebase(config.firebaseDatabaseUrl)
        return if (result.isSuccess) {
            val remoteReports = result.getOrNull() ?: emptyList()
            remoteReports.forEach { dao.insertReport(it) }
            Pair(true, "Synchronized ${remoteReports.size} reports from Firebase")
        } else {
            Pair(false, result.exceptionOrNull()?.message ?: "Firebase sync error")
        }
    }

    // === CRUD / TRASH / CANCEL OPERATIONS ===
    suspend fun insertQC(item: QCEntity): Long {
        val withTime = item.copy(createdAt = nowStr(), updatedAt = nowStr())
        val id = dao.insertQC(withTime)
        val config = dao.getCloudConfig()
        if (config != null) {
            cloudService.syncQCRecordToFirebase(config.firebaseDatabaseUrl, withTime.copy(id = if (withTime.id == 0L) id else withTime.id))
        }
        return id
    }

    suspend fun updateQC(item: QCEntity) {
        val withTime = item.copy(updatedAt = nowStr())
        dao.updateQC(withTime)
        val config = dao.getCloudConfig()
        if (config != null) {
            cloudService.syncQCRecordToFirebase(config.firebaseDatabaseUrl, withTime)
        }
    }

    suspend fun trashQC(id: Long, reason: String) = dao.trashQC(id, reason, nowStr())
    suspend fun cancelQC(id: Long, reason: String) = dao.cancelQC(id, reason, nowStr())
    suspend fun reinstateQC(id: Long) = dao.reinstateQC(id)
    suspend fun restoreQC(id: Long) = dao.restoreQC(id)
    suspend fun permanentDeleteQC(id: Long) = dao.permanentDeleteQC(id)

    suspend fun insertTask(item: TaskEntity): Long {
        val withTime = item.copy(createdAt = nowStr(), updatedAt = nowStr())
        val id = dao.insertTask(withTime)
        val config = dao.getCloudConfig()
        if (config != null) {
            cloudService.syncTaskToFirebase(config.firebaseDatabaseUrl, withTime.copy(id = if (withTime.id == 0L) id else withTime.id))
        }
        return id
    }

    suspend fun updateTask(item: TaskEntity) {
        val withTime = item.copy(updatedAt = nowStr())
        dao.updateTask(withTime)
        val config = dao.getCloudConfig()
        if (config != null) {
            cloudService.syncTaskToFirebase(config.firebaseDatabaseUrl, withTime)
        }
    }
    suspend fun trashTask(id: Long, reason: String) = dao.trashTask(id, reason, nowStr())
    suspend fun cancelTask(id: Long, reason: String) = dao.cancelTask(id, reason, nowStr())
    suspend fun reinstateTask(id: Long) = dao.reinstateTask(id)
    suspend fun restoreTask(id: Long) = dao.restoreTask(id)
    suspend fun permanentDeleteTask(id: Long) = dao.permanentDeleteTask(id)

    suspend fun emptyTrash() {
        dao.emptyTrashQC()
        dao.emptyTrashTasks()
    }

    suspend fun clearAllQC() = dao.clearAllQC()
    suspend fun clearAllTasks() = dao.clearAllTasks()

    // === RECIPIENTS ===
    suspend fun insertRecipient(email: String, name: String) = dao.insertRecipient(
        RecipientEntity(email = email, name = name, isActive = true, addedAt = nowStr())
    )
    suspend fun toggleRecipient(recipient: RecipientEntity) = dao.updateRecipient(
        recipient.copy(isActive = !recipient.isActive)
    )
    suspend fun deleteRecipient(id: Long) = dao.deleteRecipient(id)
    suspend fun getActiveRecipients(): List<RecipientEntity> = dao.getActiveRecipients()
    suspend fun getReportById(id: Long): DailyReportEntity? = dao.getReportById(id)
    suspend fun getReportByDate(date: String): DailyReportEntity? = dao.getReportByDate(date)
    suspend fun clearAllReports() = dao.clearAllReports()
    suspend fun clearAllNotificationLogs() = dao.clearAllNotificationLogs()

    suspend fun sendTestEmail(toEmail: String): Result<String> {
        val config = dao.getCloudConfig() ?: return Result.failure(Exception("Configuration not found"))
        val res = cloudService.sendTestEmail(config.resendApiKey, config.reportFromEmail, toEmail)
        if (res.isSuccess) {
            dao.insertNotificationLog(
                NotificationLogEntity(
                    reportId = 0L,
                    reportDate = todayStr(),
                    recipientEmail = toEmail,
                    sentAt = nowStr(),
                    status = if (config.resendApiKey.isNotBlank()) "Sent" else "Dispatched via Mail App",
                    errorMessage = if (config.resendApiKey.isNotBlank()) "" else "Native email dispatch"
                )
            )
        }
        return res
    }

    fun getEmailSubjectForReport(report: DailyReportEntity): String {
        return "Ethio-Djibouti Railway - Acceptance and Quality Control Daily Report - ${report.reportDate}"
    }

    fun getEmailBodyForReport(report: DailyReportEntity, publicUrl: String? = null): String {
        val url = publicUrl ?: report.publicUrl.ifBlank { "https://acceptance-8781f-default-rtdb.firebaseio.com/reports/${report.reportDate}" }
        return cloudService.buildEmailPlainText(report, url)
    }

    fun getTestEmailBody(toEmail: String): String {
        return cloudService.buildTestEmailPlainText(toEmail)
    }

    // === CLOUD CONFIG ===
    suspend fun getCloudConfig(): CloudConfigEntity = dao.getCloudConfig() ?: CloudConfigEntity()
    suspend fun saveCloudConfig(config: CloudConfigEntity) = dao.saveCloudConfig(config)

    // === UTILS ===
    fun todayStr(): String = dateFormat.format(Date())
    fun nowStr(): String = timeFormat.format(Date())
}

data class ReportDataBundle(
    val date: String,
    val qcWagon: List<QCEntity>,
    val qcLoco: List<QCEntity>,
    val qcCoach: List<QCEntity>,
    val taskAqms: List<TaskEntity>,
    val taskWagon: List<TaskEntity>,
    val taskLoco: List<TaskEntity>,
    val taskCoach: List<TaskEntity>,
    val taskEquipment: List<TaskEntity>
)
