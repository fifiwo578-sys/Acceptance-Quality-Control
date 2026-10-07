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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QCEntity
import com.example.data.model.TaskEntity
import com.example.ui.EdrViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleRecordsScreen(
    moduleKey: String,
    title: String,
    accentColor: Color,
    viewModel: EdrViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") }
    var selectedRowIds by remember { mutableStateOf(setOf<Long>()) }

    // Dialog state for cancel/trash
    var cancelTargetId by remember { mutableStateOf<Long?>(null) }
    var cancelReason by remember { mutableStateOf("") }
    var trashTargetId by remember { mutableStateOf<Long?>(null) }
    var trashReason by remember { mutableStateOf("") }

    // Inline Form State for Editing or New Entry
    var editingQCId by remember { mutableStateOf<Long?>(null) }
    var formNumber by remember { mutableStateOf("") }
    var formMaintType by remember { mutableStateOf("Preventive") }
    var formDesc by remember { mutableStateOf("") }
    var formFault by remember { mutableStateOf("") }
    var formSparePart by remember { mutableStateOf("") }
    var formStatus by remember { mutableStateOf("Pending") }
    var formStartDate by remember { mutableStateOf(viewModel.todayDateStr()) }
    var formFinishDate by remember { mutableStateOf("") }
    var formRemark by remember { mutableStateOf("") }

    // Task editing state
    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    var formTaskDate by remember { mutableStateOf(viewModel.todayDateStr()) }
    var formTaskDesc by remember { mutableStateOf("") }
    var formTaskRemark by remember { mutableStateOf("") }

    val allQC by viewModel.allQC.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()

    val isQC = moduleKey.startsWith("qc_")
    val isTask = moduleKey.startsWith("task_")

    val qcModule = when (moduleKey) {
        "qc_wagon" -> "wagon"
        "qc_locomotive" -> "locomotive"
        "qc_coach" -> "coach"
        else -> ""
    }
    val taskModule = when (moduleKey) {
        "task_aqms" -> "aqms"
        "task_wagon" -> "wagon"
        "task_locomotive" -> "locomotive"
        "task_coach" -> "coach"
        "task_equipment" -> "equipment"
        else -> ""
    }

    val currentQCList = allQC.filter { it.module == qcModule && !it.isTrash }
    val currentTaskList = allTasks.filter { it.module == taskModule && !it.isTrash }

    fun resetForm() {
        editingQCId = null
        formNumber = ""
        formMaintType = "Preventive"
        formDesc = ""
        formFault = ""
        formSparePart = ""
        formStatus = "Pending"
        formStartDate = viewModel.todayDateStr()
        formFinishDate = ""
        formRemark = ""

        editingTaskId = null
        formTaskDate = viewModel.todayDateStr()
        formTaskDesc = ""
        formTaskRemark = ""
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
    ) {
        // 1. TOP HEADER & INLINE DATA ENTRY CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header row with CSV buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            Text(
                                text = if (editingQCId != null || editingTaskId != null) "Edit $title Record" else "$title — Data Entry",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Export CSV Button
                            Button(
                                onClick = {
                                    val csvContent = if (isQC) {
                                        val header = "Number,Type of Maintenance,Description,Fault,Spare Part,Status,Start Date,Finish Date,Remark\n"
                                        val rows = currentQCList.joinToString("\n") {
                                            "\"${it.number}\",\"${it.maintenanceType}\",\"${it.description}\",\"${it.fault}\",\"${it.sparePart}\",\"${it.status}\",\"${it.startDate}\",\"${it.finishDate}\",\"${it.remark}\""
                                        }
                                        header + rows
                                    } else {
                                        val header = "Date,Description,Remark\n"
                                        val rows = currentTaskList.joinToString("\n") {
                                            "\"${it.date}\",\"${it.description}\",\"${it.remark}\""
                                        }
                                        header + rows
                                    }

                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, csvContent)
                                        putExtra(Intent.EXTRA_TITLE, "EDR_${moduleKey}_records.csv")
                                        type = "text/csv"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Export $title CSV"))
                                    viewModel.showToast("Exported $title CSV")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EDRGreen.copy(alpha = 0.2f), contentColor = EDRGreen),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("CSV", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Form Fields
                    if (isQC) {
                        // Row 1: Number & Maintenance Type
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = formNumber,
                                onValueChange = { formNumber = it },
                                label = { Text("Number *") },
                                placeholder = { Text(if (qcModule == "wagon") "e.g. W-1001" else if (qcModule == "locomotive") "e.g. LC-001" else "e.g. CH-001") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            var expandedMaint by remember { mutableStateOf(false) }
                            val maintOpts = listOf("Preventive", "Corrective", "Emergency", "Overhaul", "Inspection")
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = formMaintType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Type of Maintenance *") },
                                    trailingIcon = { IconButton(onClick = { expandedMaint = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null) } },
                                    modifier = Modifier.fillMaxWidth().clickable { expandedMaint = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                                )
                                DropdownMenu(expanded = expandedMaint, onDismissRequest = { expandedMaint = false }) {
                                    maintOpts.forEach { opt ->
                                        DropdownMenuItem(text = { Text(opt) }, onClick = { formMaintType = opt; expandedMaint = false })
                                    }
                                }
                            }
                        }

                        // Row 2: Description
                        OutlinedTextField(
                            value = formDesc,
                            onValueChange = { formDesc = it },
                            label = { Text("Description") },
                            placeholder = { Text("Enter acceptance inspection findings or maintenance details...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )

                        // Row 3: Fault & Change Spare Part
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = formFault,
                                onValueChange = { formFault = it },
                                label = { Text("Fault") },
                                placeholder = { Text("Defect identified (or Nil)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = formSparePart,
                                onValueChange = { formSparePart = it },
                                label = { Text("Change Spare Part") },
                                placeholder = { Text("Spare parts consumed") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                        }

                        // Row 4: Status, Start Date, Finish Date
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            var expandedStatus by remember { mutableStateOf(false) }
                            val statusOpts = listOf("Pending", "In Progress", "Completed", "On Hold", "Cancelled")
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = formStatus,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Status *") },
                                    trailingIcon = { IconButton(onClick = { expandedStatus = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null) } },
                                    modifier = Modifier.fillMaxWidth().clickable { expandedStatus = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                                )
                                DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                    statusOpts.forEach { st ->
                                        DropdownMenuItem(text = { Text(st) }, onClick = { formStatus = st; expandedStatus = false })
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = formStartDate,
                                onValueChange = { formStartDate = it },
                                label = { Text("Start Date *") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = formFinishDate,
                                onValueChange = { formFinishDate = it },
                                label = { Text("Finish Date") },
                                placeholder = { Text(formStartDate) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                        }

                        // Row 5: Remark
                        OutlinedTextField(
                            value = formRemark,
                            onValueChange = { formRemark = it },
                            label = { Text("Remark") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )
                    } else if (isTask) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = formTaskDate,
                                onValueChange = { formTaskDate = it },
                                label = { Text("Date *") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = formTaskRemark,
                                onValueChange = { formTaskRemark = it },
                                label = { Text("Remark") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                        }

                        OutlinedTextField(
                            value = formTaskDesc,
                            onValueChange = { formTaskDesc = it },
                            label = { Text("Description *") },
                            placeholder = { Text("Task assignment and requirements...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )
                    }

                    // Form Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isQC) {
                                    if (formNumber.isBlank()) {
                                        viewModel.showToast("Number is required")
                                        return@Button
                                    }
                                    val editId = editingQCId
                                    if (editId != null) {
                                        val existing = currentQCList.find { it.id == editId }
                                        if (existing != null) {
                                            viewModel.updateQCRecord(
                                                existing.copy(
                                                    number = formNumber.trim(),
                                                    maintenanceType = formMaintType,
                                                    description = formDesc.trim(),
                                                    fault = formFault.trim(),
                                                    sparePart = formSparePart.trim(),
                                                    status = formStatus,
                                                    startDate = formStartDate.trim(),
                                                    finishDate = formFinishDate.trim(),
                                                    remark = formRemark.trim()
                                                )
                                            )
                                        }
                                    } else {
                                        viewModel.addQC(
                                            module = qcModule,
                                            number = formNumber.trim(),
                                            maintenanceType = formMaintType,
                                            desc = formDesc.trim(),
                                            fault = formFault.trim(),
                                            sparePart = formSparePart.trim(),
                                            status = formStatus,
                                            startDate = formStartDate.trim().ifBlank { viewModel.todayDateStr() },
                                            finishDate = formFinishDate.trim(),
                                            remark = formRemark.trim()
                                        )
                                    }
                                } else if (isTask) {
                                    if (formTaskDesc.isBlank()) {
                                        viewModel.showToast("Description is required")
                                        return@Button
                                    }
                                    val editId = editingTaskId
                                    if (editId != null) {
                                        val existing = currentTaskList.find { it.id == editId }
                                        if (existing != null) {
                                            viewModel.updateTaskRecord(
                                                existing.copy(
                                                    date = formTaskDate.trim(),
                                                    description = formTaskDesc.trim(),
                                                    remark = formTaskRemark.trim()
                                                )
                                            )
                                        }
                                    } else {
                                        viewModel.addTask(
                                            module = taskModule,
                                            date = formTaskDate.trim().ifBlank { viewModel.todayDateStr() },
                                            desc = formTaskDesc.trim(),
                                            remark = formTaskRemark.trim()
                                        )
                                    }
                                }
                                resetForm()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = BgDark),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(if (editingQCId != null || editingTaskId != null) Icons.Default.Save else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (editingQCId != null || editingTaskId != null) "Update Record" else "Save to Cloud", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { resetForm() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.RotateLeft, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }
            }
        }

        // 2. FILTER TABS
        item {
            val totalCount = if (isQC) currentQCList.size else currentTaskList.size
            val activeCount = if (isQC) currentQCList.count { it.status != "Cancelled" } else currentTaskList.count { !it.isCancelled }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterTab("All", totalCount, selectedFilter == "all") { selectedFilter = "all" }
                FilterTab("Active", activeCount, selectedFilter == "active") { selectedFilter = "active" }
                if (isQC) {
                    FilterTab("Pending", currentQCList.count { it.status.equals("Pending", true) }, selectedFilter == "pending") { selectedFilter = "pending" }
                    FilterTab("In Progress", currentQCList.count { it.status.equals("In Progress", true) }, selectedFilter == "inprogress") { selectedFilter = "inprogress" }
                    FilterTab("Completed", currentQCList.count { it.status.equals("Completed", true) }, selectedFilter == "completed") { selectedFilter = "completed" }
                    FilterTab("Cancelled", currentQCList.count { it.status.equals("Cancelled", true) }, selectedFilter == "cancelled") { selectedFilter = "cancelled" }
                }
            }
        }

        // 3. STATUS PROGRESS BARS (Matching HTML Dashboard)
        if (isQC && currentQCList.isNotEmpty()) {
            item {
                val tot = currentQCList.size.coerceAtLeast(1)
                val pend = currentQCList.count { it.status.equals("Pending", true) }
                val inprog = currentQCList.count { it.status.equals("In Progress", true) }
                val comp = currentQCList.count { it.status.equals("Completed", true) }
                val onhold = currentQCList.count { it.status.equals("On Hold", true) }
                val can = currentQCList.count { it.status.equals("Cancelled", true) }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusProgressBarBox("Pending", pend, (pend * 100) / tot, EDROrange, Modifier.weight(1f))
                    StatusProgressBarBox("In Progress", inprog, (inprog * 100) / tot, EDRCyan, Modifier.weight(1f))
                    StatusProgressBarBox("Completed", comp, (comp * 100) / tot, EDRGreen, Modifier.weight(1f))
                    StatusProgressBarBox("On Hold", onhold, (onhold * 100) / tot, EDRRed, Modifier.weight(1f))
                    StatusProgressBarBox("Cancelled", can, (can * 100) / tot, TextMuted, Modifier.weight(1f))
                }
            }
        }

        // 4. BULK ACTIONS BAR (When 1 or more items are selected)
        if (selectedRowIds.isNotEmpty()) {
            item {
                Surface(
                    color = EDRCyan.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EDRCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${selectedRowIds.size} selected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EDRCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (isQC) {
                                Button(
                                    onClick = {
                                        selectedRowIds.forEach { id -> viewModel.cancelItem("qc", id, "Bulk cancelled") }
                                        selectedRowIds = emptySet()
                                        viewModel.showToast("Selected items cancelled")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EDROrange.copy(alpha = 0.2f), contentColor = EDROrange),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    val type = if (isQC) "qc" else "task"
                                    selectedRowIds.forEach { id -> viewModel.trashItem(type, id, "Bulk deleted") }
                                    selectedRowIds = emptySet()
                                    viewModel.showToast("Selected items moved to trash")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EDRRed.copy(alpha = 0.2f), contentColor = EDRRed),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Trash All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(onClick = { selectedRowIds = emptySet() }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // 5. SEARCH & QUICK REPORT BAR
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search records...", color = TextMuted, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputDark,
                        unfocusedContainerColor = InputDark,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = { viewModel.navigateTo("reports") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EDRViolet),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EDRViolet.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report", fontSize = 11.sp)
                }
            }
        }

        // 6. RECORDS LIST WITH ACTION DROPDOWN / ROW CONTROLS
        if (isQC) {
            val filteredQC = currentQCList.filter { item ->
                val matchFilter = when (selectedFilter) {
                    "active" -> !item.isCancelled
                    "pending" -> item.status.equals("Pending", true) && !item.isCancelled
                    "inprogress" -> item.status.equals("In Progress", true) && !item.isCancelled
                    "completed" -> item.status.equals("Completed", true) && !item.isCancelled
                    "cancelled" -> item.status.equals("Cancelled", true)
                    else -> true
                }
                val matchSearch = searchQuery.isBlank() ||
                        item.number.contains(searchQuery, true) ||
                        item.description.contains(searchQuery, true) ||
                        item.fault.contains(searchQuery, true) ||
                        item.maintenanceType.contains(searchQuery, true) ||
                        item.remark.contains(searchQuery, true)
                matchFilter && matchSearch
            }

            if (filteredQC.isEmpty()) {
                item { EmptyStateBox("No QC records matching criteria.") }
            } else {
                items(filteredQC, key = { it.id }) { item ->
                    val isChecked = selectedRowIds.contains(item.id)
                    QCRecordRowCard(
                        item = item,
                        isChecked = isChecked,
                        onToggleCheck = {
                            selectedRowIds = if (isChecked) selectedRowIds - item.id else selectedRowIds + item.id
                        },
                        onEdit = {
                            editingQCId = item.id
                            formNumber = item.number
                            formMaintType = item.maintenanceType
                            formDesc = item.description
                            formFault = item.fault
                            formSparePart = item.sparePart
                            formStatus = item.status
                            formStartDate = item.startDate
                            formFinishDate = item.finishDate
                            formRemark = item.remark
                            coroutineScope.launch { listState.animateScrollToItem(0) }
                        },
                        onCancel = { cancelTargetId = item.id },
                        onTrash = { trashTargetId = item.id },
                        onReinstate = { viewModel.reinstateItem("qc", item.id) }
                    )
                }
            }
        } else if (isTask) {
            val filteredTasks = currentTaskList.filter { task ->
                val matchFilter = when (selectedFilter) {
                    "active" -> !task.isCancelled
                    "cancelled" -> task.isCancelled
                    else -> true
                }
                val matchSearch = searchQuery.isBlank() ||
                        task.description.contains(searchQuery, true) ||
                        task.remark.contains(searchQuery, true) ||
                        task.date.contains(searchQuery, true)
                matchFilter && matchSearch
            }

            if (filteredTasks.isEmpty()) {
                item { EmptyStateBox("No tasks matching criteria.") }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    val isChecked = selectedRowIds.contains(task.id)
                    TaskRecordRowCard(
                        item = task,
                        isChecked = isChecked,
                        onToggleCheck = {
                            selectedRowIds = if (isChecked) selectedRowIds - task.id else selectedRowIds + task.id
                        },
                        onEdit = {
                            editingTaskId = task.id
                            formTaskDate = task.date
                            formTaskDesc = task.description
                            formTaskRemark = task.remark
                            coroutineScope.launch { listState.animateScrollToItem(0) }
                        },
                        onCancel = { cancelTargetId = task.id },
                        onTrash = { trashTargetId = task.id },
                        onReinstate = { viewModel.reinstateItem("task", task.id) }
                    )
                }
            }
        }
    }

    // Cancel Reason Dialog (Matching HTML Modal)
    if (cancelTargetId != null) {
        AlertDialog(
            onDismissRequest = { cancelTargetId = null; cancelReason = "" },
            title = { Text("Cancel Maintenance", color = EDROrange, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provide a cancellation reason. The record will remain archived under Cancelled.", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Cancellation reason (optional)...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = cancelTargetId ?: return@Button
                        val type = if (isQC) "qc" else "task"
                        viewModel.cancelItem(type, id, cancelReason)
                        cancelTargetId = null
                        cancelReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EDROrange)
                ) {
                    Text("Cancel Maintenance", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTargetId = null; cancelReason = "" }) { Text("Close", color = TextSecondary) }
            },
            containerColor = CardDark
        )
    }

    // Trash Reason Dialog
    if (trashTargetId != null) {
        AlertDialog(
            onDismissRequest = { trashTargetId = null; trashReason = "" },
            title = { Text("Move to Trash", color = EDRRed, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Move this record to Trash in local database? You can restore it later from Trash & Cancelled.", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = trashReason,
                        onValueChange = { trashReason = it },
                        label = { Text("Deletion reason (optional)...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = trashTargetId ?: return@Button
                        val type = if (isQC) "qc" else "task"
                        viewModel.trashItem(type, id, trashReason)
                        trashTargetId = null
                        trashReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EDRRed)
                ) {
                    Text("Move to Trash", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { trashTargetId = null; trashReason = "" }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = CardDark
        )
    }
}

@Composable
fun StatusProgressBarBox(label: String, count: Int, percent: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardDark2),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(label)
                Text(count.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = InputDark
            )
            Text("$percent%", fontSize = 8.5.sp, color = TextMuted)
        }
    }
}

@Composable
fun QCRecordRowCard(
    item: QCEntity,
    isChecked: Boolean,
    onToggleCheck: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onTrash: () -> Unit,
    onReinstate: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (item.isCancelled) BorderDark.copy(alpha = 0.5f) else BorderDark)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleCheck() },
                        colors = CheckboxDefaults.colors(checkedColor = AccentAmber),
                        modifier = Modifier.size(20.dp)
                    )
                    Text("#${item.number}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Surface(color = EDRCyan.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = item.maintenanceType,
                            color = EDRCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    StatusBadge(status = item.status)
                }

                // Action Menu Dropdown Button
                Box {
                    IconButton(onClick = { expandedMenu = true }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = EDRCyan, modifier = Modifier.size(14.dp)) },
                            text = { Text("Edit") },
                            onClick = { expandedMenu = false; onEdit() }
                        )
                        if (item.status != "Cancelled") {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = EDROrange, modifier = Modifier.size(14.dp)) },
                                text = { Text("Cancel") },
                                onClick = { expandedMenu = false; onCancel() }
                            )
                        } else {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.RotateLeft, contentDescription = null, tint = EDRGreen, modifier = Modifier.size(14.dp)) },
                                text = { Text("Reinstate") },
                                onClick = { expandedMenu = false; onReinstate() }
                            )
                        }
                        Divider(color = BorderDark)
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = EDRRed, modifier = Modifier.size(14.dp)) },
                            text = { Text("Trash", color = EDRRed) },
                            onClick = { expandedMenu = false; onTrash() }
                        )
                    }
                }
            }

            // Description & Details
            if (item.description.isNotBlank()) {
                Text(item.description, fontSize = 11.5.sp, color = TextSecondary, lineHeight = 16.sp)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (item.fault.isNotBlank()) {
                    Text("Fault: ${item.fault}", fontSize = 10.5.sp, color = EDROrange)
                }
                if (item.sparePart.isNotBlank()) {
                    Text("Parts: ${item.sparePart}", fontSize = 10.5.sp, color = TextMuted)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dates: ${item.startDate} ${if (item.finishDate.isNotBlank()) "→ " + item.finishDate else ""}", fontSize = 10.sp, color = TextMuted)
                if (item.remark.isNotBlank()) {
                    Text(item.remark, fontSize = 10.sp, color = AccentAmber, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun TaskRecordRowCard(
    item: TaskEntity,
    isChecked: Boolean,
    onToggleCheck: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onTrash: () -> Unit,
    onReinstate: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (item.isCancelled) BorderDark.copy(alpha = 0.5f) else BorderDark)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleCheck() },
                        colors = CheckboxDefaults.colors(checkedColor = AccentAmber),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(item.description, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Box {
                    IconButton(onClick = { expandedMenu = true }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = EDRCyan, modifier = Modifier.size(14.dp)) },
                            text = { Text("Edit") },
                            onClick = { expandedMenu = false; onEdit() }
                        )
                        if (!item.isCancelled) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = EDROrange, modifier = Modifier.size(14.dp)) },
                                text = { Text("Cancel") },
                                onClick = { expandedMenu = false; onCancel() }
                            )
                        } else {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.RotateLeft, contentDescription = null, tint = EDRGreen, modifier = Modifier.size(14.dp)) },
                                text = { Text("Reinstate") },
                                onClick = { expandedMenu = false; onReinstate() }
                            )
                        }
                        Divider(color = BorderDark)
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = EDRRed, modifier = Modifier.size(14.dp)) },
                            text = { Text("Trash", color = EDRRed) },
                            onClick = { expandedMenu = false; onTrash() }
                        )
                    }
                }
            }

            if (item.remark.isNotBlank()) {
                Text(item.remark, fontSize = 11.sp, color = TextSecondary)
            }
            Text("Date: ${item.date}", fontSize = 10.sp, color = TextMuted)
        }
    }
}
