package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String = "",
    val trainNumber: String = "",
    val location: String = "",
    val trainCaptain: String = "",
    val accidentType: String = "",
    val isCancelled: Boolean = false,
    val cancelReason: String = "",
    val cancelledAt: String = "",
    val isTrash: Boolean = false,
    val trashReason: String = "",
    val deletedAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "problems")
data class ProblemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String = "",
    val time: String = "",
    val locoNo: String = "",
    val trainNo: String = "",
    val mileage: String = "",
    val captainId: String = "",
    val captainName: String = "",
    val monitoringProblems: String = "",
    val frequency: String = "",
    val isCancelled: Boolean = false,
    val cancelReason: String = "",
    val cancelledAt: String = "",
    val isTrash: Boolean = false,
    val trashReason: String = "",
    val deletedAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "qc_records")
data class QCEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val module: String = "wagon", // "wagon", "locomotive", "coach"
    val number: String = "",
    val maintenanceType: String = "",
    val description: String = "",
    val fault: String = "",
    val sparePart: String = "",
    val status: String = "Pending", // "Pending", "In Progress", "Completed", "On Hold", "Cancelled"
    val startDate: String = "",
    val finishDate: String = "",
    val remark: String = "",
    val isCancelled: Boolean = false,
    val cancelReason: String = "",
    val cancelledAt: String = "",
    val isTrash: Boolean = false,
    val trashReason: String = "",
    val deletedAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val module: String = "aqms", // "aqms", "wagon", "locomotive", "coach", "equipment"
    val date: String = "",
    val description: String = "",
    val remark: String = "",
    val isCancelled: Boolean = false,
    val cancelReason: String = "",
    val cancelledAt: String = "",
    val isTrash: Boolean = false,
    val trashReason: String = "",
    val deletedAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "reports")
data class DailyReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportDate: String = "", // e.g., "2026-09-22"
    val status: String = "Draft", // "Draft", "Published", "Notification Sent"
    val title: String = "Ethio-Djibouti Railway Acceptance and Quality Control Daily Report",
    val executiveSummary: String = "",
    val qcWagonCount: Int = 0,
    val qcLocoCount: Int = 0,
    val qcCoachCount: Int = 0,
    val aqmsTaskCount: Int = 0,
    val wagonTaskCount: Int = 0,
    val locoTaskCount: Int = 0,
    val coachTaskCount: Int = 0,
    val equipmentTaskCount: Int = 0,
    val statsJson: String = "",
    val remarks: String = "",
    val createdBy: String = "Administrator",
    val publishedAt: String = "",
    val publicUrl: String = "",
    val notificationSentAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "report_recipients")
data class RecipientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String = "",
    val name: String = "",
    val isActive: Boolean = true,
    val addedAt: String = ""
)

@Entity(tableName = "report_notifications")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportId: Long = 0,
    val reportDate: String = "",
    val recipientEmail: String = "",
    val sentAt: String = "",
    val status: String = "Sent", // "Sent", "Failed"
    val errorMessage: String = ""
)

@Entity(tableName = "cloud_config")
data class CloudConfigEntity(
    @PrimaryKey val id: Int = 1,
    val firebaseProjectId: String = "acceptance-8781f",
    val firebaseDatabaseUrl: String = "https://acceptance-8781f-default-rtdb.firebaseio.com",
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val resendApiKey: String = "",
    val reportFromEmail: String = "reports@ethiodjiboutirailway.com",
    val appPublicUrl: String = "https://acceptance-8781f-default-rtdb.firebaseio.com",
    val scheduledTime: String = "18:00",
    val scheduledTimezone: String = "Africa/Addis_Ababa",
    val scheduledEnabled: Boolean = false
)
