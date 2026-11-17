package com.example.btvn_tuan4.notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Nhan push FCM tu Cloud Function (Phase B)
class PushMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Token doi (cai lai app, xoa du lieu...) -> cap nhat lai Firestore neu dang dang nhap
    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch {
            runCatching { DeviceTokenRepository().saveToken(uid, token) }
                .onFailure { Log.w(TAG, "Không lưu được FCM token: ${it.message}") }
        }
    }

    // App dang mo (foreground) thi FCM khong tu hien thong bao, ta hien bang NotificationDisplay.
    // App o nen thi he thong tu hien phan "notification", bam vao se mo MainActivity kem data lam extra
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val event = NotificationEvent(
            eventId = data[NotificationDisplay.EXTRA_EVENT_ID] ?: message.messageId.orEmpty(),
            userId = data[NotificationDisplay.EXTRA_USER_ID].orEmpty(),
            taskId = data[NotificationDisplay.EXTRA_TASK_ID].orEmpty(),
            type = NotificationType.TASK_REMINDER,
            scheduledAt = message.sentTime
        )
        val content = RenderedNotification(
            title = message.notification?.title ?: data["title"].orEmpty(),
            body = message.notification?.body ?: data["body"].orEmpty()
        )
        Log.i(TAG, "Nhận push ${event.eventId} cho task ${event.taskId}")
        NotificationDisplay.show(applicationContext, event, content)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val TAG = "PushMessagingService"
    }
}
