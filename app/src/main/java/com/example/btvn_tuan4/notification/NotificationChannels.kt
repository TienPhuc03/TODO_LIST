package com.example.btvn_tuan4.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val TASK_REMINDERS = "task_reminders"

    // Tao kenh thong bao khi khoi dong app (Android 8+). Goi nhieu lan khong sao
    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            TASK_REMINDERS,
            "Nhắc việc",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Nhắc trước khi công việc đến hạn" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
