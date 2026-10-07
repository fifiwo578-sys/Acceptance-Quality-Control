package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.EdrViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun TrashScreen(viewModel: EdrViewModel) {
    val trashQC by viewModel.trashQC.collectAsState()
    val trashTasks by viewModel.trashTasks.collectAsState()

    val cancelledQC by viewModel.cancelledQC.collectAsState()
    val cancelledTasks by viewModel.cancelledTasks.collectAsState()

    var activeTab by remember { mutableStateOf("trash") }

    val totalTrash = trashQC.size + trashTasks.size
    val totalCancelled = cancelledQC.size + cancelledTasks.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
    ) {
        item {
            SectionHeader(title = "Archive: Trash & Cancelled", icon = Icons.Default.DeleteOutline, accentColor = EDRRed)
        }

        // Tab Selector Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTab("Trash Archive", totalTrash, activeTab == "trash") { activeTab = "trash" }
                FilterTab("Cancelled Records", totalCancelled, activeTab == "cancelled") { activeTab = "cancelled" }
            }
        }

        if (activeTab == "trash") {
            if (totalTrash > 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { viewModel.emptyAllTrash() },
                            colors = ButtonDefaults.buttonColors(containerColor = EDRRed, contentColor = TextPrimary),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Empty Trash Archive", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (totalTrash == 0) {
                item { EmptyStateBox("Trash is completely empty") }
            } else {
                // QC
                trashQC.forEach { qc ->
                    item {
                        TrashItemRow(
                            title = "QC ${qc.module.uppercase()}: #${qc.number} - ${qc.description}",
                            subtitle = "Reason: ${qc.trashReason.ifBlank { "Manual delete" }} | Deleted: ${qc.deletedAt}",
                            onRestore = { viewModel.restoreFromTrash("qc", qc.id) },
                            onPermanentDelete = { viewModel.permanentDelete("qc", qc.id) }
                        )
                    }
                }
                // Tasks
                trashTasks.forEach { task ->
                    item {
                        TrashItemRow(
                            title = "Task (${task.module.uppercase()}): ${task.description}",
                            subtitle = "Reason: ${task.trashReason.ifBlank { "Manual delete" }} | Deleted: ${task.deletedAt}",
                            onRestore = { viewModel.restoreFromTrash("task", task.id) },
                            onPermanentDelete = { viewModel.permanentDelete("task", task.id) }
                        )
                    }
                }
            }
        } else {
            // Cancelled Tab
            if (totalCancelled == 0) {
                item { EmptyStateBox("No cancelled records currently archived") }
            } else {
                cancelledQC.forEach { qc ->
                    item {
                        CancelledItemRow(
                            title = "QC ${qc.module.uppercase()}: #${qc.number} (${qc.maintenanceType})",
                            subtitle = "Cancelled: ${qc.cancelledAt} | Reason: ${qc.cancelReason.ifBlank { "None provided" }}",
                            onReinstate = { viewModel.reinstateItem("qc", qc.id) }
                        )
                    }
                }
                cancelledTasks.forEach { task ->
                    item {
                        CancelledItemRow(
                            title = "Task: ${task.description}",
                            subtitle = "Cancelled: ${task.cancelledAt} | Reason: ${task.cancelReason.ifBlank { "None provided" }}",
                            onReinstate = { viewModel.reinstateItem("task", task.id) }
                        )
                    }
                }
            }
        }
    }
}
