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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyReportEntity
import com.example.data.model.NotificationLogEntity
import com.example.data.model.RecipientEntity
import com.example.ui.EdrViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun EmailNotificationsScreen(viewModel: EdrViewModel) {
    val context = LocalContext.current
    val reports by viewModel.reports.collectAsState()
    val recipients by viewModel.recipients.collectAsState()
    val logs by viewModel.notificationLogs.collectAsState()
    val cloudConfig by viewModel.cloudConfig.collectAsState()

    var showAddRecipientDialog by remember { mutableStateOf(false) }
    var showTestEmailDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showClearLogsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
    ) {
        // Firebase Cloud Connection & Sync Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EDRGreen.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = EDRGreen, modifier = Modifier.size(20.dp))
                            Text("Firebase Cloud Connection", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Button(
                            onClick = { viewModel.syncWithFirebase() },
                            colors = ButtonDefaults.buttonColors(containerColor = EDRGreen, contentColor = BgDark),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Connected to Firebase Realtime Database & Cloud Storage. All daily reports and recipient notifications sync so multiple authorized users share identical records.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )

                    Surface(
                        color = InputDark,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Database URL: ${cloudConfig.firebaseDatabaseUrl}",
                            fontSize = 10.sp,
                            color = EDRCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Overview & Service Card
        val latestReport = reports.firstOrNull()

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                            Text("Email Notification Service", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Surface(
                                color = EDRGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EDRGreen.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    "NO API KEY NEEDED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EDRGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        IconButton(onClick = { showConfigDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AccentAmber)
                        }
                    }

                    Text(
                        text = "Send official Acceptance & Quality Control daily reports directly using your device's built-in email client (Gmail, Outlook, Android Email). All active recipients are automatically populated with full inspection summaries — no paid Resend API key or external credentials required.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (latestReport != null) {
                                    viewModel.sendReportPdfViaEmail(context, latestReport)
                                } else {
                                    viewModel.showToast("No report created yet. Compile a report in Daily Reports.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EDRGreen, contentColor = TextOnGreen),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send Report (PDF)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showAddRecipientDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BgDark),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Recipient", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showTestEmailDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark2),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp), tint = EDRCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send Test Email", fontSize = 11.sp)
                        }

                        if (latestReport != null) {
                            OutlinedButton(
                                onClick = { viewModel.copyReportToClipboard(context, latestReport) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark2),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = AccentAmber)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Text", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Scheduled Notification Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                            Text("Scheduled Daily Dispatch", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(
                            text = "Auto-dispatch time: ${cloudConfig.scheduledTime} (${cloudConfig.scheduledTimezone})",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = cloudConfig.scheduledEnabled,
                        onCheckedChange = { isEnabled ->
                            viewModel.saveConfig(cloudConfig.copy(scheduledEnabled = isEnabled))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentAmber,
                            checkedTrackColor = AccentAmber.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CardDark2
                        )
                    )
                }
            }
        }

        // Section: Configured Recipients
        item {
            SectionHeader(
                title = "Configured Recipients (${recipients.size})",
                icon = Icons.Default.People,
                accentColor = EDRCyan
            )
        }

        if (recipients.isEmpty()) {
            item {
                EmptyStateBox("No email recipients added. Tap 'Add Recipient' above.")
            }
        } else {
            items(recipients) { recipient ->
                RecipientRowCard(
                    recipient = recipient,
                    onToggle = { viewModel.toggleRecipientActive(recipient) },
                    onDelete = { viewModel.deleteRecipient(recipient.id) },
                    onSendEmail = {
                        viewModel.launchSingleRecipientEmail(context, recipient.email, latestReport)
                    }
                )
            }
        }

        // Section: Notification History Logs
        item {
            SectionHeader(
                title = "Notification History Logs (${logs.size})",
                icon = Icons.Default.ListAlt,
                accentColor = EDRViolet,
                action = if (logs.isNotEmpty()) {
                    {
                        TextButton(
                            onClick = { showClearLogsDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = EDRRed)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = EDRRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Logs", color = EDRRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else null
            )
        }

        if (logs.isEmpty()) {
            item {
                EmptyStateBox("No email dispatch logs yet.")
            }
        } else {
            items(logs) { log ->
                NotificationLogRowCard(log = log)
            }
        }
    }

    // Modal: Add Recipient Dialog
    if (showAddRecipientDialog) {
        var emailInput by remember { mutableStateOf("") }
        var nameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddRecipientDialog = false },
            title = { Text("Add Email Recipient", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentAmber) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address *") },
                        placeholder = { Text("recipient@example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = InputDark,
                            unfocusedContainerColor = InputDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Recipient Name / Title") },
                        placeholder = { Text("e.g. Chief Quality Officer") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = InputDark,
                            unfocusedContainerColor = InputDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddRecipientDialog = false
                        viewModel.addRecipient(emailInput.trim(), nameInput.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BgDark)
                ) {
                    Text("Add Recipient", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRecipientDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = CardDark
        )
    }

    // Modal: Send Test Email
    if (showTestEmailDialog) {
        var testEmailInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showTestEmailDialog = false },
            title = { Text("Send Test Email Notification", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EDRGreen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter recipient email address. No Resend API key is required — you can open directly in your mail app or record in dispatch history:", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = testEmailInput,
                        onValueChange = { testEmailInput = it },
                        placeholder = { Text("your.email@example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = InputDark,
                            unfocusedContainerColor = InputDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (testEmailInput.isNotBlank()) {
                                showTestEmailDialog = false
                                viewModel.launchTestEmailComposer(context, testEmailInput.trim())
                            } else {
                                viewModel.showToast("Please enter an email address")
                            }
                        }
                    ) {
                        Text("Open in Mail App", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            if (testEmailInput.isNotBlank()) {
                                showTestEmailDialog = false
                                viewModel.sendTestNotification(testEmailInput.trim())
                            } else {
                                viewModel.showToast("Please enter an email address")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EDRGreen, contentColor = BgDark)
                    ) {
                        Text("Send & Log", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTestEmailDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = CardDark
        )
    }

    // Modal: Config & API Keys Dialog
    if (showConfigDialog) {
        var firebaseDbUrl by remember {
            mutableStateOf(
                if (cloudConfig.firebaseDatabaseUrl.isBlank() || cloudConfig.firebaseDatabaseUrl != "https://acceptance-8781f-default-rtdb.firebaseio.com")
                    "https://acceptance-8781f-default-rtdb.firebaseio.com"
                else cloudConfig.firebaseDatabaseUrl
            )
        }
        var resendKey by remember { mutableStateOf(cloudConfig.resendApiKey) }
        var fromEmail by remember { mutableStateOf(cloudConfig.reportFromEmail) }
        var publicUrl by remember { mutableStateOf(cloudConfig.appPublicUrl) }
        var schedTime by remember { mutableStateOf(cloudConfig.scheduledTime) }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Firebase & Email Configuration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentAmber) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Zero API key is required to send emails via your device! Configure Firebase Database URL and optional parameters below:", fontSize = 11.sp, color = TextSecondary)

                    OutlinedTextField(
                        value = firebaseDbUrl,
                        onValueChange = { firebaseDbUrl = it },
                        label = { Text("Firebase Database URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    OutlinedTextField(
                        value = resendKey,
                        onValueChange = { resendKey = it },
                        label = { Text("Optional Resend API Key (re_...)") },
                        placeholder = { Text("Leave blank for Native Mail App") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    OutlinedTextField(
                        value = fromEmail,
                        onValueChange = { fromEmail = it },
                        label = { Text("REPORT_FROM_EMAIL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    OutlinedTextField(
                        value = publicUrl,
                        onValueChange = { publicUrl = it },
                        label = { Text("APP_PUBLIC_URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    OutlinedTextField(
                        value = schedTime,
                        onValueChange = { schedTime = it },
                        label = { Text("Daily Schedule Time (e.g. 18:00)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputDark, unfocusedContainerColor = InputDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfigDialog = false
                        viewModel.saveConfig(
                            cloudConfig.copy(
                                firebaseDatabaseUrl = firebaseDbUrl.trim(),
                                resendApiKey = resendKey.trim(),
                                reportFromEmail = fromEmail.trim(),
                                appPublicUrl = publicUrl.trim(),
                                scheduledTime = schedTime.trim()
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BgDark)
                ) {
                    Text("Save Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = CardDark
        )
    }

    if (showClearLogsDialog) {
        AlertDialog(
            onDismissRequest = { showClearLogsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = EDRRed)
                    Text("Clear Notification Logs", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EDRRed)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to clear all email notification dispatch logs? This cannot be undone.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearLogsDialog = false
                        viewModel.clearNotificationHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EDRRed, contentColor = Color.White)
                ) {
                    Text("Clear Logs", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }
}
