package com.example.btvn_tuan4.notification

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTimeTest {

    @Test
    fun computesDeadlineMinusLeadInVietnamTime() {
        // 2026-12-01 14:00 gio VN = 07:00 UTC; tru 15 phut = 06:45 UTC
        val expected = 1796107500000L // 2026-12-01T06:45:00Z
        assertEquals(expected, computeReminderAt("2026-12-01", "14:00"))
    }

    @Test
    fun returnsNullWhenDateIsNotParseable() {
        assertNull(computeReminderAt("Hôm nay", "14:00"))
        assertNull(computeReminderAt("2026-13-45", "14:00"))
        assertNull(computeReminderAt("2026-12-01", ""))
    }

    @Test
    fun pastReminderIsDetected() {
        val at = computeReminderAt("2026-10-20", "14:00")!!
        assertTrue(isPast(at, nowMs = at + 1))
        assertFalse(isPast(at, nowMs = at - 60_000))
    }
}

class NotificationTemplateTest {

    @Test
    fun rendersAllPlaceholders() {
        val r = NotificationTemplate.TASK_REMINDER.render(
            mapOf("TITLE" to "Học Kotlin", "TIME" to "14:00", "PRIORITY" to "Cao")
        )
        assertTrue(r.title.contains("Học Kotlin"))
        assertTrue(r.body.contains("14:00") && r.body.contains("Cao"))
        assertFalse(r.title.contains("{") || r.body.contains("{"))
    }

    @Test
    fun missingValuesLeaveNoPlaceholder() {
        val r = NotificationTemplate.TASK_REMINDER.render(emptyMap())
        assertFalse(r.title.contains("{") || r.body.contains("{"))
    }
}

class ReminderDispatcherTest {

    private class FakeSettings(var enabled: Boolean = true) : INotificationSettingsRepository {
        override suspend fun isTaskRemindersEnabled(userId: String) = enabled
        override suspend fun setTaskRemindersEnabled(userId: String, enabled: Boolean) { this.enabled = enabled }
    }

    private class FakeLog : INotificationLogRepository {
        val statuses = mutableMapOf<String, String>()
        override suspend fun getStatus(userId: String, eventId: String) = statuses[eventId]
        override suspend fun log(userId: String, event: NotificationEvent, status: String, attempts: Int) {
            statuses[event.eventId] = status
        }
        override suspend fun markOpened(userId: String, eventId: String) { statuses[eventId] = STATUS_OPENED }
    }

    private class FakeRateLimiter : IRateLimiter {
        var count = 0L
        override suspend fun tryAcquire(userId: String, nowMs: Long): Boolean =
            isUnderLimit(count).also { if (it) count++ }
    }

    private val values = mapOf("TITLE" to "T", "TIME" to "14:00", "PRIORITY" to "Vừa")
    private fun event(i: Int = 1) = NotificationEvent.reminder("u1", "task$i", 1_000L * i)

    private fun dispatcher(
        settings: FakeSettings = FakeSettings(),
        log: FakeLog = FakeLog(),
        limiter: FakeRateLimiter = FakeRateLimiter(),
        posted: MutableList<String> = mutableListOf()
    ) = ReminderDispatcher(settings, log, limiter, canPost = { true }, post = { e, _ -> posted += e.eventId })

    @Test
    fun sendsAndLogsSent() = runBlocking {
        val log = FakeLog(); val posted = mutableListOf<String>()
        assertEquals(DispatchOutcome.SENT, dispatcher(log = log, posted = posted).dispatch(event(), values))
        assertEquals(1, posted.size)
        assertEquals(STATUS_SENT, log.statuses[event().eventId])
    }

    @Test
    fun secondDispatchOfSameEventIsDeduped() = runBlocking {
        val posted = mutableListOf<String>()
        val d = dispatcher(posted = posted)
        d.dispatch(event(), values)
        assertEquals(DispatchOutcome.SKIPPED_DUPLICATE, d.dispatch(event(), values))
        assertEquals(1, posted.size)
    }

    @Test
    fun rateLimitBlocksAfterThreshold() = runBlocking {
        val d = dispatcher()
        repeat(RATE_LIMIT_PER_HOUR) { assertEquals(DispatchOutcome.SENT, d.dispatch(event(it + 1), values)) }
        assertEquals(DispatchOutcome.SKIPPED_RATE_LIMIT, d.dispatch(event(999), values))
    }

