package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.QCEntity
import com.example.ui.EdrViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.ParsedInspectionItem
import com.example.util.ParseInspectionResult
import com.example.util.SmartDataEntryParser

@Composable
fun DataEntryScreen(
    initialModule: String = "wagon", // "wagon", "locomotive", "coach"
    viewModel: EdrViewModel
) {
    val context = LocalContext.current
    var selectedModule by remember(initialModule) { mutableStateOf(initialModule.lowercase()) }

    val accentColor = when (selectedModule) {
        "locomotive" -> EDRCyan
        "coach" -> EDRGreen
        else -> EDRViolet
    }

    val moduleTitle = when (selectedModule) {
        "locomotive" -> "Locomotive"
        "coach" -> "Coach"
        else -> "Wagon"
    }

    val allQC by viewModel.allQC.collectAsState()
    val moduleRecords = allQC.filter { it.module == selectedModule && !it.isTrash }

    // Form State
    var equipmentNumber by remember(selectedModule) {
        mutableStateOf(
            when (selectedModule) {
                "wagon" -> "W-"
                "locomotive" -> "LC-"
                "coach" -> "CH-"
                else -> ""
            }
        )
    }

    var selectedCarType by remember(selectedModule) {
        mutableStateOf(
            when (selectedModule) {
                "wagon" -> "Flat Container Car"
                "locomotive" -> "HXD1C Electric Mainline"
                "coach" -> "Hard Seat (YZ25G)"
                else -> "Standard"
            }
        )
    }

    var maintenanceType by remember { mutableStateOf("Preventive") }
    var selectedDepot by remember { mutableStateOf("Indode Depot (Addis)") }
    var selectedSubsystem by remember(selectedModule) {
        mutableStateOf(
            when (selectedModule) {
                "wagon" -> "Bogie & Wheelset"
                "locomotive" -> "Traction Motor & Bogie"
                "coach" -> "HVAC & Temperature Control"
                else -> "General"
            }
        )
    }

    var description by remember { mutableStateOf("") }
    var faultDefect by remember { mutableStateOf("Nil (Conforming to Standard)") }
    var sparePartsUsed by remember { mutableStateOf("") }
    var inspectionStatus by remember { mutableStateOf("Completed") }
    var inspectorName by remember { mutableStateOf("Quality Inspector - EDR") }
    var inspectionDate by remember { mutableStateOf(viewModel.todayDateStr()) }
    var finishDate by remember { mutableStateOf("") }
    var signOffRemarks by remember { mutableStateOf("Acceptance verification passed. Cleared for operation.") }

    var formMessage by remember { mutableStateOf<String?>(null) }

    // Smart Text Auto-Arranger & Paste Inspector Notes State
    var rawPasteText by remember { mutableStateOf("") }
    var parseResult by remember { mutableStateOf<ParseInspectionResult?>(null) }
    var isSmartPasteExpanded by remember { mutableStateOf(false) }
    var overrideBulkModule by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
    ) {
        // Module Switcher Tabs: Wagon, Locomotive, Coach
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DATA ENTRY MODULE SELECTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DataEntryModuleTab(
                            title = "Wagon Entry",
                            icon = Icons.Default.DirectionsRailway,
                            color = EDRViolet,
                            isSelected = selectedModule == "wagon",
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedModule = "wagon"
                            equipmentNumber = "W-"
                            selectedCarType = "Flat Container Car"
                            selectedSubsystem = "Bogie & Wheelset"
                        }

                        DataEntryModuleTab(
                            title = "Locomotive Entry",
                            icon = Icons.Default.Train,
                            color = EDRCyan,
                            isSelected = selectedModule == "locomotive",
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedModule = "locomotive"
                            equipmentNumber = "LC-"
                            selectedCarType = "HXD1C Electric Mainline"
                            selectedSubsystem = "Traction Motor & Bogie"
                        }

                        DataEntryModuleTab(
                            title = "Coach Entry",
                            icon = Icons.Default.Commute,
                            color = EDRGreen,
                            isSelected = selectedModule == "coach",
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedModule = "coach"
                            equipmentNumber = "CH-"
                            selectedCarType = "Hard Seat (YZ25G)"
                            selectedSubsystem = "HVAC & Temperature Control"
                        }
                    }
                }
            }
        }

        // SMART AUTO-ARRANGER & BULK PASTE INSPECTION NOTES
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                            Text("Smart Auto-Arranger & Paste Notes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Surface(
                            color = AccentAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "AUTO-ARRANGE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Paste raw acceptance & quality control notes (with vehicle IDs, Fault Record, Action Taken, and #Qualified). The system automatically parses and arranges them into structured records.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )

                    // Quick action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                if (clipText.isNotBlank()) {
                                    rawPasteText = clipText
                                    val result = SmartDataEntryParser.parseInspectionReport(
                                        clipText,
                                        defaultModule = selectedModule,
                                        fallbackDate = viewModel.todayDateStr()
                                    )
                                    parseResult = result
                                    isSmartPasteExpanded = true
                                    viewModel.showToast("Arranged ${result.items.size} records from clipboard!")
                                } else {
                                    viewModel.showToast("Clipboard is empty. Copy inspector notes first.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BgDark),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste from Clipboard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val sampleNotes = SmartDataEntryParser.SAMPLE_INSPECTION_TEXT
                                rawPasteText = sampleNotes
                                val result = SmartDataEntryParser.parseInspectionReport(
                                    sampleNotes,
                                    defaultModule = selectedModule,
                                    fallbackDate = "2026-09-21"
                                )
                                parseResult = result
                                isSmartPasteExpanded = true
                                viewModel.showToast("Loaded and arranged 6 sample records!")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EDRCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EDRCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp), tint = EDRCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Try Sample Data", fontSize = 11.sp)
                        }

                        if (rawPasteText.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    rawPasteText = ""
                                    parseResult = null
                                    isSmartPasteExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Text("Clear", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }

                    // Text Input Box
                    OutlinedTextField(
                        value = rawPasteText,
                        onValueChange = {
                            rawPasteText = it
                            if (it.isNotBlank()) {
                                parseResult = SmartDataEntryParser.parseInspectionReport(
                                    it,
                                    defaultModule = selectedModule,
                                    fallbackDate = viewModel.todayDateStr()
                                )
                            } else {
                                parseResult = null
                            }
                        },
                        placeholder = { Text("Paste inspector notes here...\ne.g.\n1. PW2ER 0122\nFault Record: ...\nAction Taken: ...\n#Qualified") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 160.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = InputDark,
                            unfocusedContainerColor = InputDark,
                            focusedBorderColor = AccentAmber,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Arranged Output Section
                    val result = parseResult
                    if (result != null && result.success && result.items.isNotEmpty()) {
                        Surface(
                            color = CardDark2,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EDRGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EDRGreen, modifier = Modifier.size(18.dp))
                                        Text("Arranged ${result.items.size} Records", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EDRGreen)
                                    }
                                    Surface(
                                        color = EDRCyan.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "Date: ${result.detectedDate}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EDRCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Module override chips:
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save to:", fontSize = 11.sp, color = TextMuted)
                                    listOf("wagon" to "Wagon", "coach" to "Coach", "locomotive" to "Locomotive").forEach { (mod, label) ->
                                        val isSel = (overrideBulkModule ?: selectedModule) == mod
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { overrideBulkModule = mod },
                                            label = { Text(label, fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AccentAmber,
                                                selectedLabelColor = BgDark
                                            )
                                        )
                                    }
                                }

                                Divider(color = BorderDark)

                                // Arranged item preview cards
                                result.items.forEach { item ->
                                    ArrangedItemPreviewCard(
                                        item = item,
                                        onLoadToForm = {
                                            selectedModule = overrideBulkModule ?: item.module
                                            equipmentNumber = item.number
                                            faultDefect = item.fault
                                            description = item.actionTaken
                                            sparePartsUsed = item.spareParts
                                            selectedSubsystem = item.subsystem
                                            inspectionStatus = item.status
                                            inspectionDate = item.date
                                            finishDate = item.date
                                            signOffRemarks = "${item.signOffTag} - EDR Acceptance Verification"
                                            viewModel.showToast("Loaded ${item.number} into manual form below!")
                                        },
                                        onSaveRecord = {
                                            viewModel.insertParsedInspectionRecords(
                                                listOf(item),
                                                overrideModule = overrideBulkModule ?: item.module
                                            )
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // One-Tap Bulk Import Button
                                Button(
                                    onClick = {
                                        viewModel.insertParsedInspectionRecords(
                                            result.items,
                                            overrideModule = overrideBulkModule ?: selectedModule
                                        )
                                        rawPasteText = ""
                                        parseResult = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EDRGreen, contentColor = BgDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Import All (${result.items.size}) Records to Database & Cloud",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else if (result != null && !result.success) {
                        Surface(
                            color = EDRRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EDRRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.summaryMessage,
                                color = EDRRed,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Data Entry — Acceptance & QC $moduleTitle",
                    icon = Icons.Default.EditNote,
                    accentColor = accentColor
                )

                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${moduleRecords.size} Saved",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Main Data Entry Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "NEW $moduleTitle ACCEPTANCE RECORD".uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 0.6.sp
                    )

                    // 1. Rolling Stock Number & Quick Preset Numbers
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$moduleTitle Number / Car ID *", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        OutlinedTextField(
                            value = equipmentNumber,
                            onValueChange = {
                                if (SmartDataEntryParser.isBatchInspectionNotes(it)) {
                                    rawPasteText = it
                                    val res = SmartDataEntryParser.parseInspectionReport(it, defaultModule = selectedModule, fallbackDate = viewModel.todayDateStr())
                                    parseResult = res
                                    isSmartPasteExpanded = true
                                    viewModel.showToast("Batch inspector notes detected! Arranged ${res.items.size} records above.")
                                } else {
                                    equipmentNumber = it
                                }
                            },
                            placeholder = { Text(if (selectedModule == "wagon") "e.g. W-1052" else if (selectedModule == "locomotive") "e.g. LC-008" else "e.g. CH-019") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = InputDark,
                                unfocusedContainerColor = InputDark,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        // Quick fill buttons
                        val quickNumbers = when (selectedModule) {
                            "wagon" -> listOf("W-1042", "W-2015", "W-3088", "W-4001")
                            "locomotive" -> listOf("LC-005", "LC-008", "LC-012", "LC-018")
                            "coach" -> listOf("CH-007", "CH-014", "CH-022", "CH-030")
                            else -> emptyList()
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text("Presets:", fontSize = 9.sp, color = TextMuted, modifier = Modifier.align(Alignment.CenterVertically))
                            quickNumbers.forEach { num ->
                                Surface(
                                    color = CardDark2,
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                                    modifier = Modifier.clickable { equipmentNumber = num }
                                ) {
                                    Text(
                                        text = num,
                                        fontSize = 9.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Rolling Stock Class / Subtype
                    val carTypes = when (selectedModule) {
                        "wagon" -> listOf("Flat Container Car", "Boxcar (Enclosed)", "Tanker Wagon", "Gondola (Open Top)", "Hopper (Grain/Ballast)")
                        "locomotive" -> listOf("HXD1C Electric Mainline", "SS7D Electric Passenger", "DF4DF Diesel Shunting", "Depot Switcher")
                        "coach" -> listOf("Hard Seat (YZ25G)", "Hard Sleeper (YW25G)", "Soft Sleeper (RW25G)", "Dining Car (CA25G)", "Generator Car (KD25G)")
                        else -> listOf("Standard")
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Equipment Subtype / Rolling Stock Class", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            carTypes.take(3).forEach { type ->
                                FilterChip(
                                    selected = selectedCarType == type,
                                    onClick = { selectedCarType = type },
                                    label = { Text(type, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                        selectedLabelColor = accentColor
                                    )
                                )
                            }
                        }
                    }

                    // 3. Maintenance Type & Station / Depot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Maintenance Type *", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            val maintTypes = listOf("Preventive", "Corrective", "Inspection", "Overhaul", "Emergency")
                            var expandedMaint by remember { mutableStateOf(false) }

                            Box {
                                OutlinedTextField(
                                    value = maintenanceType,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { expandedMaint = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { expandedMaint = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = InputDark,
                                        unfocusedContainerColor = InputDark,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                DropdownMenu(
                                    expanded = expandedMaint,
                                    onDismissRequest = { expandedMaint = false }
                                ) {
                                    maintTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                maintenanceType = type
                                                expandedMaint = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Depot / Yard Station", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            val depots = listOf("Indode Depot (Addis)", "Mojo Dry Port Yard", "Adama Station", "Dire Dawa Workshop", "Nagad Depot (Djibouti)")
                            var expandedDepot by remember { mutableStateOf(false) }

                            Box {
                                OutlinedTextField(
                                    value = selectedDepot,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { expandedDepot = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { expandedDepot = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = InputDark,
                                        unfocusedContainerColor = InputDark,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                DropdownMenu(
                                    expanded = expandedDepot,
                                    onDismissRequest = { expandedDepot = false }
                                ) {
                                    depots.forEach { depot ->
                                        DropdownMenuItem(
                                            text = { Text(depot) },
                                            onClick = {
                                                selectedDepot = depot
                                                expandedDepot = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Inspection Subsystem Tested
                    val subsystems = when (selectedModule) {
                        "wagon" -> listOf("Bogie & Wheelset", "Air Brake System (500kPa)", "Coupler & Knuckle", "Center Plate & Side Bearer", "Container Twist-locks")
                        "locomotive" -> listOf("Traction Motor & Bogie", "Pantograph & 25kV Circuit", "Air Compressor & JZ-7 Valve", "Speed Recorder (TELOC)", "Cab Controls & Vigilance")
                        "coach" -> listOf("HVAC & Temperature Control", "Passenger Plug Doors", "Bogie Suspension & Air Springs", "Emergency Brake Valve", "Lighting & Sanitary System")
                        else -> listOf("General Inspection")
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Subsystem Inspected / Quality Checklist", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            subsystems.take(3).forEach { sub ->
                                FilterChip(
                                    selected = selectedSubsystem == sub,
                                    onClick = {
                                        selectedSubsystem = sub
                                        if (description.isBlank()) {
                                            description = "$sub inspection and acceptance verification."
                                        }
                                    },
                                    label = { Text(sub, fontSize = 9.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                        selectedLabelColor = accentColor
                                    )
                                )
                            }
                        }
                    }

                    // 5. Description / Finding
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Inspection Finding / Description *", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                if (SmartDataEntryParser.isBatchInspectionNotes(it)) {
                                    rawPasteText = it
                                    val res = SmartDataEntryParser.parseInspectionReport(it, defaultModule = selectedModule, fallbackDate = viewModel.todayDateStr())
                                    parseResult = res
                                    isSmartPasteExpanded = true
                                    viewModel.showToast("Batch inspector notes detected! Arranged ${res.items.size} records above.")
                                } else {
                                    description = it
                                }
                            },
                            placeholder = { Text("Describe acceptance check results, measured clearances, or maintenance performed...") },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = InputDark,
                                unfocusedContainerColor = InputDark,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // 6. Fault / Defect & Spare Parts Used
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Defect / Non-conformance", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            OutlinedTextField(
                                value = faultDefect,
                                onValueChange = { faultDefect = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = InputDark,
                                    unfocusedContainerColor = InputDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Spare Parts Consumed", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            OutlinedTextField(
                                value = sparePartsUsed,
                                onValueChange = { sparePartsUsed = it },
                                placeholder = { Text("e.g. Brake shoes, Gasket, Oil filter") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = InputDark,
                                    unfocusedContainerColor = InputDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }

                    // 7. Status & Inspector Name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Acceptance Status", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            val statusOptions = listOf("Completed", "In Progress", "Pending", "On Hold")
                            var expandedStatus by remember { mutableStateOf(false) }

                            Box {
                                OutlinedTextField(
                                    value = inspectionStatus,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { expandedStatus = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { expandedStatus = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = InputDark,
                                        unfocusedContainerColor = InputDark,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                DropdownMenu(
                                    expanded = expandedStatus,
                                    onDismissRequest = { expandedStatus = false }
                                ) {
                                    statusOptions.forEach { st ->
                                        DropdownMenuItem(
                                            text = { Text(st) },
                                            onClick = {
                                                inspectionStatus = st
                                                expandedStatus = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Inspector / Engineer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            OutlinedTextField(
                                value = inspectorName,
                                onValueChange = { inspectorName = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = InputDark,
                                    unfocusedContainerColor = InputDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }

                    // 8. Dates (Inspection Date & Finish Date)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Inspection Date (YYYY-MM-DD)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            OutlinedTextField(
                                value = inspectionDate,
                                onValueChange = { inspectionDate = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = InputDark,
                                    unfocusedContainerColor = InputDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Completion Date", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            OutlinedTextField(
                                value = finishDate,
                                onValueChange = { finishDate = it },
                                placeholder = { Text(inspectionDate) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = InputDark,
                                    unfocusedContainerColor = InputDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }

                    // 9. Sign-off Remarks
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sign-off Remarks & Quality Certification", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        OutlinedTextField(
                            value = signOffRemarks,
                            onValueChange = { signOffRemarks = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = InputDark,
                                unfocusedContainerColor = InputDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    if (formMessage != null) {
                        Surface(
                            color = EDRGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EDRGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formMessage ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EDRGreen,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Submit & Clear Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (equipmentNumber.isBlank() || equipmentNumber == "W-" || equipmentNumber == "LC-" || equipmentNumber == "CH-") {
                                    viewModel.showToast("Please enter a valid $moduleTitle number")
                                    return@Button
                                }
                                val effectiveDesc = if (description.isBlank()) {
                                    "$selectedCarType - $selectedSubsystem inspection completed at $selectedDepot."
                                } else {
                                    "[$selectedCarType | $selectedSubsystem] $description (Depot: $selectedDepot)"
                                }

                                val fullRemarks = "Inspector: $inspectorName | Depot: $selectedDepot | $signOffRemarks"

                                viewModel.addQC(
                                    module = selectedModule,
                                    number = equipmentNumber.trim(),
                                    maintenanceType = maintenanceType,
                                    desc = effectiveDesc,
                                    fault = faultDefect.trim(),
                                    sparePart = sparePartsUsed.trim(),
                                    status = inspectionStatus,
                                    startDate = inspectionDate.trim().ifBlank { viewModel.todayDateStr() },
                                    finishDate = finishDate.trim().ifBlank { inspectionDate.trim() },
                                    remark = fullRemarks
                                )

                                formMessage = "✓ $moduleTitle #$equipmentNumber successfully saved to database & cloud!"
                                viewModel.showToast("$moduleTitle record added successfully")

                                // Reset form number for next entry
                                equipmentNumber = when (selectedModule) {
                                    "wagon" -> "W-"
                                    "locomotive" -> "LC-"
                                    "coach" -> "CH-"
                                    else -> ""
                                }
                                description = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = BgDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.4f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save $moduleTitle Record", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val targetScreen = when (selectedModule) {
                                    "locomotive" -> "qcLocomotive"
                                    "coach" -> "qcCoach"
                                    else -> "qcWagon"
                                }
                                viewModel.navigateTo(targetScreen)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View All QC", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section: Live Recent Entries for Selected Module
        item {
            SectionHeader(
                title = "Recent $moduleTitle Data Entries (${moduleRecords.size})",
                icon = Icons.Default.History,
                accentColor = accentColor
            )
        }

        if (moduleRecords.isEmpty()) {
            item {
                EmptyStateBox("No data entries registered yet for $moduleTitle.")
            }
        } else {
            items(moduleRecords.take(8), key = { it.id }) { item ->
                QCRecordRowCard(
                    item = item,
                    isChecked = false,
                    onToggleCheck = {},
                    onEdit = {
                        equipmentNumber = item.number
                        maintenanceType = item.maintenanceType
                        description = item.description
                        faultDefect = item.fault
                        sparePartsUsed = item.sparePart
                        inspectionStatus = item.status
                        inspectionDate = item.startDate
                        finishDate = item.finishDate
                    },
                    onCancel = { viewModel.cancelItem("qc", item.id, "Cancelled by user") },
                    onTrash = { viewModel.trashItem("qc", item.id, "Deleted from data entry") },
                    onReinstate = { viewModel.reinstateItem("qc", item.id) }
                )
            }
        }
    }
}

@Composable
fun DataEntryModuleTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) color.copy(alpha = 0.18f) else CardDark2,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) color else BorderDark
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ArrangedItemPreviewCard(
    item: ParsedInspectionItem,
    onLoadToForm: () -> Unit,
    onSaveRecord: (() -> Unit)? = null
) {
    Surface(
        color = CardDark,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header Row: Vehicle Number, Module Chip, Date & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = AccentAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.4f))
                    ) {
                        Text(
                            "${item.index}. ${item.number}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = EDRCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            item.module.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EDRCyan,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Text(item.date, fontSize = 10.sp, color = TextMuted)
                }

                Surface(
                    color = EDRGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EDRGreen.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EDRGreen, modifier = Modifier.size(11.dp))
                        Text(
                            item.signOffTag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EDRGreen
                        )
                    }
                }
            }

            // Subsystem Tag
            Text(
                text = "Subsystem: ${item.subsystem}",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = EDRCyan
            )

            // Technical Measurements Row (Coupler, Flow Machine, Brake Check)
            if (item.couplerSide1.isNotBlank() || item.couplerSide2.isNotBlank() || item.flowDetection.isNotBlank() || item.brakeStatus.isNotBlank()) {
                Surface(
                    color = BgDark.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (item.couplerSide1.isNotBlank() || item.couplerSide2.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Coupler Height:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                if (item.couplerSide1.isNotBlank()) {
                                    Surface(color = EDRViolet.copy(alpha = 0.2f), shape = RoundedCornerShape(3.dp)) {
                                        Text("Side 1: ${item.couplerSide1}", fontSize = 9.5.sp, color = EDRViolet, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                    }
                                }
                                if (item.couplerSide2.isNotBlank()) {
                                    Surface(color = EDRViolet.copy(alpha = 0.2f), shape = RoundedCornerShape(3.dp)) {
                                        Text("Side 2: ${item.couplerSide2}", fontSize = 9.5.sp, color = EDRViolet, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                    }
                                }
                            }
                        }

                        if (item.flowDetection.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(10.dp))
                                Text("Flow Detection: ${item.flowDetection}", fontSize = 9.5.sp, color = AccentAmber)
                            }
                        }

                        if (item.brakeStatus.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = EDRGreen, modifier = Modifier.size(10.dp))
                                Text("Brake System: ${item.brakeStatus}", fontSize = 9.5.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Fault Record
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("FAULT RECORD:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                Text(
                    text = item.fault,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Taken
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("ACTION TAKEN:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                Text(
                    text = item.actionTaken,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }

            // Spares Changed
            if (item.spareParts.isNotBlank() && item.spareParts != "None (Adjustment & Verification)") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Replaced Spares:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text(
                        text = item.spareParts,
                        fontSize = 10.sp,
                        color = EDRCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Divider(color = BorderDark, thickness = 0.5.dp)

            // Bottom Actions: Fill Form or Save Directly
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onLoadToForm,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = EDRCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fill in Manual Form", fontSize = 10.sp, color = EDRCyan)
                }

                if (onSaveRecord != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onSaveRecord,
                        colors = ButtonDefaults.buttonColors(containerColor = EDRGreen, contentColor = BgDark),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save This Record", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
