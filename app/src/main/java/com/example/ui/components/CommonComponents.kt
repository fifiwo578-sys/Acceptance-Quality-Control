package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderDark, BorderDark2)))
    ) {
        Column {
            // Gradient top border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.4f))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, fg) = when (status.lowercase()) {
        "completed", "published" -> Pair(EDRGreen.copy(alpha = 0.15f), EDRGreen)
        "in progress" -> Pair(EDRCyan.copy(alpha = 0.15f), EDRCyan)
        "pending", "draft" -> Pair(EDROrange.copy(alpha = 0.15f), EDROrange)
        "notification sent" -> Pair(EDRViolet.copy(alpha = 0.15f), EDRViolet)
        "on hold" -> Pair(EDRRed.copy(alpha = 0.15f), EDRRed)
        "cancelled" -> Pair(TextMuted.copy(alpha = 0.2f), TextMuted)
        else -> Pair(AccentAmber.copy(alpha = 0.15f), AccentAmber)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, fg.copy(alpha = 0.3f))
    ) {
        Text(
            text = status.uppercase(),
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun FilterTab(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(6.dp)),
        color = if (isSelected) AccentAmber.copy(alpha = 0.12f) else CardDark2,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) AccentAmber.copy(alpha = 0.3f) else BorderDark
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) AccentAmber else TextSecondary
            )
            Surface(
                color = if (isSelected) AccentAmber.copy(alpha = 0.2f) else CardDark,
                shape = CircleShape
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) AccentAmber else TextMuted,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    accentColor: Color = AccentAmber,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        action?.invoke()
    }
}

@Composable
fun EmptyStateBox(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark2),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = message,
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = CardDark2,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun DataMetricBox(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardDark2),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
            Text(
                text = count,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ReportSectionHeader(number: Int, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = AccentAmber.copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "$number",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AccentAmber,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
fun KPIStatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun RenderQCSublist(items: List<com.example.data.model.QCEntity>, color: Color) {
    if (items.isEmpty()) {
        Surface(
            color = CardDark2,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "No records logged for this section on this date.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(10.dp)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEach { item ->
                Surface(
                    color = CardDark2,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${item.number} • ${item.maintenanceType}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                            if (item.description.isNotBlank()) {
                                Text(
                                    text = item.description,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        StatusBadge(status = item.status)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderTaskSublist(items: List<com.example.data.model.TaskEntity>) {
    if (items.isEmpty()) {
        Surface(
            color = CardDark2,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "No tasks registered for this section on this date.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(10.dp)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEach { task ->
                Surface(
                    color = CardDark2,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ${task.description}",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (task.remark.isNotBlank()) {
                            Text(
                                text = task.remark,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipientRowCard(
    recipient: com.example.data.model.RecipientEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onSendEmail: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (recipient.isActive) EDRGreen.copy(alpha = 0.2f) else TextMuted.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = if (recipient.isActive) EDRGreen else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = recipient.name.ifBlank { "Recipient" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = recipient.email,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (onSendEmail != null) {
                    IconButton(onClick = onSendEmail) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Email",
                            tint = EDRCyan,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Switch(
                    checked = recipient.isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EDRGreen,
                        checkedTrackColor = EDRGreen.copy(alpha = 0.3f)
                    )
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = EDRRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationLogRowCard(log: com.example.data.model.NotificationLogEntity) {
    Surface(
        color = CardDark,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = log.recipientEmail,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Report: ${log.reportDate} • ${log.sentAt}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
            StatusBadge(status = log.status)
        }
    }
}

@Composable
fun TrashItemRow(
    title: String,
    subtitle: String,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, fontSize = 10.sp, color = TextMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = EDRGreen, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onPermanentDelete) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Permanent Delete", tint = EDRRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun CancelledItemRow(
    title: String,
    subtitle: String,
    onReinstate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, fontSize = 10.sp, color = TextMuted)
            }
            Button(
                onClick = onReinstate,
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber.copy(alpha = 0.2f), contentColor = AccentAmber),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Reinstate", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
