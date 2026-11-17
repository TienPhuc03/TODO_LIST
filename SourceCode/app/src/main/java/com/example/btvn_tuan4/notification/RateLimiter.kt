package com.example.btvn_tuan4.notification

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Toi da so thong bao moi gio cho mot user (Chuong 10: Rate limiting)
const val RATE_LIMIT_PER_HOUR = 20

fun isUnderLimit(countThisHour: Long) = countThisHour < RATE_LIMIT_PER_HOUR

interface IRateLimiter {
    // true neu con quota va da tru 1 luot; false neu vuot nguong
    suspend fun tryAcquire(userId: String, nowMs: Long): Boolean
}

// Bo dem users/{uid}/rateLimits/{yyyyMMddHH}, tang bang transaction de khong dem sai khi chay song song.
// Transaction can mang; offline thi het thoi gian va nem loi, dispatcher se cho phep gui (fail-open)
class FirestoreRateLimiter(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IRateLimiter {

    override suspend fun tryAcquire(userId: String, nowMs: Long): Boolean {
        val hourKey = SimpleDateFormat("yyyyMMddHH", Locale.US)
            .apply { timeZone = VN_TIME_ZONE }
            .format(Date(nowMs))
        val ref = db.collection("users").document(userId).collection("rateLimits").document(hourKey)
        // withTimeoutOrNull + nem loi thuong: withTimeout nem TimeoutCancellationException (mot loai
        // CancellationException) nen se lot qua lop fail-open cua dispatcher va lam worker retry
        return withTimeoutOrNull(TRANSACTION_TIMEOUT_MS) { db.runTransaction { tx ->
            val count = tx.get(ref).getLong("count") ?: 0L
            if (isUnderLimit(count)) {
                tx.set(ref, mapOf("count" to count + 1), SetOptions.merge())
                true
            } else {
                false
            }
        }.await() } ?: throw IllegalStateException("Rate limit transaction quá ${TRANSACTION_TIMEOUT_MS}ms (offline?)")
    }

    private companion object {
        const val TRANSACTION_TIMEOUT_MS = 5_000L
    }
}
