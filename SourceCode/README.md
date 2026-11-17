# UTH SmartTasks

App Android quản lý công việc (Jetpack Compose + Firebase Auth/Firestore + REST API mock).
Tuần 10 thêm **thông báo nhắc việc** theo Chương 10 (Design a Notification System).

## Cài đặt và chạy

1. **Firebase:** tải `google-services.json` của project Firebase (đã bật Authentication và Firestore) và đặt vào `app/`. File này không có trong repo.
2. **`local.properties`** (ở thư mục gốc, không commit). Ngoài `sdk.dir`, thêm:
   ```properties
   TASKS_API_BASE_URL=https://share.tryapps.store/mock/teach
   TASKS_API_TOKEN=<token được cấp>
   ```
   Các giá trị này được đưa vào `BuildConfig` (xem `app/build.gradle.kts`), không còn nằm trong source code.
3. Build và test:
   ```bash
   ./gradlew :app:assembleDebug
   ./gradlew :app:testDebugUnitTest
   ```

## Cấu trúc Firestore

```
users/{uid}                                   userId, name, email, createdAt, lastActive
users/{uid}/tasks/{taskId}                    id, title, note, categoryName, date (yyyy-MM-dd), time (HH:mm),
                                              remindMe, priority, isCompleted, subtasks[], createdAt
users/{uid}/notificationSettings/default      taskReminders: Boolean (mặc định true)
users/{uid}/notificationLog/{eventId}         eventId, taskId, type, scheduledAt, status, attempts,
                                              createdAt, updatedAt, openedAt
users/{uid}/rateLimits/{yyyyMMddHH}           count
users/{uid}/devices/{fcmToken}                token, platform, updatedAt            (Phase B)
users/{uid}/notificationQueue/{eventId}       taskId, values{TITLE,TIME,PRIORITY} hoặc title/body, status   (Phase B)
```

`eventId = "{taskId}_reminder_{scheduledAtEpochMs}"`.
`status` gồm `scheduled`, `retrying`, `sent`, `opened`, `failed`, `skipped_past`, `skipped_optout`, `skipped_permission`, `skipped_rate_limit`.

**Security rules:** file [`firestore.rules`](firestore.rules): mỗi user chỉ đọc và ghi dữ liệu của chính mình (`users/{uid}/**`). Dán nội dung file vào Firebase Console → Firestore Database → Rules → Publish, hoặc chạy `firebase deploy --only firestore:rules`.

## Thông báo nhắc việc (Chương 10)

Mã nguồn trong `app/src/main/java/com/example/btvn_tuan4/notification/`.

| Thành phần Chương 10 | File |
|---|---|
| Event | `NotificationEvent.kt` |
| Template | `NotificationTemplate.kt` |
| Scheduler | `ReminderScheduler.kt` (WorkManager, unique work `reminder_{taskId}`) |
| Worker và retry | `ReminderWorker.kt` (`Result.retry()`, backoff mũ 30 giây, tối đa 3 lần rồi `failed`) |
| Opt-in | `NotificationSettingsRepository.kt`, toggle ở màn Hồ sơ |
| Dedupe | `NotificationLogRepository.kt` (không gửi lại event đã `sent`/`opened`) |
| Rate limit | `RateLimiter.kt` (20 thông báo mỗi giờ, transaction Firestore) |
| Tracking | `MainActivity.kt` ghi `opened` khi bấm thông báo |
| Monitoring | `notificationLog`: `status`, `attempts`, `createdAt`, `updatedAt` |
| Kênh thông báo | `NotificationChannels.kt` (`task_reminders`) |
| Hiển thị thông báo | `NotificationDisplay.kt` (dùng chung cho local và push) |
| Thiết bị (Phase B) | `DeviceTokenRepository.kt`, `PushMessagingService.kt` |
| Hàng đợi + worker server (Phase B) | `functions/index.js` |

**Luồng:** lưu task có `remindMe` → `FirestoreTaskRepository` gọi `ReminderScheduler.schedule` (hoặc huỷ khi task xong, bị xoá, hoặc tắt nhắc) → đến `hạn - 15 phút`, `ReminderWorker` kiểm tra lần lượt: user hợp lệ (không phải `default_user`) → opt-in → dedupe → quyền thông báo → rate limit → hiển thị thông báo và ghi `sent` → bấm thông báo mở `task_detail/{taskId}` và ghi `opened`.

