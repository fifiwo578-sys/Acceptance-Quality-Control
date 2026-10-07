package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.EdrViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DataManagementScreen(viewModel: EdrViewModel) {
    val context = LocalContext.current
    val allQC by viewModel.allQC.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val recipients by viewModel.recipients.collectAsState()
    val logs by viewModel.notificationLogs.collectAsState()
    val trashQC by viewModel.trashQC.collectAsState()
    val trashTasks by viewModel.trashTasks.collectAsState()
    val cloudConfig by viewModel.cloudConfig.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val qcWagon = allQC.filter { it.module == "wagon" }
    val qcLoco = allQC.filter { it.module == "locomotive" }
    val qcCoach = allQC.filter { it.module == "coach" }

    val taskAqms = allTasks.filter { it.module == "aqms" }
    val taskWagon = allTasks.filter { it.module == "wagon" }
    val taskLoco = allTasks.filter { it.module == "locomotive" }
    val taskCoach = allTasks.filter { it.module == "coach" }
    val taskEquip = allTasks.filter { it.module == "equipment" }

    val totalRecords = allQC.size + allTasks.size + reports.size
    val totalTrash = trashQC.size + trashTasks.size
    val completedCount = allQC.count { it.status.equals("Completed", true) }

    // Health Score calculation (matching HTML)
    val healthScore = if (allQC.isNotEmpty()) {
        ((completedCount * 100) / allQC.size).coerceIn(10, 100)
    } else 85

    val healthColor = when {
        healthScore >= 70 -> EDRGreen
        healthScore >= 40 -> AccentAmber
        else -> EDRRed
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
    ) {
        item {
            SectionHeader(title = "Data Management & Cloud Storage", icon = Icons.Default.Storage, accentColor = AccentAmber)
        }

        // Storage & Health Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Room Database + Firebase Cloud Sync", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Local SQLite database synced with Firebase Realtime Database", fontSize = 11.sp, color = TextSecondary)
                            Text(cloudConfig.firebaseDatabaseUrl.ifBlank { "https://acceptance-8781f-default-rtdb.firebaseio.com" }, fontSize = 10.sp, color = EDRCyan, fontWeight = FontWeight.SemiBold)
                        }
                        Surface(
                            color = EDRGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EDRGreen.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "CONNECTED",
                                color = EDRGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // System Health & Storage Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Health Circle Box
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CardDark2)
                                .border(3.dp, healthColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$healthScore%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = healthColor)
                                Text("HEALTH", fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DataMetricBox("Active QC", allQC.size.toString(), AccentAmber, Modifier.weight(1f))
                                DataMetricBox("Tasks", allTasks.size.toString(), EDRCyan, Modifier.weight(1f))
                                DataMetricBox("In Trash", totalTrash.toString(), if (totalTrash > 0) EDRRed else TextMuted, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Smart Actions
        item {
            SectionHeader(title = "Smart Management Actions", icon = Icons.Default.AutoFixHigh, accentColor = EDRCyan)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Firebase Sync Button
                    Button(
                        onClick = { viewModel.syncWithFirebase() },
                        colors = ButtonDefaults.buttonColors(containerColor = EDRGreen, contentColor = BgDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Synchronize with Firebase Cloud Database", fontWeight = FontWeight.Bold)
                    }

                    // Export All CSV
                    Button(
                        onClick = {
                            val csvBuilder = StringBuilder()
                            csvBuilder.append("=== QC - WAGON ===\nNumber,Type,Description,Fault,Spare Part,Status,Start Date,Finish Date,Remark\n")
                            qcWagon.forEach { csvBuilder.append("\"${it.number}\",\"${it.maintenanceType}\",\"${it.description}\",\"${it.fault}\",\"${it.sparePart}\",\"${it.status}\",\"${it.startDate}\",\"${it.finishDate}\",\"${it.remark}\"\n") }
                            csvBuilder.append("\n=== QC - LOCOMOTIVE ===\nNumber,Type,Description,Fault,Spare Part,Status,Start Date,Finish Date,Remark\n")
                            qcLoco.forEach { csvBuilder.append("\"${it.number}\",\"${it.maintenanceType}\",\"${it.description}\",\"${it.fault}\",\"${it.sparePart}\",\"${it.status}\",\"${it.startDate}\",\"${it.finishDate}\",\"${it.remark}\"\n") }
                            csvBuilder.append("\n=== QC - COACH ===\nNumber,Type,Description,Fault,Spare Part,Status,Start Date,Finish Date,Remark\n")
                            qcCoach.forEach { csvBuilder.append("\"${it.number}\",\"${it.maintenanceType}\",\"${it.description}\",\"${it.fault}\",\"${it.sparePart}\",\"${it.status}\",\"${it.startDate}\",\"${it.finishDate}\",\"${it.remark}\"\n") }

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
                                putExtra(Intent.EXTRA_TITLE, "EDR_Acceptance_QC_All.csv")
                                type = "text/csv"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export All Acceptance & QC Records CSV"))
                            viewModel.showToast("All records exported to CSV")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BgDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export All Records to CSV", fontWeight = FontWeight.Bold)
                    }

                    // Clean Dupes & Reload Samples in a Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.cleanDuplicates() },
                            colors = ButtonDefaults.buttonColors(containerColor = EDRCyan.copy(alpha = 0.2f), contentColor = EDRCyan),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clean Dupes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.loadRichSampleData() },
                            colors = ButtonDefaults.buttonColors(containerColor = EDRViolet.copy(alpha = 0.2f), contentColor = EDRViolet),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load Samples", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Empty Trash & Clear All
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.emptyAllTrash() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EDRRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EDRRed.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Empty Trash", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { showClearConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EDRRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EDRRed.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All DB", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Database Tables Overview (Matching HTML Table view)
        item {
            SectionHeader(title = "Database Tables & Key Storage", icon = Icons.Default.TableChart, accentColor = EDRViolet)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tableItems = listOf(
                        Triple("edr_qc_wagon", "QC - Wagon", qcWagon.size),
                        Triple("edr_qc_locomotive", "QC - Locomotive", qcLoco.size),
                        Triple("edr_qc_coach", "QC - Coach", qcCoach.size),
                        Triple("edr_task_aqms", "AQMS Task", taskAqms.size),
                        Triple("edr_task_wagon", "Wagon Task", taskWagon.size),
                        Triple("edr_task_locomotive", "Locomotive Task", taskLoco.size),
                        Triple("edr_task_coach", "Coach Task", taskCoach.size),
                        Triple("edr_task_equipment", "Equipment Task", taskEquip.size),
                        Triple("edr_reports", "Daily Reports", reports.size),
                        Triple("edr__trash", "Trash Archive", totalTrash)
                    )

                    tableItems.forEach { (key, label, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(key, fontSize = 10.sp, color = AccentAmber)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = if (count > 0) CardDark2 else TextMuted.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                                ) {
                                    Text(
                                        text = "$count records",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (count > 0) TextPrimary else TextMuted,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Divider(color = BorderDark.copy(alpha = 0.5f), thickness = 0.8.dp)
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Database Records", color = EDRRed, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to permanently clear all QC records, tasks, and trash from the database? This cannot be undone.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EDRRed)
                ) {
                    Text("Clear All Records", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = CardDark
        )
    }
}
