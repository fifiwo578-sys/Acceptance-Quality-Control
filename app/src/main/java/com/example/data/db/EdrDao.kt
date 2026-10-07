package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EdrDao {

    // === INCIDENTS ===
    @Query("SELECT * FROM incidents WHERE isTrash = 0 ORDER BY id DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE date = :date AND isTrash = 0 AND isCancelled = 0")
    suspend fun getIncidentsByDate(date: String): List<IncidentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncidents(incidents: List<IncidentEntity>)

    @Update
    suspend fun updateIncident(incident: IncidentEntity)

    @Query("UPDATE incidents SET isTrash = 1, trashReason = :reason, deletedAt = :deletedAt WHERE id = :id")
    suspend fun trashIncident(id: Long, reason: String, deletedAt: String)

    @Query("UPDATE incidents SET isCancelled = 1, cancelReason = :reason, cancelledAt = :cancelledAt WHERE id = :id")
    suspend fun cancelIncident(id: Long, reason: String, cancelledAt: String)

    @Query("UPDATE incidents SET isCancelled = 0, cancelReason = '', cancelledAt = '' WHERE id = :id")
    suspend fun reinstateIncident(id: Long)

    @Query("UPDATE incidents SET isTrash = 0, trashReason = '', deletedAt = '' WHERE id = :id")
    suspend fun restoreIncident(id: Long)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun permanentDeleteIncident(id: Long)

    @Query("DELETE FROM incidents")
    suspend fun clearAllIncidents()

    // === PROBLEMS ===
    @Query("SELECT * FROM problems WHERE isTrash = 0 ORDER BY id DESC")
    fun getAllProblems(): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE date = :date AND isTrash = 0 AND isCancelled = 0")
    suspend fun getProblemsByDate(date: String): List<ProblemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(problem: ProblemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblems(problems: List<ProblemEntity>)

    @Update
    suspend fun updateProblem(problem: ProblemEntity)

    @Query("UPDATE problems SET isTrash = 1, trashReason = :reason, deletedAt = :deletedAt WHERE id = :id")
    suspend fun trashProblem(id: Long, reason: String, deletedAt: String)

    @Query("UPDATE problems SET isCancelled = 1, cancelReason = :reason, cancelledAt = :cancelledAt WHERE id = :id")
    suspend fun cancelProblem(id: Long, reason: String, cancelledAt: String)

    @Query("UPDATE problems SET isCancelled = 0, cancelReason = '', cancelledAt = '' WHERE id = :id")
    suspend fun reinstateProblem(id: Long)

    @Query("UPDATE problems SET isTrash = 0, trashReason = '', deletedAt = '' WHERE id = :id")
    suspend fun restoreProblem(id: Long)

    @Query("DELETE FROM problems WHERE id = :id")
    suspend fun permanentDeleteProblem(id: Long)

    @Query("DELETE FROM problems")
    suspend fun clearAllProblems()

    // === QUALITY CONTROL (Wagon, Locomotive, Coach) ===
    @Query("SELECT * FROM qc_records WHERE module = :module AND isTrash = 0 ORDER BY id DESC")
    fun getQCRecords(module: String): Flow<List<QCEntity>>

    @Query("SELECT * FROM qc_records WHERE isTrash = 0 ORDER BY id DESC")
    fun getAllQCRecords(): Flow<List<QCEntity>>

    @Query("SELECT * FROM qc_records WHERE module = :module AND startDate = :date AND isTrash = 0 AND isCancelled = 0")
    suspend fun getQCByDate(module: String, date: String): List<QCEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQC(qc: QCEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQCList(qcList: List<QCEntity>)

    @Update
    suspend fun updateQC(qc: QCEntity)

    @Query("UPDATE qc_records SET isTrash = 1, trashReason = :reason, deletedAt = :deletedAt WHERE id = :id")
    suspend fun trashQC(id: Long, reason: String, deletedAt: String)

    @Query("UPDATE qc_records SET isCancelled = 1, status = 'Cancelled', cancelReason = :reason, cancelledAt = :cancelledAt WHERE id = :id")
    suspend fun cancelQC(id: Long, reason: String, cancelledAt: String)

    @Query("UPDATE qc_records SET isCancelled = 0, status = 'Pending', cancelReason = '', cancelledAt = '' WHERE id = :id")
    suspend fun reinstateQC(id: Long)

    @Query("UPDATE qc_records SET isTrash = 0, trashReason = '', deletedAt = '' WHERE id = :id")
    suspend fun restoreQC(id: Long)

    @Query("DELETE FROM qc_records WHERE id = :id")
    suspend fun permanentDeleteQC(id: Long)

    @Query("DELETE FROM qc_records WHERE module = :module")
    suspend fun clearQCByModule(module: String)

    @Query("DELETE FROM qc_records")
    suspend fun clearAllQC()

    // === TASKS (AQMS, Wagon, Locomotive, Coach, Equipment) ===
    @Query("SELECT * FROM tasks WHERE module = :module AND isTrash = 0 ORDER BY id DESC")
    fun getTasks(module: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isTrash = 0 ORDER BY id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE module = :module AND date = :date AND isTrash = 0 AND isCancelled = 0")
    suspend fun getTasksByDate(module: String, date: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isTrash = 1, trashReason = :reason, deletedAt = :deletedAt WHERE id = :id")
    suspend fun trashTask(id: Long, reason: String, deletedAt: String)

    @Query("UPDATE tasks SET isCancelled = 1, cancelReason = :reason, cancelledAt = :cancelledAt WHERE id = :id")
    suspend fun cancelTask(id: Long, reason: String, cancelledAt: String)

    @Query("UPDATE tasks SET isCancelled = 0, cancelReason = '', cancelledAt = '' WHERE id = :id")
    suspend fun reinstateTask(id: Long)

    @Query("UPDATE tasks SET isTrash = 0, trashReason = '', deletedAt = '' WHERE id = :id")
    suspend fun restoreTask(id: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun permanentDeleteTask(id: Long)

    @Query("DELETE FROM tasks WHERE module = :module")
    suspend fun clearTasksByModule(module: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    // === DAILY REPORTS ===
    @Query("SELECT * FROM reports ORDER BY reportDate DESC, id DESC")
    fun getAllReports(): Flow<List<DailyReportEntity>>

    @Query("SELECT * FROM reports WHERE reportDate = :date LIMIT 1")
    suspend fun getReportByDate(date: String): DailyReportEntity?

    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: Long): DailyReportEntity?

    @Query("SELECT * FROM reports")
    suspend fun getReportList(): List<DailyReportEntity>

    @Query("UPDATE reports SET publicUrl = :newUrl WHERE id = :id")
    suspend fun updateReportUrl(id: Long, newUrl: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: DailyReportEntity): Long

    @Update
    suspend fun updateReport(report: DailyReportEntity)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteReport(id: Long)

    @Query("DELETE FROM reports")
    suspend fun clearAllReports()

    // === RECIPIENTS ===
    @Query("SELECT * FROM report_recipients ORDER BY id DESC")
    fun getAllRecipients(): Flow<List<RecipientEntity>>

    @Query("SELECT * FROM report_recipients WHERE isActive = 1")
    suspend fun getActiveRecipients(): List<RecipientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipient(recipient: RecipientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipients(recipients: List<RecipientEntity>)

    @Update
    suspend fun updateRecipient(recipient: RecipientEntity)

    @Query("DELETE FROM report_recipients WHERE id = :id")
    suspend fun deleteRecipient(id: Long)

    // === NOTIFICATION LOGS ===
    @Query("SELECT * FROM report_notifications ORDER BY sentAt DESC, id DESC")
    fun getAllNotificationLogs(): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationLog(log: NotificationLogEntity): Long

    @Query("DELETE FROM report_notifications")
    suspend fun clearAllNotificationLogs()

    // === TRASH QUERIES ===
    @Query("SELECT * FROM incidents WHERE isTrash = 1")
    fun getTrashIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM problems WHERE isTrash = 1")
    fun getTrashProblems(): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM qc_records WHERE isTrash = 1")
    fun getTrashQC(): Flow<List<QCEntity>>

    @Query("SELECT * FROM tasks WHERE isTrash = 1")
    fun getTrashTasks(): Flow<List<TaskEntity>>

    @Query("DELETE FROM incidents WHERE isTrash = 1")
    suspend fun emptyTrashIncidents()

    @Query("DELETE FROM problems WHERE isTrash = 1")
    suspend fun emptyTrashProblems()

    @Query("DELETE FROM qc_records WHERE isTrash = 1")
    suspend fun emptyTrashQC()

    @Query("DELETE FROM tasks WHERE isTrash = 1")
    suspend fun emptyTrashTasks()

    // === CANCELLED QUERIES ===
    @Query("SELECT * FROM incidents WHERE isCancelled = 1 AND isTrash = 0")
    fun getCancelledIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM problems WHERE isCancelled = 1 AND isTrash = 0")
    fun getCancelledProblems(): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM qc_records WHERE isCancelled = 1 AND isTrash = 0")
    fun getCancelledQC(): Flow<List<QCEntity>>

    @Query("SELECT * FROM tasks WHERE isCancelled = 1 AND isTrash = 0")
    fun getCancelledTasks(): Flow<List<TaskEntity>>

    // === CLOUD CONFIG ===
    @Query("SELECT * FROM cloud_config WHERE id = 1 LIMIT 1")
    suspend fun getCloudConfig(): CloudConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCloudConfig(config: CloudConfigEntity)
}
