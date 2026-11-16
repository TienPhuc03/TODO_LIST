// Phase B (Chuong 10): worker phia server doc hang doi notificationQueue va gui push qua FCM.
// Pipeline giong ban local trong app: dedupe -> opt-in -> rate limit -> gui -> ghi notificationLog, co retry.
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

const MAX_ATTEMPTS = 3;
const RATE_LIMIT_PER_HOUR = 20;
const INVALID_TOKEN_CODES = [
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
];

// Cung mau voi NotificationTemplate.TASK_REMINDER trong app
const TEMPLATE = {
  title: "Sắp đến hạn: {TITLE}",
  body: "Công việc \"{TITLE}\" đến hạn lúc {TIME} (ưu tiên {PRIORITY}).",
};

function render(text, values) {
  return text.replace(/\{([A-Z_]+)\}/g, (_, key) => (values && values[key]) || "");
}

// Khoa gio "yyyyMMddHH" theo gio Viet Nam, trung voi RateLimiter.kt
function hourKey(date = new Date()) {
  const vn = new Date(date.getTime() + 7 * 60 * 60 * 1000);
  return vn.toISOString().slice(0, 13).replace(/[-T]/g, "");
}

async function tryAcquireRateLimit(uid) {
  const ref = db.doc(`users/${uid}/rateLimits/${hourKey()}`);
  return db.runTransaction(async (tx) => {
    const count = (await tx.get(ref)).get("count") || 0;
    if (count >= RATE_LIMIT_PER_HOUR) return false;
    tx.set(ref, { count: count + 1 }, { merge: true });
    return true;
  });
}

// Tao document users/{uid}/notificationQueue/{eventId} voi:
//   { taskId, values: { TITLE, TIME, PRIORITY } }  hoac  { taskId, title, body }
// retry: true -> neu ham nem loi, Cloud Functions tu chay lai (kem kiem tra so lan thu ben duoi)
exports.sendQueuedNotification = onDocumentCreated(
  // Neu Firestore khong o us-central1, them region trung vi tri database, vd: region: "asia-southeast1"
  { document: "users/{uid}/notificationQueue/{eventId}", retry: true },
  async (event) => {
    const { uid, eventId } = event.params;
    const item = event.data.data();
    const logRef = db.doc(`users/${uid}/notificationLog/${eventId}`);
    const queueRef = event.data.ref;

    const log = await logRef.get();
    const status = log.get("status");
    const attempts = (log.get("attempts") || 0) + 1;

    const writeLog = (newStatus, extra = {}) =>
      Promise.all([
        logRef.set(
          {
            eventId,
            taskId: item.taskId || "",
            type: "TASK_REMINDER",
            channel: "push",
            status: newStatus,
            attempts,
            createdAt: log.exists ? log.get("createdAt") : FieldValue.serverTimestamp(),
            updatedAt: FieldValue.serverTimestamp(),
            ...extra,
          },
          { merge: true }
        ),
        queueRef.set({ status: newStatus }, { merge: true }),
      ]);

    // Dedupe: event da gui roi thi bo qua (vd ham bi goi lai)
    if (status === "sent" || status === "opened") {
      logger.info(`Bo qua ${eventId}: da gui`);
      return;
    }

    // Opt-in (mac dinh bat)
    const settings = await db.doc(`users/${uid}/notificationSettings/default`).get();
    if (settings.get("taskReminders") === false) {
      await writeLog("skipped_optout");
      return;
    }

    // Rate limit chi tinh o lan thu dau, retry khong ton them quota
    if (attempts === 1 && !(await tryAcquireRateLimit(uid))) {
      await writeLog("skipped_rate_limit");
      return;
    }

    const devices = await db.collection(`users/${uid}/devices`).get();
    const tokens = devices.docs.map((d) => d.id);
    if (tokens.length === 0) {
      await writeLog("skipped_no_device");
      return;
    }

    const title = item.title || render(TEMPLATE.title, item.values);
    const body = item.body || render(TEMPLATE.body, item.values);

    try {
      const res = await getMessaging().sendEachForMulticast({
        tokens,
        notification: { title, body },
        // Key trung voi NotificationDisplay.EXTRA_* de bam thong bao mo dung task va ghi "opened"
        data: {
          notification_task_id: String(item.taskId || ""),
          notification_event_id: eventId,
          notification_user_id: uid,
        },
        android: { priority: "high", notification: { channelId: "task_reminders" } },
      });

      // Xoa token het han de lan sau khong gui vao thiet bi khong ton tai
      const cleanup = [];
      res.responses.forEach((r, i) => {
        if (!r.success && INVALID_TOKEN_CODES.includes(r.error.code)) {
          cleanup.push(db.doc(`users/${uid}/devices/${tokens[i]}`).delete());
        }
      });
      await Promise.all(cleanup);

      if (res.successCount === 0) {
        throw new Error(`Gui that bai toi ca ${tokens.length} thiet bi`);
      }
      await writeLog("sent", { successCount: res.successCount, failureCount: res.failureCount });
      logger.info(`Da gui ${eventId} toi ${res.successCount}/${tokens.length} thiet bi`);
    } catch (err) {
      if (attempts >= MAX_ATTEMPTS) {
        await writeLog("failed", { error: String(err.message || err) });
        logger.error(`${eventId} that bai sau ${attempts} lan`, err);
        return; // khong nem loi nua -> dung retry
      }
      await writeLog("retrying", { error: String(err.message || err) });
      throw err; // nem loi -> Cloud Functions chay lai voi backoff
    }
  }
);
