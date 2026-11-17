package com.example.btvn_tuan4.notification

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

// Luu FCM token cua thiet bi tai users/{uid}/devices/{token} de backend biet gui push den dau (Phase B)
class DeviceTokenRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun doc(userId: String, token: String) =
        db.collection("users").document(userId).collection("devices").document(token)

    suspend fun saveToken(userId: String, token: String) {
        if (userId.isBlank() || userId == DEFAULT_USER_ID) return
        doc(userId, token).set(
            mapOf("token" to token, "platform" to "android", "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    // Goi sau khi dang nhap
    suspend fun registerCurrentDevice(userId: String) {
        saveToken(userId, FirebaseMessaging.getInstance().token.await())
    }

    // Goi truoc khi dang xuat (luc con quyen ghi), de user cu khong nhan push tren may nay nua
    suspend fun unregisterCurrentDevice(userId: String) {
        if (userId.isBlank() || userId == DEFAULT_USER_ID) return
        doc(userId, FirebaseMessaging.getInstance().token.await()).delete().await()
    }
}
