package com.example.btvn_tuan4.notification

// Loai su kien thong bao (Chuong 10: Event). Hien tai chi co nhac viec
enum class NotificationType { TASK_REMINDER }

// User mac dinh khi chua dang nhap, khong bao gio gui thong bao cho user nay
const val DEFAULT_USER_ID = "default_user"

// Su kien thong bao: moi lan nhac cua mot task tai mot thoi diem la mot event duy nhat
data class NotificationEvent(
    val eventId: String,
    val userId: String,
    val taskId: String,
    val type: NotificationType,
    val scheduledAt: Long
) {
    companion object {
        // eventId on dinh theo (task, thoi diem) de dedupe khi worker chay lai
        fun reminder(userId: String, taskId: String, scheduledAt: Long) = NotificationEvent(
            eventId = "${taskId}_reminder_$scheduledAt",
            userId = userId,
            taskId = taskId,
            type = NotificationType.TASK_REMINDER,
            scheduledAt = scheduledAt
        )
    }
}
