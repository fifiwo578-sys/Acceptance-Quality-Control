package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyReportEntity
import com.example.ui.EdrViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DailyReportsScreen(viewModel: EdrViewModel) {
    val reports by viewModel.reports.collectAsState()
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayReport = reports.firstOrNull { it.reportDate == today }
    val context = LocalContext.current

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedDateInput by remember { mutableStateOf(today) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
    ) {
        // Hero Header & Action Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Reports Dashboard", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentAmber)
                            Text("Acceptance and Quality Control | Ethio-Djibouti Railway", fontSize = 11.sp, color = TextMuted)
                        }
                        Surface(
                            color = CardDark2,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Today, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                                Text(today, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }

                    Text(
                        text = "Generates the official daily report by compiling all QC inspections (Wagon, Locomotive, Coach) and departmental tasks. Published reports generate shareable public links that anyone can view without an admin account.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.createDailyReport(today) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BgDark),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compile Today's Report", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showDatePickerDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark2),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Date", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Today's Report Status Card
        item {
            SectionHeader(title = "Today's Report Status", icon = Icons.Default.Info, accentColor = AccentAmber)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Date: $today", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            StatusBadge(todayReport?.status ?: "Draft")
                        }

                        if (todayReport != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Direct Email Report in PDF Form (Smart Report)
                                IconButton(
                                    onClick = {
                                        viewModel.sendReportPdfViaEmail(context, todayReport)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = "Email Report PDF", tint = EDRGreen, modifier = Modifier.size(16.dp))
                                }

                                // Share Web Report
                                IconButton(
                                    onClick = {
                                        viewModel.shareReportViaApps(context, todayReport)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Web Report", tint = AccentAmber, modifier = Modifier.size(16.dp))
                                }

                                // Open Web Report
                                IconButton(
                                    onClick = {
                                        viewModel.openWebReport(context, todayReport)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = "Open Web Report", tint = EDRGreen, modifier = Modifier.size(16.dp))
                                }

                                Button(
                                    onClick = { viewModel.viewReport(todayReport.reportDate) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EDRCyan.copy(alpha = 0.2f), contentColor = EDRCyan),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Metadata details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardDark2, RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CREATED", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(todayReport?.createdAt?.ifBlank { "Not generated" } ?: "Not generated", fontSize = 11.sp, color = TextSecondary)
                        }
                        Column {
                            Text("PUBLISHED", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(todayReport?.publishedAt?.ifBlank { "Draft" } ?: "Draft", fontSize = 11.sp, color = if (todayReport?.status == "Published" || todayReport?.status == "Notification Sent") EDRGreen else TextMuted)
                        }
                        Column {
                            Text("NOTIFICATIONS", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(todayReport?.notificationSentAt?.ifBlank { "None sent" } ?: "None sent", fontSize = 11.sp, color = if (todayReport?.notificationSentAt?.isNotBlank() == true) EDRViolet else TextMuted)
                        }
                    }

                    // Acceptance & QC Summary - Displays ONLY entered modules
                    Text("ACCEPTANCE & QC CONSOLIDATED SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)

                    val activeMetrics = mutableListOf<Triple<String, Int, Color>>()
                    if ((todayReport?.qcWagonCount ?: 0) > 0) activeMetrics.add(Triple("QC Wagon", todayReport?.qcWagonCount ?: 0, EDRViolet))
                    if ((todayReport?.qcLocoCount ?: 0) > 0) activeMetrics.add(Triple("QC Loco", todayReport?.qcLocoCount ?: 0, EDRCyan))
                    if ((todayReport?.qcCoachCount ?: 0) > 0) activeMetrics.add(Triple("QC Coach", todayReport?.qcCoachCount ?: 0, EDRGreen))
                    if ((todayReport?.aqmsTaskCount ?: 0) > 0) activeMetrics.add(Triple("AQMS Task", todayReport?.aqmsTaskCount ?: 0, AccentAmber))
                    if ((todayReport?.wagonTaskCount ?: 0) > 0) activeMetrics.add(Triple("Wagon Task", todayReport?.wagonTaskCount ?: 0, EDROrange))
                    if ((todayReport?.locoTaskCount ?: 0) > 0) activeMetrics.add(Triple("Loco Task", todayReport?.locoTaskCount ?: 0, EDRCyan))
                    if ((todayReport?.coachTaskCount ?: 0) > 0) activeMetrics.add(Triple("Coach Task", todayReport?.coachTaskCount ?: 0, EDRGreen))
                    if ((todayReport?.equipmentTaskCount ?: 0) > 0) activeMetrics.add(Triple("Equip Task", todayReport?.equipmentTaskCount ?: 0, EDRViolet))

                    val totalQC = (todayReport?.qcWagonCount ?: 0) + (todayReport?.qcLocoCount ?: 0) + (todayReport?.qcCoachCount ?: 0)
                    if (totalQC > 0 && activeMetrics.size > 1) {
                        activeMetrics.add(Triple("Total QC", totalQC, AccentAmber))
                    }

                    if (activeMetrics.isEmpty()) {
                        Surface(
                            color = CardDark2,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No inspection or maintenance records entered for this date.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activeMetrics.chunked(3).forEach { rowMetrics ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowMetrics.forEach { (label, count, color) ->
                                        MetricPill(label, count, color, Modifier.weight(1f))
                                    }
                                    repeat(3 - rowMetrics.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    if (todayReport?.executiveSummary?.isNotBlank() == true) {
                        Surface(
                            color = CardDark2,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("EXECUTIVE SUMMARY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentAmber)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(todayReport.executiveSummary, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section: Browse & Share Other Daily Reports
        item {
            SectionHeader(
                title = "All Daily Reports (Share & View)",
                icon = Icons.Default.Share,
                accentColor = EDRCyan,
                action = {
                    TextButton(onClick = { viewModel.navigateTo("reportHistory") }) {
                        Text("Full History", fontSize = 11.sp, color = EDRCyan)
                    }
                }
            )
        }

        val displayReports = reports
        if (displayReports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No reports created yet. Tap 'Compile Today's Report' above.", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        } else {
            items(displayReports.size) { index ->
                val r = displayReports[index]
                ReportSummaryRowCardWithShare(
                    report = r,
                    onSelect = { viewModel.viewReport(r.reportDate) },
                    onShare = {
                        viewModel.shareReportViaApps(context, r)
                    },
                    onEmail = {
                        viewModel.sendReportPdfViaEmail(context, r)
                    }
                )
            }
        }
    }

    // Date Picker Dialog
    if (showDatePickerDialog) {
        AlertDialog(
            onDismissRequest = { showDatePickerDialog = false },
            title = { Text("Select Report Date", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentAmber) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter date in YYYY-MM-DD format to collect records for that date:", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = selectedDateInput,
                        onValueChange = { selectedDateInput = it },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = InputDark,
                            unfocusedContainerColor = InputDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDatePickerDialog = false
                        viewModel.createDailyReport(selectedDateInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BgDark)
                ) {
                    Text("Generate Report", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }
}

@Composable
fun ReportSummaryRowCardWithShare(
    report: DailyReportEntity,
    onSelect: () -> Unit,
    onShare: () -> Unit,
    onEmail: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(report.reportDate, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    StatusBadge(report.status)
                }
                val totalRecs = report.qcWagonCount + report.qcLocoCount + report.qcCoachCount +
                        report.aqmsTaskCount + report.wagonTaskCount + report.locoTaskCount +
                        report.coachTaskCount + report.equipmentTaskCount
                Text("$totalRecs QC & Task records | Sign-off: ${report.createdBy}", fontSize = 11.sp, color = TextMuted)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (onEmail != null) {
                    IconButton(onClick = onEmail, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Email, contentDescription = "Email", tint = EDRGreen, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = AccentAmber, modifier = Modifier.size(16.dp))
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
            }
        }
    }
}
