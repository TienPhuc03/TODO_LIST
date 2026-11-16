package com.example.btvn_tuan4.notification

data class RenderedNotification(val title: String, val body: String)

// Mau thong bao voi placeholder dang {KEY} (Chuong 10: Template)
data class NotificationTemplate(val title: String, val body: String) {

    // Thay placeholder bang gia tri; placeholder thieu gia tri thanh chuoi rong, khong de sot {...}
    fun render(values: Map<String, String>) = RenderedNotification(
        title = fill(title, values),
        body = fill(body, values)
    )

    private fun fill(text: String, values: Map<String, String>) =
        PLACEHOLDER.replace(text) { values[it.groupValues[1]].orEmpty() }

    companion object {
        private val PLACEHOLDER = Regex("\\{([A-Z_]+)\\}")

        val TASK_REMINDER = NotificationTemplate(
            title = "Sắp đến hạn: {TITLE}",
            body = "Công việc \"{TITLE}\" đến hạn lúc {TIME} (ưu tiên {PRIORITY})."
        )
    }
}
