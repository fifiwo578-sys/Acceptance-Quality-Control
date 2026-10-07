package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.EdrViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.theme.*

@Composable
fun DashboardScreen(viewModel: EdrViewModel) {
    val allQC by viewModel.allQC.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val trashQC by viewModel.trashQC.collectAsState()
    val trashTasks by viewModel.trashTasks.collectAsState()
    val cancelledQC by viewModel.cancelledQC.collectAsState()
    val cancelledTasks by viewModel.cancelledTasks.collectAsState()

    val qcWagon = allQC.filter { it.module == "wagon" }
    val qcLoco = allQC.filter { it.module == "locomotive" }
    val qcCoach = allQC.filter { it.module == "coach" }

    val taskAqms = allTasks.filter { it.module == "aqms" }
    val taskWagon = allTasks.filter { it.module == "wagon" }
    val taskLoco = allTasks.filter { it.module == "locomotive" }
    val taskCoach = allTasks.filter { it.module == "coach" }
    val taskEquipment = allTasks.filter { it.module == "equipment" }

    val totalCancelled = cancelledQC.size + cancelledTasks.size
    val totalTrash = trashQC.size + trashTasks.size
    val totalQC = allQC.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 12.dp)
    ) {
        // App Title & Firebase Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentAmber.copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Acceptance and Quality Control",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentAmber
                                    )
                                    Text(
                                        text = "Ethio-Djibouti Railway Share Company",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Firebase Live Cloud Sync Indicator
                            Surface(
                                color = EDRGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EDRGreen.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { viewModel.syncWithFirebase() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(EDRGreen)
                                    )
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = EDRGreen, modifier = Modifier.size(12.dp))
                                    Text("Firebase Connected", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EDRGreen)
                                }
                            }
                        }

                        Text(
                            text = "Comprehensive daily acceptance verification, rolling stock inspection checklists (Wagon, Locomotive, Coach), and departmental tasks consolidated with multi-user cloud synchronization.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Button(
                                onClick = { viewModel.createDailyReport() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentAmber,
                                    contentColor = BgDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Today's Report", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.navigateTo("dailyReports") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark2),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = EDRCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Daily Reports Hub", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Data Entry Quick Access Hub (Wagon, Locomotive, Coach)
        item {
            SectionHeader(title = "Data Entry — Rolling Stock Acceptance", icon = Icons.Default.EditNote, accentColor = AccentAmber)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SELECT MODULE TO ENTER NEW QUALITY & ACCEPTANCE RECORDS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.navigateTo("dataEntryWagon") },
                            color = EDRViolet.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EDRViolet.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.DirectionsRailway, contentDescription = null, tint = EDRViolet, modifier = Modifier.size(24.dp))
                                Text("Wagon", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("+ New Entry", fontSize = 9.5.sp, color = EDRViolet, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.navigateTo("dataEntryLocomotive") },
                            color = EDRCyan.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EDRCyan.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Train, contentDescription = null, tint = EDRCyan, modifier = Modifier.size(24.dp))
                                Text("Locomotive", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("+ New Entry", fontSize = 9.5.sp, color = EDRCyan, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.navigateTo("dataEntryCoach") },
                            color = EDRGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EDRGreen.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Commute, contentDescription = null, tint = EDRGreen, modifier = Modifier.size(24.dp))
                                Text("Coach", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("+ New Entry", fontSize = 9.5.sp, color = EDRGreen, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Quality Control Section Stats
        item {
            SectionHeader(title = "Quality Control Overview", icon = Icons.Default.FactCheck, accentColor = AccentAmber)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("QC - Wagon", qcWagon.size.toString(), Icons.Default.DirectionsRailway, EDRViolet, Modifier.weight(1f)) {
                        viewModel.navigateTo("qcWagon")
                    }
                    StatCard("QC - Locomotive", qcLoco.size.toString(), Icons.Default.Train, EDRCyan, Modifier.weight(1f)) {
                        viewModel.navigateTo("qcLocomotive")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("QC - Coach", qcCoach.size.toString(), Icons.Default.Commute, EDRGreen, Modifier.weight(1f)) {
                        viewModel.navigateTo("qcCoach")
                    }
                    StatCard("Daily Reports", reports.size.toString(), Icons.Default.Assessment, AccentAmber, Modifier.weight(1f)) {
                        viewModel.navigateTo("dailyReports")
                    }
                }
            }
        }

        // Section: Department Tasks Breakdown
        item {
            SectionHeader(title = "Additional Tasks by Department", icon = Icons.Default.ListAlt, accentColor = EDRCyan)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("AQMS Tasks", taskAqms.size.toString(), Icons.Default.AssignmentTurnedIn, AccentAmber, Modifier.weight(1f)) {
                        viewModel.navigateTo("taskAQMS")
                    }
                    StatCard("Wagon Tasks", taskWagon.size.toString(), Icons.Default.Checklist, EDROrange, Modifier.weight(1f)) {
                        viewModel.navigateTo("taskWagon")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Loco Tasks", taskLoco.size.toString(), Icons.Default.Checklist, EDRCyan, Modifier.weight(1f)) {
                        viewModel.navigateTo("taskLocomotive")
                    }
                    StatCard("Coach Tasks", taskCoach.size.toString(), Icons.Default.Checklist, EDRGreen, Modifier.weight(1f)) {
                        viewModel.navigateTo("taskCoach")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Equipment Tasks", taskEquipment.size.toString(), Icons.Default.Build, EDRViolet, Modifier.weight(1f)) {
                        viewModel.navigateTo("taskEquipment")
                    }
                    StatCard("Archive & Trash", (totalCancelled + totalTrash).toString(), Icons.Default.Archive, EDRRed, Modifier.weight(1f)) {
                        viewModel.navigateTo("trash")
                    }
                }
            }
        }

        // Section: Pivot Table (QC Section x Status)
        item {
            SectionHeader(title = "Acceptance & QC Status Matrix", icon = Icons.Default.GridOn, accentColor = AccentAmber)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardDark2, RoundedCornerShape(6.dp))
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SECTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.weight(1.4f))
                        Text("PEND", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EDROrange, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        Text("PROG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EDRCyan, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        Text("DONE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EDRGreen, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        Text("TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentAmber, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val teams = listOf(
                        Triple("Wagon", qcWagon, EDRViolet),
                        Triple("Locomotive", qcLoco, EDRCyan),
                        Triple("Coach", qcCoach, EDRGreen)
                    )

                    teams.forEach { (name, list, color) ->
                        val pend = list.count { it.status.equals("Pending", ignoreCase = true) }
                        val prog = list.count { it.status.equals("In Progress", ignoreCase = true) }
                        val done = list.count { it.status.equals("Completed", ignoreCase = true) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("QC $name", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.weight(1.4f))
                            Text(pend.toString(), fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            Text(prog.toString(), fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            Text(done.toString(), fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            Text(list.size.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                        }
                        Divider(color = BorderDark.copy(alpha = 0.5f), thickness = 0.8.dp)
                    }

                    val totalPend = allQC.count { it.status.equals("Pending", ignoreCase = true) }
                    val totalProg = allQC.count { it.status.equals("In Progress", ignoreCase = true) }
                    val totalDone = allQC.count { it.status.equals("Completed", ignoreCase = true) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp, start = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentAmber, modifier = Modifier.weight(1.4f))
                        Text(totalPend.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EDROrange, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        Text(totalProg.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EDRCyan, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        Text(totalDone.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EDRGreen, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        Text(allQC.size.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentAmber, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                    }
                }
            }
        }
    }
}
