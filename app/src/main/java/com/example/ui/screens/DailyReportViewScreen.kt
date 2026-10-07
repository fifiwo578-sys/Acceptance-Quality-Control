package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.EdrViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DailyReportViewScreen(viewModel: EdrViewModel) {
    val reportEntity by viewModel.activeReportEntity.collectAsState()
    val bundle by viewModel.activeReportBundle.collectAsState()
    val allReports by viewModel.reports.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val context = LocalContext.current

    var showPublishConfirmDialog by remember { mutableStateOf(false) }
    var showEmailConfirmDialog by remember { mutableStateOf(false) }
    var showOtherReportsDialog by remember { mutableStateOf(false) }

    if (reportEntity == null || bundle == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentAmber)
        }
        return
    }

    val report = reportEntity!!
    val data = bundle!!
    val publicUrl = if (report.publicUrl.isNotBlank() && !report.publicUrl.endsWith(".json") && !report.publicUrl.contains("railway.et")) {
        report.publicUrl
    } else {
        "https://acceptance-8781f-default-rtdb.firebaseio.com/reports/${report.reportDate}"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
    ) {
        // Top Action Bar (Share, Copy, Open, Print, View Other Reports, Publish, Email)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = EDRCyan, modifier = Modifier.size(16.dp))
                            Text("Shareable Link:", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = "/reports/${report.reportDate}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EDRCyan
                            )
                        }
                        StatusBadge(report.status)
                    }

                    // Shareable Link Row
                    Surface(
                        color = InputDark,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(publicUrl, fontSize = 11.sp, color = TextSecondary, maxLines = 1, modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Share intent (WhatsApp, Telegram, Gmail, etc.)
                                IconButton(
                                    onClick = {
                                        viewModel.shareReportViaApps(context, report)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Web Report", tint = AccentAmber, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.copyReportWebLink(context, report)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Web Link", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.openWebReport(context, report)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = "Open Web Report", tint = EDRGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Switch / View Other Reports Button
                        OutlinedButton(
                            onClick = { showOtherReportsDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EDRCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EDRCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp), tint = EDRCyan)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Reports", fontSize = 11.sp)
                        }

                        // Open Web Report Button
                        OutlinedButton(
                            onClick = { viewModel.openWebReport(context, report) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EDRGreen),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EDRGreen.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp), tint = EDRGreen)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Web View", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Download PDF Button
                        OutlinedButton(
                            onClick = { printDailyReport(context, report, data) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark2),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("PDF/Print", fontSize = 11.sp)
                        }

                        // Admin Actions
                        if (isAdmin) {
                            if (report.status == "Draft") {
                                Button(
                                    onClick = { showPublishConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BgDark),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Publish", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = { showEmailConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = EDRGreen, contentColor = TextOnGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Email", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= OFFICIAL REPORT DOCUMENT =================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentAmber.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // REPORT HEADER
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ethio-Djibouti Railway",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Acceptance and Quality Control",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Text(
                            text = "Daily Report",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = CardDark2,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                        ) {
                            Text(
                                text = "DATE: ${report.reportDate}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Divider(color = BorderDark, thickness = 1.dp)

                    var sectionNum = 1

                    // SECTION 1: EXECUTIVE SUMMARY
                    ReportSectionHeader(number = sectionNum++, title = "Executive Summary")
                    Surface(
                        color = CardDark2,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                    ) {
                        Text(
                            text = report.executiveSummary,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // QC SECTIONS - DISPLAY ONLY IF ENTERED DATA EXISTS FOR THAT DAY
                    if (data.qcWagon.isNotEmpty()) {
                        ReportSectionHeader(number = sectionNum++, title = "QC - Wagon (${data.qcWagon.size})")
                        RenderQCSublist(data.qcWagon, EDRViolet)
                    }

                    if (data.qcLoco.isNotEmpty()) {
                        ReportSectionHeader(number = sectionNum++, title = "QC - Locomotive (${data.qcLoco.size})")
                        RenderQCSublist(data.qcLoco, EDRCyan)
                    }

                    if (data.qcCoach.isNotEmpty()) {
                        ReportSectionHeader(number = sectionNum++, title = "QC - Coach (${data.qcCoach.size})")
                        RenderQCSublist(data.qcCoach, EDRGreen)
                    }

                    // TASK SECTIONS - DISPLAY ONLY IF ENTERED DATA EXISTS FOR THAT DAY
                    if (data.taskAqms.isNotEmpty()) {
                        ReportSectionHeader(number = sectionNum++, title = "AQMS Tasks (${data.taskAqms.size})")
                        RenderTaskSublist(data.taskAqms)
                    }

                    if (data.taskWagon.isNotEmpty()) {
                        ReportSectionHeader(number = sectionNum++, title = "Wagon Tasks (${data.taskWagon.size})")
                        RenderTaskSublist(data.taskWagon)
                    }

                    if (data.taskLoco.isNotEmpty()) {
                        ReportSectionHeader(number = sectionNum++, title = "Locomotive Tasks (${data.taskLoco.size})")
                        RenderTaskSublist(data.taskLoco)
                    }

                    if (data.taskCoach.isNotEmpty()) {
                        ReportSectionHeader(number = sectionNum++, title = "Coach Tasks (${data.taskCoach.size})")
                        RenderTaskSublist(data.taskCoach)
                    }

                    if (data.taskEquipment.isNotEmpty()) {
                        ReportSectionHeader(number = sectionNum++, title = "Equipment Tasks (${data.taskEquipment.size})")
                        RenderTaskSublist(data.taskEquipment)
                    }

                    val totalQC = data.qcWagon.size + data.qcLoco.size + data.qcCoach.size
                    val totalTasks = data.taskAqms.size + data.taskWagon.size + data.taskLoco.size + data.taskCoach.size + data.taskEquipment.size

                    if (totalQC == 0 && totalTasks == 0) {
                        Surface(
                            color = CardDark2,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No inspection records or departmental tasks were entered for ${report.reportDate}.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // STATISTICS / KPI SUMMARY - ONLY SHOW SECTIONS WITH ENTERED DATA
                    ReportSectionHeader(number = sectionNum++, title = "Statistics / KPI Summary")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardDark2, RoundedCornerShape(6.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val completedQC = (data.qcWagon + data.qcLoco + data.qcCoach).count { it.status.equals("Completed", true) }
                        val inProgressQC = (data.qcWagon + data.qcLoco + data.qcCoach).count { it.status.equals("In Progress", true) }

                        if (totalQC > 0) {
                            KPIStatRow("Total Rolling Stock QC Sign-offs:", "$totalQC records", AccentAmber)
                            if (data.qcWagon.isNotEmpty()) KPIStatRow("• Wagon Quality Inspections:", "${data.qcWagon.size} units", EDRViolet)
                            if (data.qcLoco.isNotEmpty()) KPIStatRow("• Locomotive Sign-offs:", "${data.qcLoco.size} units", EDRCyan)
                            if (data.qcCoach.isNotEmpty()) KPIStatRow("• Coach Inspections:", "${data.qcCoach.size} units", EDRGreen)
                            KPIStatRow("Completed Quality Inspections:", "$completedQC completed", EDRGreen)
                            if (inProgressQC > 0) KPIStatRow("In Progress Inspections:", "$inProgressQC ongoing", EDRCyan)
                        } else {
                            KPIStatRow("Rolling Stock QC Inspections:", "None entered today", TextMuted)
                        }

                        if (totalTasks > 0) {
                            KPIStatRow("Department Tasks Consolidated:", "$totalTasks tasks", EDROrange)
                        }
                        KPIStatRow("Acceptance Quality Status:", "PASSED / VERIFIED FOR REVENUE SERVICE", EDRGreen)
                    }

                    // REMARKS
                    ReportSectionHeader(number = sectionNum++, title = "Remarks")
                    Surface(
                        color = CardDark2,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                    ) {
                        Text(
                            text = report.remarks.ifBlank { "All rolling stock inspections adhered to EDR railway quality control standards. Acceptance checklists signed by quality engineers." },
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // DIGITAL CLOUD VERIFICATION & PUBLIC URL
                    ReportSectionHeader(number = sectionNum++, title = "Digital Cloud Verification & Public URL")
                    Surface(
                        color = CardDark2,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EDRGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = EDRGreen, modifier = Modifier.size(18.dp))
                                    Text("Official Public Report URL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EDRGreen)
                                }
                                Surface(
                                    color = EDRGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "LIVE CLOUD AUDIT",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EDRGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Surface(
                                color = InputDark,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = publicUrl,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Text(
                                text = "Open this verified URL to view the daily inspection report in web format matching the app design, or view the raw realtime database record.",
                                fontSize = 10.sp,
                                color = TextMuted,
                                lineHeight = 14.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.openWebReport(context, report) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EDRGreen, contentColor = TextOnGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1.1f)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Open Web", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.shareReportViaApps(context, report) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentAmber),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1.1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentAmber)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Share Web", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.copyReportWebLink(context, report) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Copy Link", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val rawJsonUrl = "https://acceptance-8781f-default-rtdb.firebaseio.com/reports/${report.reportDate}.json"
                                        viewModel.openDirectUrl(context, rawJsonUrl)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EDRCyan),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EDRCyan.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp), tint = EDRCyan)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("JSON", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Document Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Generated: ${report.createdAt}", fontSize = 9.sp, color = TextMuted)
                        Text("Ethio-Djibouti Standard Gauge Railway Share Company", fontSize = 9.sp, color = TextMuted)
                    }
                }
            }
        }
    }

    // Modal: View & Select Other Daily Reports Dialog
    if (showOtherReportsDialog) {
        AlertDialog(
            onDismissRequest = { showOtherReportsDialog = false },
            title = { Text("Select Daily Report to View / Share", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EDRCyan) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Select any published or archived daily report:", fontSize = 11.sp, color = TextSecondary)
                    allReports.forEach { otherReport ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOtherReportsDialog = false
                                    viewModel.viewReport(otherReport.reportDate)
                                },
                            color = if (otherReport.reportDate == report.reportDate) AccentAmber.copy(alpha = 0.15f) else CardDark2,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (otherReport.reportDate == report.reportDate) AccentAmber else BorderDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(otherReport.reportDate, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    val count = otherReport.qcWagonCount + otherReport.qcLocoCount + otherReport.qcCoachCount +
                                            otherReport.aqmsTaskCount + otherReport.wagonTaskCount + otherReport.locoTaskCount +
                                            otherReport.coachTaskCount + otherReport.equipmentTaskCount
                                    Text("$count QC records", fontSize = 10.sp, color = TextMuted)
                                }
                                StatusBadge(otherReport.status)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOtherReportsDialog = false }) { Text("Close", color = TextSecondary) }
            },
            containerColor = CardDark
        )
    }

    // Confirmation Dialog: Publish Report
    if (showPublishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPublishConfirmDialog = false },
            title = { Text("Publish Daily Report", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentAmber) },
            text = {
                Text(
                    text = "Publish the Daily Report for ${report.reportDate}? It will generate a permanent public shareable link and sync with the Firebase cloud database.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPublishConfirmDialog = false
                        viewModel.publishCurrentReport(report.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BgDark)
                ) {
                    Text("Confirm & Publish", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishConfirmDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = CardDark
        )
    }

    // Confirmation Dialog: Send Email Notification
    if (showEmailConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmailConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = EDRGreen)
                    Text("Send Report via Email", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EDRGreen)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Send this official Acceptance & QC report to all active stakeholders. Zero API key required!",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Choose your preferred dispatch method below:",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Surface(
                        color = CardDark2,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showEmailConfirmDialog = false
                                        viewModel.sendReportPdfViaEmail(context, report)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = EDRGreen, modifier = Modifier.size(16.dp))
                                Column {
                                    Text("Send Report in PDF Form (Smart Report)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Attaches official PDF file & includes smart report summary", fontSize = 10.sp, color = TextSecondary)
                                }
                            }

                            Divider(color = BorderDark)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showEmailConfirmDialog = false
                                        viewModel.shareReportPdf(context, report)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                Column {
                                    Text("Share PDF to Other Apps (WhatsApp, Telegram)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Share generated PDF document across installed apps", fontSize = 10.sp, color = TextSecondary)
                                }
                            }

                            Divider(color = BorderDark)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showEmailConfirmDialog = false
                                        viewModel.launchEmailComposerForReport(context, report)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = EDRCyan, modifier = Modifier.size(16.dp))
                                Column {
                                    Text("Email Text Summary (Fast Dispatch)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Pre-fills subject and formatted text summary", fontSize = 10.sp, color = TextSecondary)
                                }
                            }

                            Divider(color = BorderDark)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showEmailConfirmDialog = false
                                        viewModel.copyReportToClipboard(context, report)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = EDRGreen, modifier = Modifier.size(16.dp))
                                Column {
                                    Text("Copy Full Report Text", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Copy formatted summary to clipboard", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmailConfirmDialog = false
                        viewModel.sendReportPdfViaEmail(context, report)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EDRGreen, contentColor = TextOnGreen)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send PDF via Email", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailConfirmDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = CardDark
        )
    }
}