**Offline:** nhắc việc local vẫn hiện khi máy mất mạng. Mỗi bước kiểm tra cần Firestore sẽ đọc bản lưu trên máy (cache); nếu không đọc được thì dùng giá trị an toàn: opt-in coi như bật, coi như chưa gửi, rate limit cho phép (fail-open). Log ghi không chờ server, Firestore tự đồng bộ khi có mạng lại. Worker chỉ `retry` khi lỗi xảy ra lúc hiện thông báo.

**Soft real-time:** WorkManager có thể chạy trễ khi máy ở chế độ Doze. Điều này chấp nhận được với nhắc việc, nên app không dùng `AlarmManager` exact.

Màn Hồ sơ hiển thị "Đang chờ nhắc: N" (số lịch nhắc chưa chạy trong WorkManager). Đăng xuất sẽ gỡ FCM token của máy và huỷ toàn bộ lịch nhắc.

## Phase B: push qua FCM

**Phía app (đã chạy được, không cần gói Blaze):**
- Sau khi đăng nhập, app lấy FCM token và lưu vào `users/{uid}/devices/{token}`. Khi đăng xuất thì xoá.
- `PushMessagingService` nhận push. App đang mở thì tự hiện thông báo; app ở nền thì hệ thống hiện. Bấm vào đều mở đúng `task_detail` và ghi `opened`.
- Thử nhanh không cần server: Firebase Console → **Messaging** → tạo campaign **Firebase Notification messages** → **Send test message**, dán token lấy từ Firestore (`users/{uid}/devices`).

**Phía server (`functions/`, cần gói Blaze để deploy):**
- `sendQueuedNotification` chạy khi có document mới ở `users/{uid}/notificationQueue/{eventId}`. Hàm kiểm tra dedupe → opt-in → rate limit (20/giờ) → gửi FCM tới mọi thiết bị của user → xoá token hết hạn → ghi `notificationLog` (`channel: "push"`). Lỗi thì ném exception để Cloud Functions tự retry, quá 3 lần ghi `failed`.
- Deploy:
  ```bash
  npm install -g firebase-tools
  firebase login
  firebase use --add
  cd functions && npm install && cd ..
  firebase deploy --only functions,firestore:rules
  ```
- Nếu Firestore không ở `us-central1`, thêm `region` trùng vị trí database vào tuỳ chọn của hàm (xem comment trong `functions/index.js`).
- Thử: trong Firestore tạo document `users/{uid}/notificationQueue/test1` với `taskId = "1"` và map `values = {TITLE: "Học Kotlin", TIME: "14:00", PRIORITY: "Cao"}`. Vài giây sau điện thoại nhận push, `notificationLog/test1` có `status = "sent"`.

### Kiểm thử thủ công

1. Đăng nhập bằng email/password (không dùng tài khoản mặc định `default_user`).
2. Thêm task, bấm dòng **Ngày** để chọn ngày hôm nay trên lịch, bấm dòng **Giờ** để chọn giờ hiện tại + 16 phút, bật **Nhắc nhở**.
3. Khoảng 1 phút sau có thông báo. Bấm vào thì mở màn Chi tiết của task.
4. Tắt "Nhắc việc trước hạn" ở màn Hồ sơ, tạo task tương tự: không có thông báo, log ghi `skipped_optout`.
5. Từ chối quyền thông báo (Android 13+): app không crash, log ghi `skipped_permission`.

## Hạn chế

- Task mẫu từ API có ngày 20–24/10/2026 (đã qua) nên không được lên lịch (`skipped_past`). Việc mới do người dùng tự chọn ngày và giờ bằng lịch và đồng hồ hệ thống; lịch không cho chọn ngày đã qua.
- Nhắc việc local và push là hai kênh độc lập; app chưa tự ghi vào `notificationQueue`, hàng đợi dành cho backend hoặc tạo tay để demo.
- Token API cũ đã từng nằm trong lịch sử git, nên đổi token nếu có thể.
- Chưa làm SMS, email, iOS (chỉ nêu trong thiết kế).
- Cloud Function chưa được deploy và chạy thử thật (cần gói Blaze).
