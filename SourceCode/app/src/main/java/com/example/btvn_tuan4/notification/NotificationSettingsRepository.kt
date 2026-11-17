package com.example.btvn_tuan4.notification

import com.google.firebase.firestore.FirebaseFirestore

// Cai dat opt-in cua nguoi dung (Chuong 10: User preferences)
interface INotificationSettingsRepository {
    suspend fun isTaskRemindersEnabled(userId: String): Boolean
    suspend fun setTaskRemindersEnabled(userId: String, enabled: Boolean)
}

// Luu tai users/{uid}/notificationSettings/default, field taskReminders (mac dinh true)
class FirestoreNotificationSettingsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : INotificationSettingsRepository {

    private fun doc(userId: String) =
        db.collection("users").document(userId).collection("notificationSettings").document("default")

    override suspend fun isTaskRemindersEnabled(userId: String): Boolean =
        doc(userId).getOrCached().getBoolean("taskReminders") ?: true

    // Offline van doi duoc: cache tren may cap nhat ngay nen worker doc lai thay gia tri moi
    override suspend fun setTaskRemindersEnabled(userId: String, enabled: Boolean) {
        doc(userId).setInBackground(mapOf("taskReminders" to enabled))
    }
}
