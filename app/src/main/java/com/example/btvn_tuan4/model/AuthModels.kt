package com.example.btvn_tuan4.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.firestore.PropertyName

// Model thong tin nguoi dung
data class AuthUser(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isGoogleLinked: Boolean = true,
    val isAnonymous: Boolean = false
) {
    // Lay 2 ky tu viet tat cua ten
    val initials: String
        get() {
            val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
            return when {
                parts.size >= 2 -> "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
                parts.size == 1 -> parts.first().take(1).uppercase()
                else -> "K"
            }
        }
}

// Trang thai giao dien xac thuc
sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val user: AuthUser, val message: String) : AuthUiState
    data class Error(val errorMessage: String) : AuthUiState
}

// Model viec nho (Subtask) trong man hinh Chi tiet cong viec
data class SubTask(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("done") @set:PropertyName("done")
    var done: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "done" to done
        )
    }
}

// Model cong viec Todo dong bo ca REST API (/mock/teach/tasks) va Cloud Firestore
data class TodoTask(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("note") @set:PropertyName("note")
    var note: String = "",
    @get:PropertyName("categoryName") @set:PropertyName("categoryName")
    var categoryName: String = "Học tập",
    @get:PropertyName("date") @set:PropertyName("date")
    var date: String = "2026-10-20",
    @get:PropertyName("time") @set:PropertyName("time")
    var time: String = "14:00",
    @get:PropertyName("remindMe") @set:PropertyName("remindMe")
    var remindMe: Boolean = true,
    @get:PropertyName("priority") @set:PropertyName("priority")
    var priority: String = "Vừa",
    @get:PropertyName("isCompleted") @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    @get:PropertyName("subtasks") @set:PropertyName("subtasks")
    var subtasks: List<SubTask> = emptyList(),
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis()
) {
    // Ma category gui len API ("study" hoac "life")
    val apiCategory: String
        get() = if (categoryName == "Cuộc sống" || categoryName.equals("life", ignoreCase = true)) "life" else "study"

    // Ten hien thi danh muc tieng Viet
    val displayCategory: String
        get() = if (apiCategory == "life") "Cuộc sống" else "Học tập"

    // Ma priority gui len API ("low", "medium", "high")
    val apiPriority: String
        get() = when (priority.lowercase()) {
            "cao", "high", "gấp" -> "high"
            "thấp", "low" -> "low"
            else -> "medium"
        }

    // Nhan muc uu tien ngan gon ("Thấp", "Vừa", "Cao")
    val displayPriorityShort: String
        get() = when (apiPriority) {
            "high" -> "Cao"
            "low" -> "Thấp"
            else -> "Vừa"
        }

    // Nhan muc uu tien tren man hinh Chi tiet ("Ưu tiên cao", "Ưu tiên vừa", "Ưu tiên thấp")
    val displayPriorityBadge: String
        get() = when (apiPriority) {
            "high" -> "Ưu tiên cao"
            "low" -> "Ưu tiên thấp"
            else -> "Ưu tiên vừa"
        }

    // Kiem tra co phai viec uu tien cao (hien thi nhan "Gấp" o man hinh Danh sach)
    val isUrgent: Boolean
        get() = apiPriority == "high"

    // Kiem tra cong viec co thuoc danh sach "Viec hom nay" tren Trang chu
    val isToday: Boolean
        get() = date == "2026-10-20" ||
                date.contains("Hôm nay", ignoreCase = true) ||
                date.contains("20/10")

    // Hien thi han chot ket hop ngay va gio theo dung giao dien mau
    val displayDeadline: String
        get() {
            val dayLabel = when {
                date == "2026-10-20" || date.contains("Hôm nay", ignoreCase = true) -> "Hôm nay"
                date == "2026-10-21" || date.contains("Ngày mai", ignoreCase = true) -> "Ngày mai"
                date == "2026-10-24" || date == "2026-10-25" || date.contains("Tuần này", ignoreCase = true) -> "Tuần này"
                else -> date
            }
            return if (time.isNotBlank()) "$dayLabel, $time" else dayLabel
        }

    // Chuyen doi tuong sang Map cho Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "note" to note,
            "categoryName" to displayCategory,
            "date" to date,
            "time" to time,
            "remindMe" to remindMe,
            "priority" to displayPriorityShort,
            "isCompleted" to isCompleted,
            "subtasks" to subtasks.map { it.toMap() },
            "createdAt" to createdAt
        )
    }

    companion object {
        // Chuyen doi ma category tu API sang tieng Viet
        fun categoryFromApi(raw: String): String {
            return if (raw.equals("life", ignoreCase = true) || raw == "Cuộc sống") "Cuộc sống" else "Học tập"
        }

        // Chuyen doi ma priority tu API sang tieng Viet
        fun priorityFromApi(raw: String): String {
            return when (raw.lowercase()) {
                "high", "cao" -> "Cao"
                "low", "thấp" -> "Thấp"
                else -> "Vừa"
            }
        }
    }
}

// Model the danh muc cong viec
data class CategoryCardData(
    val name: String,
    val remainingCount: Int,
    val backgroundColor: Color,
    val accentColor: Color,
    val icon: ImageVector
)
