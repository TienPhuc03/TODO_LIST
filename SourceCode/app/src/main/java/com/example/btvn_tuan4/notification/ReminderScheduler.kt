package com.example.btvn_tuan4.notification

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.btvn_tuan4.model.TodoTask
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

// Nhac truoc han chot bao nhieu phut (doi tam thanh 1 khi kiem thu thu cong)
const val REMINDER_LEAD_MINUTES = 15

val VN_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")

// Ham thuan: tinh thoi diem nhac (epoch ms) tu date "yyyy-MM-dd" + time "HH:mm" theo gio VN.
fun computeReminderAt(date: String, time: String, leadMinutes: Int = REMINDER_LEAD_MINUTES): Long? {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = VN_TIME_ZONE
        isLenient = false
    }
    return try {
        format.parse("${date.trim()} ${time.trim()}")!!.time - TimeUnit.MINUTES.toMillis(leadMinutes.toLong())
    } catch (_: ParseException) {
        null
    }
}

fun isPast(reminderAt: Long, nowMs: Long = System.currentTimeMillis()) = reminderAt <= nowMs

// Hien thi gio nhac tren man hinh Chi tiet, vd "14:45, 01/12"
fun formatReminderTime(reminderAt: Long): String =
    SimpleDateFormat("HH:mm, dd/MM", Locale.US).apply { timeZone = VN_TIME_ZONE }.format(Date(reminderAt))

// Len lich / huy nhac viec bang WorkManager (Chuong 10: Scheduler).
// Moi task co mot unique work "reminder_{taskId}" nen sua task se thay the lich cu
class ReminderScheduler(
    context: Context,
    private val logRepository: INotificationLogRepository = FirestoreNotificationLogRepository()
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    suspend fun schedule(userId: String, task: TodoTask) {
        if (userId.isBlank() || userId == DEFAULT_USER_ID || !task.remindMe || task.isCompleted) {
            cancel(task.id)
            return
        }
        val reminderAt = computeReminderAt(task.date, task.time)
        if (reminderAt == null) {
            Log.w(TAG, "Bỏ qua nhắc việc task ${task.id}: không đọc được hạn '${task.date} ${task.time}'")
            cancel(task.id)
            return
        }
        val event = NotificationEvent.reminder(userId, task.id, reminderAt)

        // Khong de loi Firestore (offline...) lam hong viec luu task
        try {
            if (isDelivered(logRepository.getStatus(userId, event.eventId))) return
            if (isPast(reminderAt)) {
                cancel(task.id)
                logRepository.log(userId, event, STATUS_SKIPPED_PAST, 0)
                return
            }
            logRepository.log(userId, event, STATUS_SCHEDULED, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Không ghi được notificationLog: ${e.message}")
            if (isPast(reminderAt)) return
        }

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(reminderAt - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_USER_ID to userId,
                    ReminderWorker.KEY_TASK_ID to task.id,
                    ReminderWorker.KEY_SCHEDULED_AT to reminderAt,
                    ReminderWorker.KEY_TITLE to task.title,
                    ReminderWorker.KEY_TIME to task.time,
                    ReminderWorker.KEY_PRIORITY to task.displayPriorityShort
                )
            )
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(workName(task.id), ExistingWorkPolicy.REPLACE, request)
        Log.i(TAG, "Đã lên lịch nhắc task ${task.id} lúc ${formatReminderTime(reminderAt)}")
    }

    fun cancel(taskId: String) {
        workManager.cancelUniqueWork(workName(taskId))
    }

    companion object {
        const val TAG = "ReminderScheduler"
        fun workName(taskId: String) = "reminder_$taskId"

        // Huy moi lich nhac cua thiet bi (goi khi dang xuat)
        fun cancelAll(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelAllWorkByTag(TAG)
        }
    }
}