    @Test
    fun optOutSkipsAndLogs() = runBlocking {
        val log = FakeLog(); val posted = mutableListOf<String>()
        val outcome = dispatcher(settings = FakeSettings(enabled = false), log = log, posted = posted)
            .dispatch(event(), values)
        assertEquals(DispatchOutcome.SKIPPED_OPTOUT, outcome)
        assertTrue(posted.isEmpty())
        assertEquals(STATUS_SKIPPED_OPTOUT, log.statuses[event().eventId])
    }

    @Test
    fun defaultUserIsRejected() = runBlocking {
        val e = NotificationEvent.reminder(DEFAULT_USER_ID, "task1", 1L)
        assertEquals(DispatchOutcome.SKIPPED_INVALID_USER, dispatcher().dispatch(e, values))
    }

    @Test
    fun missingPermissionSkipsWithoutCrash() = runBlocking {
        val log = FakeLog()
        val d = ReminderDispatcher(FakeSettings(), log, FakeRateLimiter(), canPost = { false }, post = { _, _ -> error("không được gọi") })
        assertEquals(DispatchOutcome.SKIPPED_PERMISSION, d.dispatch(event(), values))
        assertEquals(STATUS_SKIPPED_PERMISSION, log.statuses[event().eventId])
    }
}

class RealDateLabelTest {

    @Test
    fun realTodayAndTomorrowAreRecognized() {
        val today = com.example.btvn_tuan4.model.TodoTask(date = com.example.btvn_tuan4.model.TodoTask.isoDate(0), time = "14:00")
        val tomorrow = today.copy(date = com.example.btvn_tuan4.model.TodoTask.isoDate(1))
        assertTrue(today.isToday)
        assertEquals("Hôm nay, 14:00", today.displayDeadline)
        assertEquals("Ngày mai, 14:00", tomorrow.displayDeadline)
    }
}

class OfflineDispatchTest {

    // Gia lap Firestore offline: moi lan doc/ghi deu nem loi
    private val offline = Exception("Failed to get document because the client is offline.")

    private val offlineSettings = object : INotificationSettingsRepository {
        override suspend fun isTaskRemindersEnabled(userId: String): Boolean = throw offline
        override suspend fun setTaskRemindersEnabled(userId: String, enabled: Boolean) = throw offline
    }
    private val offlineLog = object : INotificationLogRepository {
        override suspend fun getStatus(userId: String, eventId: String): String? = throw offline
        override suspend fun log(userId: String, event: NotificationEvent, status: String, attempts: Int) = throw offline
        override suspend fun markOpened(userId: String, eventId: String) = throw offline
    }
    private val offlineLimiter = object : IRateLimiter {
        override suspend fun tryAcquire(userId: String, nowMs: Long): Boolean = throw offline
    }

    @Test
    fun reminderIsStillShownWhenFirestoreIsOffline() = runBlocking {
        val posted = mutableListOf<String>()
        val d = ReminderDispatcher(offlineSettings, offlineLog, offlineLimiter, canPost = { true }, post = { e, _ -> posted += e.eventId })
        val e = NotificationEvent.reminder("u1", "task1", 1L)
        assertEquals(DispatchOutcome.SENT, d.dispatch(e, mapOf("TITLE" to "T")))
        assertEquals(listOf(e.eventId), posted)
    }
    // Loi thuc te tren emulator: rate limit het thoi gian (withTimeout) khong duoc lam worker retry
    @Test
    fun rateLimitTimeoutStillSends() = runBlocking {
        val slowLimiter = object : IRateLimiter {
            override suspend fun tryAcquire(userId: String, nowMs: Long): Boolean =
                kotlinx.coroutines.withTimeout(10) { kotlinx.coroutines.delay(1_000); true }
        }
        val posted = mutableListOf<String>()
        val d = ReminderDispatcher(offlineSettings, offlineLog, slowLimiter, canPost = { true }, post = { e, _ -> posted += e.eventId })
        assertEquals(DispatchOutcome.SENT, d.dispatch(NotificationEvent.reminder("u1", "task2", 2L), emptyMap()))
        assertEquals(1, posted.size)
    }
}
