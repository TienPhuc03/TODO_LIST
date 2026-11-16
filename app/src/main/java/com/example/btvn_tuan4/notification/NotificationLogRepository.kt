package com.example.btvn_tuan4.notification

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

// Trang thai cua mot event trong notificationLog (Chuong 10: Tracking + Monitoring)
const val STATUS_SCHEDULED = "scheduled"
const val STATUS_RETRYING = "retrying"
const val STATUS_SENT = "sent"
const val STATUS_OPENED = "opened"
const val STATUS_FAILED = "failed"
const val STATUS_SKIPPED_PAST = "skipped_past"
const val STATUS_SKIPPED_OPTOUT = "skipped_optout"
const val STATUS_SKIPPED_PERMISSION = "skipped_permission"
const val STATUS_SKIPPED_RATE_LIMIT = "skipped_rate_limit"

// Event da gui (hoac da mo) thi khong gui lai, khong ghi de
fun isDelivered(status: String?) = status == STATUS_SENT || status == STATUS_OPENED

// Doc tu server; offline thi doc ban luu tren may (cache). Ca hai deu khong co thi nem loi
suspend fun DocumentReference.getOrCached(): DocumentSnapshot = try {
    get().await()
} catch (e: Exception) {
    get(Source.CACHE).await()
}

// Ghi khong cho server xac nhan: offline thi Firestore xep hang va tu dong bo khi co mang.
// (await() cho xac nhan se treo mai khi offline)
fun DocumentReference.setInBackground(data: Map<String, Any>) {
    set(data, SetOptions.merge()).addOnFailureListener {
        Log.w("NotificationLog", "Ghi ${path} thất bại: ${it.message}")
    }
}

interface INotificationLogRepository {
    suspend fun getStatus(userId: String, eventId: String): String?
    suspend fun log(userId: String, event: NotificationEvent, status: String, attempts: Int)
    suspend fun markOpened(userId: String, eventId: String)
}

// Moi event la mot document users/{uid}/notificationLog/{eventId} (dung luon cho dedupe)
class FirestoreNotificationLogRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : INotificationLogRepository {

    private fun doc(userId: String, eventId: String) =
        db.collection("users").document(userId).collection("notificationLog").document(eventId)

    override suspend fun getStatus(userId: String, eventId: String): String? =
        doc(userId, eventId).getOrCached().getString("status")

    override suspend fun log(userId: String, event: NotificationEvent, status: String, attempts: Int) {
        val data = mutableMapOf<String, Any>(
            "eventId" to event.eventId,
            "taskId" to event.taskId,
            "type" to event.type.name,
            "scheduledAt" to event.scheduledAt,
            "status" to status,
            "attempts" to attempts,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        // createdAt ghi luc len lich (hoac luc bo qua vi qua khu), cac lan sau chi cap nhat
        if (status == STATUS_SCHEDULED || status == STATUS_SKIPPED_PAST) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }
        doc(userId, event.eventId).setInBackground(data)
    }

    override suspend fun markOpened(userId: String, eventId: String) {
        doc(userId, eventId).setInBackground(
            mapOf("status" to STATUS_OPENED, "openedAt" to FieldValue.serverTimestamp())
        )
    }
}
