package com.example.btvn_tuan4.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.btvn_tuan4.MainActivity

// Hien thi thong bao tren thiet bi, dung chung cho nhac viec local (worker) va push FCM
object NotificationDisplay {

    // Extra trong Intent khi bam thong bao (deep link + tracking "opened").
    // Cloud Function gui data FCM voi dung cac key nay de bam thong bao nen cung mo dung task
    const val EXTRA_TASK_ID = "notification_task_id"
    const val EXTRA_EVENT_ID = "notification_event_id"
    const val EXTRA_USER_ID = "notification_user_id"

    fun canPost(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // Goi canPost() truoc; khong co quyen thi bo qua thay vi crash
    @SuppressLint("MissingPermission")
    fun show(context: Context, event: NotificationEvent, content: RenderedNotification) {
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, event.taskId)
            putExtra(EXTRA_EVENT_ID, event.eventId)
            putExtra(EXTRA_USER_ID, event.userId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            event.eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.TASK_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(event.eventId.hashCode(), notification)
    }
}
