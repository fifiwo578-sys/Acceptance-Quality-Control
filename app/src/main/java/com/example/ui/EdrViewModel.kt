package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.EdrRepository
import com.example.data.repository.ReportDataBundle
import com.example.util.ParsedInspectionItem
import com.example.util.PdfReportExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EdrViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = EdrRepository(database.edrDao())

    // Navigation & Auth
    val currentScreen = MutableStateFlow("dashboard")
    val isAdmin = MutableStateFlow(true) // Admin mode vs Public viewer mode
    val activeReportDate = MutableStateFlow<String?>(null)
    val activeReportBundle = MutableStateFlow<ReportDataBundle?>(null)
    val activeReportEntity = MutableStateFlow<DailyReportEntity?>(null)

    // Data Flows
    val allQC = repository.allQCRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTasks = repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reports = repository.allReports.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recipients = repository.allRecipients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notificationLogs = repository.allNotificationLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Trash & Cancelled
    val trashQC = repository.trashQC.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trashTasks = repository.trashTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cancelledQC = repository.cancelledQC.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cancelledTasks = repository.cancelledTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cloud Config
    val cloudConfig = MutableStateFlow(CloudConfigEntity())

    // UI Feedback
    val toastMessage = MutableStateFlow<String?>(null)
    val isBusy = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
            loadCloudConfig()
            checkAndSeedSampleData()
        }
    }

    private suspend fun loadCloudConfig() {
        cloudConfig.value = repository.getCloudConfig()
    }

    fun showToast(msg: String) {
        toastMessage.value = msg
    }

    fun clearToast() {
        toastMessage.value = null
    }

    fun navigateTo(screen: String) {
        currentScreen.value = screen
    }

    fun todayDateStr(): String = repository.todayStr()

    // === FIREBASE SYNC ===
    fun syncWithFirebase() {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val (success, message) = repository.syncAllWithFirebase()
                showToast(message)
            } catch (e: Exception) {
                showToast("Firebase sync error: ${e.message}")
            } finally {
                isBusy.value = false
            }
        }
    }

    // === DAILY REPORT ACTIONS ===
    fun createDailyReport(date: String = repository.todayStr()) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val report = repository.generateDailyReportForDate(date)
                showToast("Daily report compiled for $date")
                viewReport(report.reportDate)
            } catch (e: Exception) {
                showToast("Error creating report: ${e.message}")
            } finally {
                isBusy.value = false
            }
        }
    }

    fun viewReport(date: String) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val bundle = repository.getFullReportData(date)
                val reportEntity = repository.generateDailyReportForDate(date)
                activeReportDate.value = date
                activeReportBundle.value = bundle
                activeReportEntity.value = reportEntity
                currentScreen.value = "dailyReportView"
            } catch (e: Exception) {
                showToast("Could not load report: ${e.message}")
            } finally {
                isBusy.value = false
            }
        }
    }

    fun publishCurrentReport(reportId: Long) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val updated = repository.publishReport(reportId)
                if (updated != null) {
                    activeReportEntity.value = updated
                    showToast("Report published! Synced to Firebase & ready to share.")
                }
            } catch (e: Exception) {
                showToast("Publish error: ${e.message}")
            } finally {
                isBusy.value = false
            }
        }
    }

    fun sendEmailNotificationsForReport(reportId: Long) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val (count, msg) = repository.sendReportNotifications(reportId)
                showToast(msg)
                activeReportDate.value?.let { date ->
                    activeReportEntity.value = repository.generateDailyReportForDate(date)
                }
            } catch (e: Exception) {
                showToast("Notification failed: ${e.message}")
            } finally {
                isBusy.value = false
            }
        }
    }

    // === RECIPIENTS ===
    fun addRecipient(email: String, name: String) {
        viewModelScope.launch {
            if (email.isBlank()) {
                showToast("Email address is required")
                return@launch
            }
            repository.insertRecipient(email, name)
            showToast("Recipient added")
        }
    }

    fun toggleRecipientActive(recipient: RecipientEntity) {
        viewModelScope.launch {
            repository.toggleRecipient(recipient)
        }
    }

    fun deleteRecipient(id: Long) {
        viewModelScope.launch {
            repository.deleteRecipient(id)
            showToast("Recipient removed")
        }
    }

    fun sendTestNotification(toEmail: String) {
        viewModelScope.launch {
            isBusy.value = true
            val result = repository.sendTestEmail(toEmail)
            if (result.isSuccess) {
                showToast("Test notification dispatched to $toEmail")
            } else {
                showToast("Test notification: ${result.exceptionOrNull()?.message}")
            }
            isBusy.value = false
        }
    }

    /**
     * Direct Native Android Email Dispatch (No API Key Required)
     * Opens Gmail, Outlook, or installed email client with all recipients & formatted report
     */
    fun launchEmailComposerForReport(context: Context, report: DailyReportEntity) {
        viewModelScope.launch {
            val config = repository.getCloudConfig()
            val activeRecipients = repository.getActiveRecipients()
            val recipientEmails = if (activeRecipients.isNotEmpty()) {
                activeRecipients.map { it.email }
            } else {
                listOf(config.reportFromEmail.ifBlank { "reports@ethiodjiboutirailway.com" })
            }

            val subject = repository.getEmailSubjectForReport(report)
            val publicUrl = repository.getOrCreateWebViewUrl(report)
            val body = repository.getEmailBodyForReport(report, publicUrl)

            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, recipientEmails.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            try {
                context.startActivity(Intent.createChooser(mailIntent, "Send EDR Acceptance & QC Report"))
                // Also record logs and update report status
                repository.sendReportNotifications(report.id)
                showToast("Opening Email client with report for ${recipientEmails.size} recipients...")
            } catch (e: Exception) {
                // If mailto chooser fails, fallback to general share intent
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_EMAIL, recipientEmails.toTypedArray())
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, body)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Report via Email/Apps"))
                    repository.sendReportNotifications(report.id)
                } catch (ex: Exception) {
                    showToast("Could not launch email app: ${ex.message}")
                }
            }
        }
    }

    /**
     * Send official Daily Report in PDF form via Email with attached PDF and smart summary
     */
    fun sendReportPdfViaEmail(context: Context, report: DailyReportEntity) {
        viewModelScope.launch {
            val bundle = repository.getFullReportData(report.reportDate)
            val config = repository.getCloudConfig()
            val activeRecipients = repository.getActiveRecipients()
            val recipientEmails = if (activeRecipients.isNotEmpty()) {
                activeRecipients.map { it.email }
            } else {
                listOf(config.reportFromEmail.ifBlank { "reports@ethiodjiboutirailway.com" })
            }
            val publicUrl = repository.getOrCreateWebViewUrl(report, bundle)

            val result = PdfReportExporter.sendReportPdfViaEmail(
                context = context,
                report = report,
                data = bundle,
                recipients = recipientEmails,
                publicUrl = publicUrl
            )

            if (result.isSuccess) {
                repository.sendReportNotifications(report.id)
                showToast("Opening Email app with attached PDF report...")
            } else {
                showToast("Could not prepare PDF attachment: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Share Daily Report PDF across installed applications
     */
    fun shareReportPdf(context: Context, report: DailyReportEntity) {
        viewModelScope.launch {
            val bundle = repository.getFullReportData(report.reportDate)
            val publicUrl = repository.getOrCreateWebViewUrl(report, bundle)
            val result = PdfReportExporter.sharePdfFile(context, report, bundle, publicUrl)
            if (result.isSuccess) {
                showToast("Sharing Daily Report PDF...")
            } else {
                showToast("Error generating PDF: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Opens the Daily Report in an external web browser formatted exactly like the app
     */
    fun openWebReport(context: Context, report: DailyReportEntity) {
        viewModelScope.launch {
            val bundle = repository.getFullReportData(report.reportDate)
            val publicUrl = repository.getOrCreateWebViewUrl(report, bundle)
            val result = com.example.util.HtmlReportExporter.openHtmlReportInBrowser(context, report, bundle, publicUrl)
            if (result.isSuccess) {
                showToast("Opening web report...")
            } else {
                // If launching browser fails, open online URL directly
                openDirectUrl(context, publicUrl)
            }
        }
    }

    /**
     * Opens a direct web URL in the browser
     */
    fun openDirectUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open link: ${e.message}")
        }
    }

    /**
     * Clear all Daily Report history
     */
    fun clearReportHistory() {
        viewModelScope.launch {
            try {
                repository.clearAllReports()
                showToast("Report history cleared successfully")
            } catch (e: Exception) {
                showToast("Failed to clear reports: ${e.message}")
            }
        }
    }

    /**
     * Clear all Email Notification history logs
     */
    fun clearNotificationHistory() {
        viewModelScope.launch {
            try {
                repository.clearAllNotificationLogs()
                showToast("Notification history logs cleared successfully")
            } catch (e: Exception) {
                showToast("Failed to clear notification logs: ${e.message}")
            }
        }
    }

    /**
     * Direct Email to single recipient (No API Key Required)
     */
    fun launchSingleRecipientEmail(context: Context, toEmail: String, report: DailyReportEntity? = null) {
        viewModelScope.launch {
            val subject = if (report != null) {
                repository.getEmailSubjectForReport(report)
            } else {
                "Ethio-Djibouti Railway - Acceptance & QC Report Notification"
            }
            val body = if (report != null) {
                repository.getEmailBodyForReport(report)
            } else {
                "Dear Colleague,\n\nPlease find the latest rolling stock acceptance and quality control status for Ethio-Djibouti Railway.\n\nRegards,\nAcceptance and QC Team"
            }

            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${Uri.encode(toEmail)}")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            try {
                context.startActivity(Intent.createChooser(mailIntent, "Send Email to $toEmail"))
                repository.sendTestEmail(toEmail)
            } catch (e: Exception) {
                showToast("Could not open email app: ${e.message}")
            }
        }
    }

    /**
     * Launch Test Email Composer in User's Email App (No API Key Required)
     */
    fun launchTestEmailComposer(context: Context, toEmail: String) {
        viewModelScope.launch {
            val subject = "Ethio-Djibouti Railway - Test Notification"
            val body = repository.getTestEmailBody(toEmail)

            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${Uri.encode(toEmail)}")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            try {
                context.startActivity(Intent.createChooser(mailIntent, "Send Test Email"))
                repository.sendTestEmail(toEmail)
                showToast("Opening Email composer for $toEmail...")
            } catch (e: Exception) {
                showToast("Error opening email app: ${e.message}")
            }
        }
    }

    /**
     * Copy full formatted report text to clipboard
     */
    fun copyReportToClipboard(context: Context, report: DailyReportEntity) {
        viewModelScope.launch {
            val body = repository.getEmailBodyForReport(report)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("EDR Daily Report", body))
            showToast("Report text copied to clipboard!")
        }
    }

    /**
     * Share report across any app (WhatsApp, Telegram, SMS, Email, etc.) as an interactive Web View
     */
    fun shareReportViaApps(context: Context, report: DailyReportEntity) {
        viewModelScope.launch {
            val bundle = repository.getFullReportData(report.reportDate)
            val webUrl = repository.getOrCreateWebViewUrl(report, bundle)
            val subject = repository.getEmailSubjectForReport(report)
            val body = repository.getEmailBodyForReport(report, webUrl)

            val res = com.example.util.HtmlReportExporter.shareHtmlReportFile(
                context = context,
                report = report,
                data = bundle,
                publicUrl = webUrl,
                summaryText = body
            )
            if (res.isFailure) {
                // Fallback to text intent if file provider intent has issue
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Daily Report Web Link"))
            }
        }
    }

    /**
     * Copies the Web View URL of the report (never raw .json file) to the clipboard
     */
    fun copyReportWebLink(context: Context, report: DailyReportEntity) {
        viewModelScope.launch {
            val bundle = repository.getFullReportData(report.reportDate)
            val webUrl = repository.getOrCreateWebViewUrl(report, bundle)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("EDR Web View Link", webUrl))
            showToast("Web View link copied!")
        }
    }

    fun getEmailBodyForReport(report: DailyReportEntity): String = repository.getEmailBodyForReport(report)

    fun saveConfig(newConfig: CloudConfigEntity) {
        viewModelScope.launch {
            repository.saveCloudConfig(newConfig)
            cloudConfig.value = newConfig
            showToast("Settings saved successfully")
        }
    }

    // === MODULE CRUD OPERATIONS ===
    fun addQC(
        module: String, number: String, maintenanceType: String, desc: String,
        fault: String, sparePart: String, status: String, startDate: String, finishDate: String, remark: String
    ) {
        viewModelScope.launch {
            repository.insertQC(
                QCEntity(
                    module = module,
                    number = number,
                    maintenanceType = maintenanceType,
                    description = desc,
                    fault = fault,
                    sparePart = sparePart,
                    status = status,
                    startDate = startDate.ifBlank { repository.todayStr() },
                    finishDate = finishDate,
                    remark = remark
                )
            )
            showToast("QC Record saved")
        }
    }

    /**
     * Batch insert auto-arranged inspection records from parsed text notes
     */
    fun insertParsedInspectionRecords(
        records: List<ParsedInspectionItem>,
        overrideModule: String? = null
    ) {
        viewModelScope.launch {
            if (records.isEmpty()) {
                showToast("No records to save")
                return@launch
            }

            var savedCount = 0
            records.forEach { item ->
                val targetModule = overrideModule ?: item.module
                val entity = QCEntity(
                    id = 0L,
                    module = targetModule,
                    number = item.number,
                    maintenanceType = item.maintenanceType,
                    description = item.actionTaken,
                    fault = item.fault,
                    sparePart = item.spareParts,
                    status = item.status,
                    startDate = item.date,
                    finishDate = item.date,
                    remark = "${item.signOffTag} [${item.subsystem}] - EDR Acceptance Verification",
                    createdAt = repository.nowStr(),
                    updatedAt = repository.nowStr()
                )
                repository.insertQC(entity)
                savedCount++
            }

            if (savedCount == 1) {
                showToast("Saved record #${records.first().number} to database & cloud!")
            } else {
                showToast("Imported $savedCount auto-arranged records to database & cloud!")
            }
        }
    }

    fun updateQCRecord(item: QCEntity) {
        viewModelScope.launch {
            repository.updateQC(item)
            showToast("QC Record #${item.number} updated")
        }
    }

    fun addTask(module: String, date: String, desc: String, remark: String) {
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    module = module,
                    date = date.ifBlank { repository.todayStr() },
                    description = desc,
                    remark = remark
                )
            )
            showToast("Task saved")
        }
    }

    fun updateTaskRecord(item: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(item)
            showToast("Task updated")
        }
    }

    // Cancel / Trash / Reinstate / Restore
    fun moveToTrash(tableKey: String, id: Long, reason: String = "Manual deletion") = trashItem(tableKey, id, reason)

    fun trashItem(tableKey: String, id: Long, reason: String = "Manual deletion") {
        viewModelScope.launch {
            when (tableKey) {
                "qc" -> repository.trashQC(id, reason)
                "task" -> repository.trashTask(id, reason)
            }
            showToast("Moved to Trash")
        }
    }

    fun cancelItem(tableKey: String, id: Long, reason: String = "Inspection cancelled") {
        viewModelScope.launch {
            when (tableKey) {
                "qc" -> repository.cancelQC(id, reason)
                "task" -> repository.cancelTask(id, reason)
            }
            showToast("Record cancelled")
        }
    }

    fun reinstateItem(tableKey: String, id: Long) {
        viewModelScope.launch {
            when (tableKey) {
                "qc" -> repository.reinstateQC(id)
                "task" -> repository.reinstateTask(id)
            }
            showToast("Record reinstated")
        }
    }

    fun restoreFromTrash(tableKey: String, id: Long) {
        viewModelScope.launch {
            when (tableKey) {
                "qc" -> repository.restoreQC(id)
                "task" -> repository.restoreTask(id)
            }
            showToast("Restored from Trash")
        }
    }

    fun permanentDelete(tableKey: String, id: Long) {
        viewModelScope.launch {
            when (tableKey) {
                "qc" -> repository.permanentDeleteQC(id)
                "task" -> repository.permanentDeleteTask(id)
            }
            showToast("Permanently deleted")
        }
    }

    fun emptyAllTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            showToast("Trash archive emptied")
        }
    }

    fun cleanDuplicates() {
        viewModelScope.launch {
            val qc = repository.allQCRecords.first()
            val seen = mutableSetOf<String>()
            var dupes = 0
            qc.forEach { item ->
                val key = "${item.module}_${item.number.trim().lowercase()}"
                if (seen.contains(key)) {
                    repository.permanentDeleteQC(item.id)
                    dupes++
                } else {
                    seen.add(key)
                }
            }
            if (dupes > 0) {
                showToast("Cleaned $dupes duplicate records")
            } else {
                showToast("No duplicate records found")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllQC()
            repository.clearAllTasks()
            repository.emptyTrash()
            showToast("All records cleared from database")
        }
    }

    // === POPULATE REAL SAMPLE DATA ===
    fun loadRichSampleData() {
        viewModelScope.launch {
            val d = { daysAgo: Int ->
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
            }

            // QC Wagon
            repository.insertQC(QCEntity(module = "wagon", number = "W-1001", maintenanceType = "Preventive", description = "QC verification after brake rigging overhaul", fault = "None - fully verified", sparePart = "Brake shoes", status = "Completed", startDate = d(0), finishDate = d(0), remark = "Passed all static & dynamic tests"))
            repository.insertQC(QCEntity(module = "wagon", number = "W-1002", maintenanceType = "Corrective", description = "QC bearing temperature inspection", fault = "Bearing spec clearance check", sparePart = "SKF Bearing", status = "Completed", startDate = d(1), finishDate = d(0), remark = "Cleared for revenue freight service"))
            repository.insertQC(QCEntity(module = "wagon", number = "W-1003", maintenanceType = "Emergency", description = "QC door locking mechanism repair", fault = "Latch misalignment", sparePart = "Lock assembly", status = "In Progress", startDate = d(0), finishDate = "", remark = "Under inspection"))
            repository.insertQC(QCEntity(module = "wagon", number = "W-1004", maintenanceType = "Inspection", description = "Wagon wheel profile & flange wear verification", fault = "Flange thickness at 28mm", sparePart = "None", status = "Completed", startDate = d(2), finishDate = d(1), remark = "Acceptable tolerance"))

            // QC Locomotive
            repository.insertQC(QCEntity(module = "locomotive", number = "LC-001", maintenanceType = "Preventive", description = "QC sign-off after oil and filter change", fault = "None", sparePart = "Lube oil filters", status = "Completed", startDate = d(0), finishDate = d(0), remark = "Approved for main line service"))
            repository.insertQC(QCEntity(module = "locomotive", number = "LC-002", maintenanceType = "Corrective", description = "Traction motor bearing replacement check", fault = "Minor vibration noted", sparePart = "Pinion gear", status = "In Progress", startDate = d(1), finishDate = "", remark = "NCR issued to maintenance unit"))
            repository.insertQC(QCEntity(module = "locomotive", number = "LC-003", maintenanceType = "Emergency", description = "Brake air compressor overhaul sign-off", fault = "Pressure valve seal", sparePart = "Gasket kit", status = "Pending", startDate = d(0), finishDate = "", remark = "Priority inspection scheduled"))
            repository.insertQC(QCEntity(module = "locomotive", number = "LC-004", maintenanceType = "Inspection", description = "Bogie frame ultrasound non-destructive test", fault = "Weld seam nominal", sparePart = "None", status = "Completed", startDate = d(2), finishDate = d(1), remark = "Within tolerance"))

            // QC Coach
            repository.insertQC(QCEntity(module = "coach", number = "CH-001", maintenanceType = "Preventive", description = "QC passenger HVAC conditioning system", fault = "Refrigerant pressure low", sparePart = "R134a refill", status = "Completed", startDate = d(0), finishDate = d(0), remark = "Passenger comfort standards verified"))
            repository.insertQC(QCEntity(module = "coach", number = "CH-002", maintenanceType = "Corrective", description = "Passenger entrance plug door automation check", fault = "Obstacle detector sensor", sparePart = "Photocell sensor", status = "Completed", startDate = d(1), finishDate = d(0), remark = "Passed safety clearance"))
            repository.insertQC(QCEntity(module = "coach", number = "CH-003", maintenanceType = "Emergency", description = "Emergency exit window seal inspection", fault = "Gasket degradation", sparePart = "Rubber weatherstrip", status = "In Progress", startDate = d(0), finishDate = "", remark = "Seal replacement in progress"))

            // Departmental Tasks
            repository.insertTask(TaskEntity(module = "aqms", date = d(0), description = "Prepare weekly quality safety audit report for Acceptance and QC Team", remark = "Target submission: 17:00"))
            repository.insertTask(TaskEntity(module = "aqms", date = d(1), description = "Review acceptance protocol for newly arrived container flatcars", remark = "Documentation sign-off"))
            repository.insertTask(TaskEntity(module = "wagon", date = d(0), description = "Audit spare wheelset inventory in Dire Dawa workshop", remark = "High priority"))
            repository.insertTask(TaskEntity(module = "wagon", date = d(1), description = "Calibrate wagon brake test bench pressure sensors", remark = "Calibration certified"))
            repository.insertTask(TaskEntity(module = "locomotive", date = d(0), description = "Calibrate locomotive speed recording units (TELOC)", remark = "Coordinate with signal team"))
            repository.insertTask(TaskEntity(module = "coach", date = d(0), description = "Interior hygiene & sanitary inspection for passenger cars", remark = "Addis to Djibouti express"))
            repository.insertTask(TaskEntity(module = "equipment", date = d(0), description = "Monthly crane and lifting tackle load test certification", remark = "Workshop bay 2"))

            // Generate today's report
            repository.generateDailyReportForDate(d(0))
            // Generate yesterday's report for viewing other reports
            val yestReport = repository.generateDailyReportForDate(d(1))
            repository.publishReport(yestReport.id)

            showToast("Loaded Acceptance and Quality Control records")
        }
    }

    private suspend fun checkAndSeedSampleData() {
        val qcList = repository.allQCRecords.first()
        if (qcList.isEmpty()) {
            loadRichSampleData()
        }
    }
}
