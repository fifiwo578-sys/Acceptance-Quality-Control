package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyReportEntity
import com.example.ui.EdrViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ReportHistoryScreen(viewModel: EdrViewModel) {
    val reports by viewModel.reports.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val context = LocalContext.current

    var dateFilterQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val filtered = reports.filter { r ->
        val matchDate = dateFilterQuery.isBlank() || r.reportDate.contains(dateFilterQuery)
        val matchStatus = selectedStatusFilter == "All" || r.status.equals(selectedStatusFilter, true)
        matchDate && matchStatus
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
    ) {
        item {
            SectionHeader(
                title = "Daily Report History (${reports.size})",
                icon = Icons.Default.History,
                accentColor = AccentAmber,
                action = if (reports.isNotEmpty()) {
                    {
                        TextButton(
                            onClick = { showClearHistoryDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = EDRRed)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = EDRRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear History", color = EDRRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else null
            )
        }

        // Search & Filter Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = dateFilterQuery,
                    onValueChange = { dateFilterQuery = it },
                    placeholder = { Text("Filter by date (YYYY-MM-DD)...", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputDark,
                        unfocusedContainerColor = InputDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                // Quick Compile Button
                Button(
                    onClick = { viewModel.createDailyReport() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateBox("No daily reports found matching filter")
            }
        } else {
            items(filtered) { report ->
                ReportHistoryItemCard(
                    report = report,
                    isAdmin = isAdmin,
                    onView = { viewModel.viewReport(report.reportDate) },
                    onOpenWeb = { viewModel.openWebReport(context, report) },
                    onPublish = { viewModel.publishCurrentReport(report.id) },
                    onSendNotification = { viewModel.sendReportPdfViaEmail(context, report) },
                    onCopyLink = {
                        viewModel.copyReportWebLink(context, report)
                    },
                    onShare = { viewModel.shareReportViaApps(context, report) }
                )
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = EDRRed)
                    Text("Clear All Report History", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EDRRed)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all daily report history records? This will clear all archived daily report snapshots from local storage. Your inspection entries will remain intact.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryDialog = false
                        viewModel.clearReportHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EDRRed, contentColor = Color.White)
                ) {
                    Text("Clear All Reports", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }
}

@Composable
fun ReportHistoryItemCard(
    report: DailyReportEntity,
    isAdmin: Boolean,
    onView: () -> Unit,
    onOpenWeb: () -> Unit,
    onPublish: () -> Unit,
    onSendNotification: () -> Unit,
    onCopyLink: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(report.reportDate, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    StatusBadge(report.status)
                }
                Text("Created: ${report.createdAt}", fontSize = 10.sp, color = TextMuted)
            }

            val totalRecords = report.qcWagonCount +
                    report.qcLocoCount + report.qcCoachCount + report.aqmsTaskCount +
                    report.wagonTaskCount + report.locoTaskCount + report.coachTaskCount + report.equipmentTaskCount

            val enteredParts = mutableListOf<String>()
            if (report.qcWagonCount > 0) enteredParts.add("Wagon: ${report.qcWagonCount}")
            if (report.qcLocoCount > 0) enteredParts.add("Loco: ${report.qcLocoCount}")
            if (report.qcCoachCount > 0) enteredParts.add("Coach: ${report.qcCoachCount}")
            if (report.aqmsTaskCount > 0) enteredParts.add("AQMS: ${report.aqmsTaskCount}")
            if (report.wagonTaskCount > 0) enteredParts.add("Wagon: ${report.wagonTaskCount}")
            if (report.locoTaskCount > 0) enteredParts.add("Loco: ${report.locoTaskCount}")
            if (report.coachTaskCount > 0) enteredParts.add("Coach: ${report.coachTaskCount}")
            if (report.equipmentTaskCount > 0) enteredParts.add("Equip: ${report.equipmentTaskCount}")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (enteredParts.isNotEmpty()) {
                    Text(enteredParts.joinToString(" • "), fontSize = 11.sp, color = AccentAmber, fontWeight = FontWeight.Bold)
                } else {
                    Text("No records logged", fontSize = 11.sp, color = TextMuted)
                }
                if (report.publishedAt.isNotBlank()) {
                    Text("Published: ${report.publishedAt}", fontSize = 11.sp, color = EDRGreen)
                }
            }

            Text(
                text = report.executiveSummary.take(120) + if (report.executiveSummary.length > 120) "..." else "",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )

            // Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onView) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = EDRCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View", fontSize = 11.sp, color = EDRCyan)
                }

                TextButton(onClick = onOpenWeb) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp), tint = EDRGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Web", fontSize = 11.sp, color = EDRGreen)
                }

                TextButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentAmber)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 11.sp, color = AccentAmber)
                }

                TextButton(onClick = onCopyLink) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 11.sp, color = TextSecondary)
                }

                if (isAdmin) {
                    if (report.status == "Draft") {
                        TextButton(onClick = onPublish) {
                            Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentAmber)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Publish", fontSize = 11.sp, color = AccentAmber)
                        }
                    } else {
                        TextButton(onClick = onSendNotification) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = EDRGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Email PDF", fontSize = 11.sp, color = EDRGreen)
                        }
                    }
                }
            }
        }
    }
}
