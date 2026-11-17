package com.example.btvn_tuan4.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

enum class DispatchOutcome {
    SENT,
    SKIPPED_INVALID_USER,
    SKIPPED_OPTOUT,
    SKIPPED_DUPLICATE,
    SKIPPED_PERMISSION,
    SKIPPED_RATE_LIMIT
}

// Logic quyet dinh gui hay khong, tach khoi Android de unit test duoc.
// Thu tu kiem tra: user hop le -> opt-in -> dedupe -> quyen thong bao -> rate limit -> gui.
// Nhac viec local phai hien ca khi offline: loi Firestore o tung buoc khong chan thong bao
// ma dung gia tri an toan (opt-in mac dinh bat, chua tung gui, cho phep gui) - "fail-open"
class ReminderDispatcher(
    private val settings: INotificationSettingsRepository,
    private val logRepository: INotificationLogRepository,
    private val rateLimiter: IRateLimiter,
    private val canPost: () -> Boolean,
    private val post: (NotificationEvent, RenderedNotification) -> Unit,
    private val template: NotificationTemplate = NotificationTemplate.TASK_REMINDER
) {
    suspend fun dispatch(event: NotificationEvent, values: Map<String, String>, attempt: Int = 1): DispatchOutcome {
        val userId = event.userId
        if (userId.isBlank() || userId == DEFAULT_USER_ID) return DispatchOutcome.SKIPPED_INVALID_USER

        val log: suspend (String) -> Unit = { status -> orDefault(Unit) { logRepository.log(userId, event, status, attempt) } }

        if (!orDefault(true) { settings.isTaskRemindersEnabled(userId) }) {
            log(STATUS_SKIPPED_OPTOUT)
            return DispatchOutcome.SKIPPED_OPTOUT
        }
        if (isDelivered(orDefault(null) { logRepository.getStatus(userId, event.eventId) })) {
            return DispatchOutcome.SKIPPED_DUPLICATE
        }

        // Kiem tra quyen truoc rate limit de khong ton quota khi khong hien duoc thong bao
        if (!canPost()) {
            log(STATUS_SKIPPED_PERMISSION)
            return DispatchOutcome.SKIPPED_PERMISSION
        }
        if (!orDefault(true) { rateLimiter.tryAcquire(userId, System.currentTimeMillis()) }) {
            log(STATUS_SKIPPED_RATE_LIMIT)
            return DispatchOutcome.SKIPPED_RATE_LIMIT
        }

        // Loi o day (hien thong bao) moi nem ra ngoai de worker retry
        post(event, template.render(values))
        log(STATUS_SENT)
        return DispatchOutcome.SENT
    }

    // Chay block; loi (vd offline) thi tra fallback. Van de CancellationException di qua de huy worker dung cach
    // TimeoutCancellationException (het thoi gian cho Firestore) la loi thuong, khong phai worker bi huy
    private suspend fun <T> orDefault(fallback: T, block: suspend () -> T): T = try {
        block()
    } catch (e: TimeoutCancellationException) {
        Log.w(ReminderWorker.TAG, "Firestore quá thời gian, dùng giá trị mặc định: ${e.message}")
        fallback
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(ReminderWorker.TAG, "Bỏ qua lỗi Firestore, dùng giá trị mặc định: ${e.message}")
        fallback
    }
}

// Worker chay luc (han chot - 15 phut). Loi khi hien thong bao -> Result.retry() voi backoff, toi da MAX_ATTEMPTS lan
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val event = NotificationEvent.reminder(
            userId = inputData.getString(KEY_USER_ID).orEmpty(),
            taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure(),
            scheduledAt = inputData.getLong(KEY_SCHEDULED_AT, 0L)
        )
        val values = mapOf(
            "TITLE" to inputData.getString(KEY_TITLE).orEmpty(),
            "TIME" to inputData.getString(KEY_TIME).orEmpty(),
            "PRIORITY" to inputData.getString(KEY_PRIORITY).orEmpty()
        )
        val attempt = runAttemptCount + 1
        val logRepository = FirestoreNotificationLogRepository()
        val dispatcher = ReminderDispatcher(
            settings = FirestoreNotificationSettingsRepository(),
            logRepository = logRepository,
            rateLimiter = FirestoreRateLimiter(),
            canPost = { NotificationDisplay.canPost(applicationContext) },
            post = { e, content -> NotificationDisplay.show(applicationContext, e, content) }
        )

        return try {
            val outcome = dispatcher.dispatch(event, values, attempt)
            Log.i(TAG, "Event ${event.eventId}: $outcome (lần thử $attempt)")
            Result.success()
        } catch (e: Exception) {
            val status = if (attempt >= MAX_ATTEMPTS) STATUS_FAILED else STATUS_RETRYING
            Log.w(TAG, "Event ${event.eventId} lỗi lần $attempt: ${e.message} -> $status")
            runCatching { logRepository.log(event.userId, event, status, attempt) }
            if (status == STATUS_FAILED) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val TAG = "ReminderWorker"
        const val MAX_ATTEMPTS = 3

        const val KEY_USER_ID = "userId"
        const val KEY_TASK_ID = "taskId"
        const val KEY_SCHEDULED_AT = "scheduledAt"
        const val KEY_TITLE = "title"
        const val KEY_TIME = "time"
        const val KEY_PRIORITY = "priority"
    }
}