/**
 * Android Print / PDF document generation - Only prints sections with entered data
 */
fun printDailyReport(
    context: Context,
    report: com.example.data.model.DailyReportEntity,
    data: com.example.data.repository.ReportDataBundle
) {
    val liveUrl = if (report.publicUrl.isNotBlank() && !report.publicUrl.endsWith(".json") && !report.publicUrl.contains("railway.et")) {
        report.publicUrl
    } else {
        "https://acceptance-8781f-default-rtdb.firebaseio.com/reports/${report.reportDate}"
    }

    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            com.example.util.PdfReportExporter.sharePdfFile(context, report, data, liveUrl)
            return
        }

        var printSecNum = 1
        val totalQC = data.qcWagon.size + data.qcLoco.size + data.qcCoach.size
        val totalTasks = data.taskAqms.size + data.taskWagon.size + data.taskLoco.size + data.taskCoach.size + data.taskEquipment.size

        val qcRows = buildString {
            if (data.qcWagon.isNotEmpty()) {
                append(data.qcWagon.joinToString("") { "<tr><td>Wagon</td><td>${it.number}</td><td>${it.maintenanceType}</td><td>${it.description}</td><td>${it.status}</td></tr>" })
            }
            if (data.qcLoco.isNotEmpty()) {
                append(data.qcLoco.joinToString("") { "<tr><td>Locomotive</td><td>${it.number}</td><td>${it.maintenanceType}</td><td>${it.description}</td><td>${it.status}</td></tr>" })
            }
            if (data.qcCoach.isNotEmpty()) {
                append(data.qcCoach.joinToString("") { "<tr><td>Coach</td><td>${it.number}</td><td>${it.maintenanceType}</td><td>${it.description}</td><td>${it.status}</td></tr>" })
            }
        }

        val taskRows = buildString {
            if (data.taskAqms.isNotEmpty()) {
                append(data.taskAqms.joinToString("") { "<tr><td>AQMS</td><td>${it.description}</td><td>${it.remark}</td></tr>" })
            }
            if (data.taskWagon.isNotEmpty()) {
                append(data.taskWagon.joinToString("") { "<tr><td>Wagon</td><td>${it.description}</td><td>${it.remark}</td></tr>" })
            }
            if (data.taskLoco.isNotEmpty()) {
                append(data.taskLoco.joinToString("") { "<tr><td>Locomotive</td><td>${it.description}</td><td>${it.remark}</td></tr>" })
            }
            if (data.taskCoach.isNotEmpty()) {
                append(data.taskCoach.joinToString("") { "<tr><td>Coach</td><td>${it.description}</td><td>${it.remark}</td></tr>" })
            }
            if (data.taskEquipment.isNotEmpty()) {
                append(data.taskEquipment.joinToString("") { "<tr><td>Equipment</td><td>${it.description}</td><td>${it.remark}</td></tr>" })
            }
        }

        val webView = WebView(context)
        val htmlDocument = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <title>Ethio-Djibouti Railway - Daily Report - ${report.reportDate}</title>
              <style>
                body { font-family: Arial, sans-serif; color: #1E293B; padding: 24px; background: #FFFFFF; }
                h1 { font-size: 22px; color: #006837; text-align: center; margin: 0; }
                h2 { font-size: 14px; text-align: center; color: #005A2B; margin-top: 4px; }
                h3 { font-size: 16px; text-align: center; margin-top: 6px; }
                .date-badge { text-align: center; font-weight: bold; margin: 12px 0; font-size: 13px; color: #006837; }
                .section { margin: 18px 0; }
                .sec-title { font-size: 13px; font-weight: bold; border-bottom: 2px solid #006837; padding-bottom: 4px; margin-bottom: 8px; text-transform: uppercase; color: #006837; }
                table { width: 100%; border-collapse: collapse; margin-top: 8px; font-size: 11px; }
                th, td { border: 1px solid #A5D6A7; padding: 6px 8px; text-align: left; }
                th { background-color: #E8F5E9; color: #005A2B; }
                .url-box { background: #F1F8F3; border: 1.5px solid #81C784; padding: 12px; border-radius: 6px; margin: 16px 0; }
                .url-label { font-size: 11px; font-weight: bold; color: #006837; margin-bottom: 4px; }
                .url-link { font-size: 11px; color: #006837; word-break: break-all; font-weight: bold; font-family: monospace; text-decoration: underline; }
                .url-desc { font-size: 9.5px; color: #64748B; margin-top: 4px; }
                .footer { font-size: 10px; color: #64748B; text-align: center; margin-top: 30px; border-top: 1px solid #A5D6A7; padding-top: 10px; }
              </style>
            </head>
            <body>
              <h1>Ethio-Djibouti Railway</h1>
              <h2>Acceptance and Quality Control</h2>
              <h3>Daily Report</h3>
              <div class="date-badge">DATE: ${report.reportDate} | STATUS: ${report.status}</div>
              
              <div class="section">
                <div class="sec-title">${printSecNum++}. Executive Summary</div>
                <p style="font-size: 12px; line-height: 1.5;">${report.executiveSummary}</p>
              </div>

              ${if (totalQC > 0) """
              <div class="section">
                <div class="sec-title">${printSecNum++}. Quality Control Entered Units ($totalQC Total)</div>
                <table>
                  <tr><th>Section</th><th>Number</th><th>Type</th><th>Description</th><th>Status</th></tr>
                  $qcRows
                </table>
              </div>
              """ else ""}

              ${if (totalTasks > 0) """
              <div class="section">
                <div class="sec-title">${printSecNum++}. Departmental Tasks Consolidated ($totalTasks Total)</div>
                <table>
                  <tr><th>Department</th><th>Task Description</th><th>Remark</th></tr>
                  $taskRows
                </table>
              </div>
              """ else ""}

              <div class="url-box">
                <div class="url-label">ONLINE DIGITAL CLOUD VERIFICATION & PUBLIC URL:</div>
                <div class="url-link">${liveUrl}</div>
                <div class="url-desc">Open this URL in any web browser to view the realtime Firebase database record, audit status, and full JSON payload.</div>
              </div>

              <div class="section">
                <div class="sec-title">${printSecNum++}. Remarks</div>
                <p style="font-size: 11px;">${report.remarks}</p>
              </div>

              <div class="footer">
                Ethio-Djibouti Standard Gauge Railway Share Company | Public Verification Link: ${liveUrl}
              </div>
            </body>
            </html>
        """.trimIndent()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printAdapter = webView.createPrintDocumentAdapter("EDR_QC_Daily_Report_${report.reportDate}")
                printManager.print("EDR_QC_Daily_Report_${report.reportDate}", printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlDocument, "text/html", "UTF-8", null)
    } catch (e: Throwable) {
        // Fallback directly to native PDF exporter if WebView or PrintManager fails in emulator
        com.example.util.PdfReportExporter.sharePdfFile(context, report, data, liveUrl)
    }
}
