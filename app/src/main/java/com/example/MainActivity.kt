package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.viewModels
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.EdrViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: EdrViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleReportIntent(intent)
        setContent {
            MyApplicationTheme {
                MainApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleReportIntent(intent)
    }

    private fun handleReportIntent(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        val urlString = uri.toString()
        // Extract YYYY-MM-DD from URL path or query
        val dateRegex = Regex("""\b(\d{4}-\d{2}-\d{2})\b""")
        val match = dateRegex.find(urlString)
        if (match != null) {
            val date = match.value
            viewModel.viewReport(date)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: EdrViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    val allQC by viewModel.allQC.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val trashQC by viewModel.trashQC.collectAsState()
    val trashTasks by viewModel.trashTasks.collectAsState()
    val cancelledQC by viewModel.cancelledQC.collectAsState()
    val cancelledTasks by viewModel.cancelledTasks.collectAsState()

    val totalRecords = allQC.size + allTasks.size
    val totalArchive = trashQC.size + trashTasks.size + cancelledQC.size + cancelledTasks.size

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = currentScreen != "dashboard") {
        viewModel.navigateTo("dashboard")
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerContentColor = TextPrimary,
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
            ) {
                // Sidebar Logo Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandGreen)
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Acceptance & QC",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Ethio-Djibouti Railway",
                                fontSize = 10.5.sp,
                                color = BrandGreenSubtle
                            )
                        }
                    }
                }

                Divider(color = BorderDark, thickness = 1.dp)

                // Navigation Items List (Monitoring Incident & Monitoring Problem are REMOVED)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    DrawerItem(
                        title = "Dashboard",
                        icon = Icons.Default.Dashboard,
                        accentColor = AccentAmber,
                        isSelected = currentScreen == "dashboard"
                    ) {
                        viewModel.navigateTo("dashboard")
                        coroutineScope.launch { drawerState.close() }
                    }

                    DrawerSectionTitle("Data Entry")
                    DrawerItem("Data Entry - Wagon", Icons.Default.EditNote, EDRViolet, currentScreen == "dataEntryWagon") {
                        viewModel.navigateTo("dataEntryWagon")
                        coroutineScope.launch { drawerState.close() }
                    }
                    DrawerItem("Data Entry - Locomotive", Icons.Default.EditNote, EDRCyan, currentScreen == "dataEntryLocomotive") {
                        viewModel.navigateTo("dataEntryLocomotive")
                        coroutineScope.launch { drawerState.close() }
                    }
                    DrawerItem("Data Entry - Coach", Icons.Default.EditNote, EDRGreen, currentScreen == "dataEntryCoach") {
                        viewModel.navigateTo("dataEntryCoach")
                        coroutineScope.launch { drawerState.close() }
                    }

                    DrawerSectionTitle("Quality Control")
                    DrawerItem("QC - Wagon", Icons.Default.DirectionsRailway, EDRViolet, currentScreen == "qcWagon") {
                        viewModel.navigateTo("qcWagon")
                        coroutineScope.launch { drawerState.close() }
                    }
                    DrawerItem("QC - Locomotive", Icons.Default.Train, EDRCyan, currentScreen == "qcLocomotive") {
                        viewModel.navigateTo("qcLocomotive")
                        coroutineScope.launch { drawerState.close() }
                    }
                    DrawerItem("QC - Coach", Icons.Default.Commute, EDRGreen, currentScreen == "qcCoach") {
                        viewModel.navigateTo("qcCoach")
                        coroutineScope.launch { drawerState.close() }
                    }

                    DrawerSectionTitle("Additional Tasks")
                    DrawerItem("AQMS Task", Icons.Default.AssignmentTurnedIn, AccentAmber, currentScreen == "taskAQMS") {
                        viewModel.navigateTo("taskAQMS")
                        coroutineScope.launch { drawerState.close() }
                    }
                    DrawerItem("Wagon Task", Icons.Default.Checklist, EDROrange, currentScreen == "taskWagon") {
                        viewModel.navigateTo("taskWagon")
                        coroutineScope.launch { drawerState.close() }
                    }
                    DrawerItem("Locomotive Task", Icons.Default.Checklist, EDRCyan, currentScreen == "taskLocomotive") {
                        viewModel.navigateTo("taskLocomotive")
                        coroutineScope.launch { drawerState.close() }
                    }
                    DrawerItem("Coach Task", Icons.Default.Checklist, EDRGreen, currentScreen == "taskCoach") {
                        viewModel.navigateTo("taskCoach")
                        coroutineScope.launch { drawerState.close() }
                    }
                    DrawerItem("Equipment Task", Icons.Default.Build, EDRViolet, currentScreen == "taskEquipment") {
                        viewModel.navigateTo("taskEquipment")
                        coroutineScope.launch { drawerState.close() }
                    }

                    DrawerSectionTitle("Reporting")
                    DrawerItem("Daily Reports", Icons.Default.Assessment, AccentAmber, currentScreen == "dailyReports" || currentScreen == "dailyReportView", badge = "ACTIVE") {
                        viewModel.navigateTo("dailyReports")
                        coroutineScope.launch { drawerState.close() }
                    }
                    DrawerItem("Report History", Icons.Default.History, EDRCyan, currentScreen == "reportHistory") {
                        viewModel.navigateTo("reportHistory")
                        coroutineScope.launch { drawerState.close() }
                    }
                    DrawerItem("Email Notifications", Icons.Default.Email, EDRGreen, currentScreen == "emailNotifications") {
                        viewModel.navigateTo("emailNotifications")
                        coroutineScope.launch { drawerState.close() }
                    }
                    DrawerItem("Reports Generator", Icons.Default.Description, TextSecondary, currentScreen == "reports") {
                        viewModel.navigateTo("reports")
                        coroutineScope.launch { drawerState.close() }
                    }

                    DrawerSectionTitle("Data")
                    DrawerItem("Data Management", Icons.Default.Storage, AccentAmber, currentScreen == "dataManagement") {
                        viewModel.navigateTo("dataManagement")
                        coroutineScope.launch { drawerState.close() }
                    }
                    DrawerItem(
                        title = "Trash & Cancelled",
                        icon = Icons.Default.DeleteOutline,
                        accentColor = EDRRed,
                        isSelected = currentScreen == "trash",
                        badge = if (totalArchive > 0) totalArchive.toString() else null
                    ) {
                        viewModel.navigateTo("trash")
                        coroutineScope.launch { drawerState.close() }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BrandGreen,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val screenTitle = when (currentScreen) {
                                "dashboard" -> "Acceptance and Quality Control"
                                "dataEntryWagon" -> "Data Entry - Wagon"
                                "dataEntryLocomotive" -> "Data Entry - Locomotive"
                                "dataEntryCoach" -> "Data Entry - Coach"
                                "qcWagon" -> "QC - Wagon"
                                "qcLocomotive" -> "QC - Locomotive"
                                "qcCoach" -> "QC - Coach"
                                "taskAQMS" -> "AQMS Task"
                                "taskWagon" -> "Wagon Task"
                                "taskLocomotive" -> "Locomotive Task"
                                "taskCoach" -> "Coach Task"
                                "taskEquipment" -> "Equipment Task"
                                "dailyReports" -> "Daily Reports"
                                "dailyReportView" -> "Daily Report Document"
                                "reportHistory" -> "Report History"
                                "emailNotifications" -> "Email Notifications"
                                "reports" -> "Custom Reports"
                                "dataManagement" -> "Data Management"
                                "trash" -> "Trash & Cancelled"
                                else -> "Acceptance and Quality Control"
                            }
                            Text(
                                text = screenTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        // Storage / Record Count Pill
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Text(
                                    text = "$totalRecords QC recs",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Admin / Public Mode Toggle
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clickable {
                                    viewModel.isAdmin.value = !isAdmin
                                    viewModel.showToast(if (!isAdmin) "Switched to Admin Mode" else "Switched to Public View Mode")
                                }
                                .padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isAdmin) "ADMIN" else "PUBLIC",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreen
                                )
                            }
                        }
                    }
                )
            },
            containerColor = BgDark
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    "dashboard" -> DashboardScreen(viewModel)
                    "dataEntryWagon" -> DataEntryScreen("wagon", viewModel)
                    "dataEntryLocomotive" -> DataEntryScreen("locomotive", viewModel)
                    "dataEntryCoach" -> DataEntryScreen("coach", viewModel)
                    "qcWagon" -> ModuleRecordsScreen("qc_wagon", "QC - Wagon", EDRViolet, viewModel)
                    "qcLocomotive" -> ModuleRecordsScreen("qc_locomotive", "QC - Locomotive", EDRCyan, viewModel)
                    "qcCoach" -> ModuleRecordsScreen("qc_coach", "QC - Coach", EDRGreen, viewModel)
                    "taskAQMS" -> ModuleRecordsScreen("task_aqms", "AQMS Additional Task", AccentAmber, viewModel)
                    "taskWagon" -> ModuleRecordsScreen("task_wagon", "Wagon Additional Task", EDROrange, viewModel)
                    "taskLocomotive" -> ModuleRecordsScreen("task_locomotive", "Locomotive Additional Task", EDRCyan, viewModel)
                    "taskCoach" -> ModuleRecordsScreen("task_coach", "Coach Additional Task", EDRGreen, viewModel)
                    "taskEquipment" -> ModuleRecordsScreen("task_equipment", "Equipment Additional Task", EDRViolet, viewModel)
                    "dailyReports" -> DailyReportsScreen(viewModel)
                    "dailyReportView" -> DailyReportViewScreen(viewModel)
                    "reportHistory" -> ReportHistoryScreen(viewModel)
                    "emailNotifications" -> EmailNotificationsScreen(viewModel)
                    "reports" -> CustomReportsScreen(viewModel)
                    "dataManagement" -> DataManagementScreen(viewModel)
                    "trash" -> TrashScreen(viewModel)
                    else -> DashboardScreen(viewModel)
                }

                if (isBusy) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentAmber)
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 4.dp)
    )
}

@Composable
fun DrawerItem(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (isSelected) BrandGreenSubtle else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) BrandGreen else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BrandGreen else TextSecondary
                )
            }

            if (badge != null) {
                Surface(
                    color = if (badge == "ACTIVE") BrandGreen else EDRRed,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
