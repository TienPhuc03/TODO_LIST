# UTH SmartTasks: ứng dụng quản lý công việc có nhắc việc

Ứng dụng Android quản lý công việc cá nhân, làm bằng Jetpack Compose. Dữ liệu được lưu trên Firebase (Authentication và Cloud Firestore), đồng thời lấy dữ liệu mẫu từ một REST API mock. Phần **thông báo nhắc việc** được xây dựng theo Chương 10 (Design a Notification System) trong sách *System Design Interview* của Alex Xu.

Đây là bài tập môn Lập trình di động. Repo này gồm mã nguồn và tài liệu báo cáo.

---

## 1. Cấu trúc thư mục

```
BTVN_Tuan4/                        (thư mục cha, chứa git repo)
├── .git/                          lịch sử git, nhánh feature/notifications
├── README.md                      file này
├── SourceCode/                    toàn bộ mã nguồn, mở project Android từ đây
│   ├── app/                       module Android
│   │   └── src/main/java/com/example/btvn_tuan4/
│   │       ├── MainActivity.kt    điểm vào, điều hướng, deep link thông báo
│   │       ├── model/             TodoTask, AuthUser, SubTask
│   │       ├── network/           TaskApiService (REST API mock)
│   │       ├── repository/        TaskRepository, AuthRepository, OnboardingRepository
│   │       ├── notification/      toàn bộ chức năng nhắc việc (11 file)
│   │       └── ui/                màn hình (Compose) và component dùng chung
│   ├── functions/                 Cloud Function gửi push (Phase B, chưa deploy)
│   ├── firestore.rules            quy tắc bảo mật Firestore
│   ├── firebase.json              cấu hình Firebase CLI (rules + functions)
│   ├── gradle/, gradlew, gradlew.bat, build.gradle.kts, settings.gradle.kts
│   └── README.md                  chi tiết kỹ thuật nhắc việc và Phase B
└── TaiLieu/                       tài liệu báo cáo
    ├── GIAI_THICH_NOTIFICATION.md             giải thích chi tiết chức năng thông báo
    ├── TECHNICAL_HANDOFF_THONG_BAO_NHAC_VIEC_CHUONG10.docx   tài liệu bàn giao kỹ thuật
    └── handoff_claude_code.md                 đặc tả ban đầu của chức năng thông báo
```

Mở project trong Android Studio bằng thư mục **`SourceCode`**, không phải thư mục cha. Mở thư mục cha sẽ không nhận ra project Gradle.

---

## 2. Tính năng

**Quản lý công việc (có sẵn):**
- Đăng nhập bằng email và mật khẩu. Có màn giới thiệu (onboarding) trước khi đăng nhập.
- Thêm, sửa, xoá, đánh dấu hoàn thành công việc. Mỗi công việc có ngày, giờ, mức ưu tiên, danh mục, ghi chú và danh sách việc nhỏ (subtask).
- Trang chủ hiển thị "Việc hôm nay", danh sách công việc và màn chi tiết.
- Đồng bộ dữ liệu giữa REST API mock và Cloud Firestore.

**Nhắc việc (thêm trong bài này):**
- Khi lưu công việc có bật "Nhắc nhở", app sẽ hiện thông báo **trước hạn 15 phút**, đúng giờ theo múi giờ Việt Nam.
- Người dùng chọn ngày và giờ bằng lịch và đồng hồ của hệ thống. Không cho chọn ngày đã qua.
- Bấm vào thông báo sẽ mở đúng công việc đó.
- Bật/tắt nhắc việc ở màn Hồ sơ. Màn này cũng hiển thị số lịch nhắc đang chờ.
- Hoạt động khi app đang mở, đang chạy nền, và cả khi máy mất mạng (xem mục 8).

Các thành phần của Chương 10 được ánh xạ sang code như sau:

| Chương 10 | Triển khai |
|---|---|
| Event | `NotificationEvent.kt` |
| Template | `NotificationTemplate.kt` |
| Scheduler | `ReminderScheduler.kt` (WorkManager) |
| Worker, retry | `ReminderWorker.kt` (có backoff, tối đa 3 lần) |
| Opt-in | `NotificationSettingsRepository.kt` |
| Dedupe | `NotificationLogRepository.kt` |
| Rate limit | `RateLimiter.kt` (20 thông báo mỗi giờ) |
| Tracking, monitoring | `notificationLog` trên Firestore |

Chi tiết từng thành phần và luồng xử lý nằm trong [SourceCode/README.md](SourceCode/README.md) và [TaiLieu/GIAI_THICH_NOTIFICATION.md](TaiLieu/GIAI_THICH_NOTIFICATION.md).

---

## 3. Yêu cầu môi trường

| Thành phần | Yêu cầu |
|---|---|
| Android Studio | Bản mới, hỗ trợ AGP 9.4.1 |
| JDK | 17 trở lên (đã build với JDK 21). Java source và target là 17 |
| Android SDK | compileSdk 37, targetSdk 37, minSdk 24 |
| Thiết bị | Emulator hoặc điện thoại Android 7.0 (API 24) trở lên. Để test thông báo trên Android 13+, cần Android 13 trở lên |
| Google Play services | Cần cho FCM. Emulator phải có biểu tượng **Google Play** |
| Tài khoản Google | Để tạo project Firebase |
| Node.js 20 | Chỉ cần khi deploy Cloud Function (Phase B) |

---

## 4. Cài đặt và chạy từ đầu

Làm các bước theo thứ tự. Các bước 1 đến 4 chỉ làm một lần.

### Bước 1: Lấy mã nguồn

```bash
git clone https://github.com/TienPhuc03/TODO_LIST.git BTVN_Tuan4
cd BTVN_Tuan4
git checkout feature/notifications
```

> **Lưu ý:** cấu trúc `SourceCode/` và `TaiLieu/` chỉ có trên remote sau khi các thay đổi đã được commit và push. Nếu clone trước khi push, bạn sẽ thấy cấu trúc cũ với mã nguồn ở thư mục gốc.

Nếu đã có sẵn thư mục repo, chỉ cần chuyển nhánh và kéo về:

```bash
cd BTVN_Tuan4
git checkout feature/notifications
git pull
```

### Bước 2: Tạo project Firebase

1. Vào [Firebase Console](https://console.firebase.google.com) và tạo project mới (hoặc dùng project đã có).
2. **Authentication:** vào **Build → Authentication → Sign-in method**, bật **Email/Password**. Vào tab **Users** và tạo một tài khoản test.
3. **Firestore:** vào **Build → Firestore Database → Create database**. Chọn chế độ **production** và vị trí gần bạn.
4. **Rules:** vào tab **Rules**, dán nội dung file `SourceCode/firestore.rules`, rồi bấm **Publish**.

   Nội dung rules:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{uid}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == uid;
       }
     }
   }
   ```

   Rules này không cho phép đọc hay ghi dữ liệu của user khác.

### Bước 3: Đăng ký ứng dụng Android và tải `google-services.json`

1. Trong Firebase Console, bấm biểu tượng **⚙ Project settings → General**.
2. Ở mục **Your apps**, bấm **Add app → Android**.
3. Nhập **Android package name**: `com.example.btvn_tuan4`. Phải đúng chính xác, nếu không app sẽ không khởi động được.
4. Bấm **Register app**, rồi **Download google-services.json**.
5. Chép file vừa tải vào thư mục `SourceCode/app/`:

   ```
   BTVN_Tuan4/SourceCode/app/google-services.json
   ```

File này không có trong repo vì đã bị `.gitignore` bỏ qua. Không đưa file này lên GitHub.

### Bước 4: Tạo `local.properties`

File `SourceCode/local.properties` chứa đường dẫn SDK và thông tin REST API. Android Studio tự tạo dòng `sdk.dir` khi mở project. Bạn cần thêm hai dòng sau:

```properties
TASKS_API_BASE_URL=https://share.tryapps.store/mock/teach
TASKS_API_TOKEN=<token được cấp cho bài tập>
```

- `TASKS_API_BASE_URL` là địa chỉ REST API mock. Giá trị đã đúng, không cần đổi.
- `TASKS_API_TOKEN` là token được cấp. Không ghi token vào source code. `build.gradle.kts` sẽ đưa token vào `BuildConfig` khi build.
- File này đã bị `.gitignore` bỏ qua. Không commit file này.

Nếu không có token, app vẫn build được, nhưng các lời gọi REST API sẽ bị từ chối. Chức năng Firestore và nhắc việc vẫn chạy.

### Bước 5: Mở project và đồng bộ Gradle

1. Mở Android Studio. Chọn **File → Open**, trỏ tới thư mục **`BTVN_Tuan4/SourceCode`**.
2. Đợi Gradle sync xong (thanh tiến trình ở dưới cùng).
3. Nếu thấy lỗi đỏ như `Unresolved reference`, bấm **Sync Now** hoặc nhấn `Ctrl + Shift + O`.

### Bước 6: Tạo emulator

1. Mở **Device Manager**, bấm **Create Virtual Device**.
2. Chọn **Pixel 8**. Ở bước chọn system image, chọn image có biểu tượng **Google Play**, ví dụ API 35.
3. Khởi động emulator.

Sau khi emulator khởi động, kiểm tra mạng bằng cách mở Chrome và vào `google.com`. Nếu không vào được, xem mục 8.

### Bước 7: Build và chạy

Trong Android Studio, chọn cấu hình chạy **app**, chọn emulator ở ô bên cạnh, rồi bấm ▶.

Hoặc chạy từ terminal, trong thư mục `SourceCode`:

```bash
cd SourceCode
./gradlew :app:assembleDebug
```

Trên PowerShell của Windows, dùng `.\gradlew.bat :app:assembleDebug`.

File APK nằm ở `SourceCode/app/build/outputs/apk/debug/app-debug.apk`. Có thể cài thủ công bằng `adb install`.

### Bước 8: Chạy unit test

```bash
cd SourceCode
./gradlew :app:testDebugUnitTest
```

Kết quả mong đợi: **15 test đều pass**. Báo cáo test nằm ở `SourceCode/app/build/reports/tests/testDebugUnitTest/index.html`.

---

## 5. Kiểm thử nhanh sau khi chạy

1. Đăng nhập bằng tài khoản email đã tạo ở bước 2. Cho phép thông báo khi app hỏi.
2. Trên Trang chủ, bấm **+** để thêm công việc:
   - Chọn **Ngày** là hôm nay.
   - Chọn **Giờ** là giờ hiện tại + 16 phút.
   - Bật **Nhắc nhở**, rồi lưu.
3. Chờ khoảng 1 phút. Thông báo "Sắp đến hạn: …" sẽ hiện.
4. Bấm vào thông báo. App mở đúng màn chi tiết công việc.
5. Vào Firebase Console → **Firestore Database → Data**, mở `users/{uid}/notificationLog`. Document có `status: "opened"`.

Để kiểm tra kỹ hơn, xem Logcat với bộ lọc `tag:ReminderWorker | tag:ReminderScheduler`.

---

## 6. Phase B: push FCM và Cloud Function

Phase B là phần mở rộng, không bắt buộc để nhắc việc local chạy.

**Phía app** (đã có, không cần gói trả phí):
- App lưu FCM token vào `users/{uid}/devices/{token}` sau khi đăng nhập.
- Để gửi thử một push: Firebase Console → **Messaging → Create your first campaign → Firebase Notification messages → Send test message**. Dán token lấy từ `users/{uid}/devices`.

**Phía server** (cần gói Blaze, cần deploy):

```bash
npm install -g firebase-tools
firebase login
cd SourceCode
firebase use --add
cd functions && npm install && cd ..
firebase deploy --only functions,firestore:rules
```

Hàm `sendQueuedNotification` trong `SourceCode/functions/index.js` gửi push khi có document mới trong `users/{uid}/notificationQueue`. Lưu ý: app không tự ghi vào collection này. Muốn kiểm thử phải tạo document bằng tay trên Console.

Nếu Firestore không đặt ở `us-central1`, cần thêm `region` vào hàm cho khớp vị trí database. Chi tiết xem [SourceCode/README.md](SourceCode/README.md).

---

## 7. Dữ liệu trên Firestore

```
users/{uid}
├── tasks/{taskId}                       công việc
├── notificationSettings/default         taskReminders: bật/tắt nhắc việc
├── notificationLog/{eventId}            trạng thái từng lần nhắc
├── rateLimits/{yyyyMMddHH}              số thông báo đã gửi trong giờ
├── devices/{fcmToken}                   FCM token (Phase B)
└── notificationQueue/{eventId}          hàng đợi cho Cloud Function (Phase B)
```

Trạng thái trong `notificationLog`: `scheduled` → `sent` → `opened`. Các trạng thái khác: `retrying`, `failed`, `skipped_past`, `skipped_optout`, `skipped_permission`, `skipped_rate_limit`.

---

## 8. Lỗi thường gặp

| Hiện tượng | Nguyên nhân và cách xử lý |
|---|---|
| Build báo lỗi liên quan `google-services` | Chưa làm bước 3, hoặc file không nằm đúng `SourceCode/app/`. Kiểm tra lại package name `com.example.btvn_tuan4` khi đăng ký app |
| Đỏ `Unresolved reference 'messaging'` hoặc `FirebaseMessaging` | Chưa Gradle sync. Bấm **Sync Now** hoặc `Ctrl + Shift + O` |
| Log `Unable to resolve host firestore.googleapis.com` | Emulator mất mạng hoặc DNS lỗi. Mở Chrome trong emulator để kiểm tra. Có thể bật **Private DNS** với `dns.google` trong Settings, hoặc **Cold Boot** emulator |
| Thông báo không hiện khi app ở nền | Với Android 14 trở lên, hệ thống chặn mạng app khi chạy nền. App vẫn hiện thông báo nhờ cơ chế fail-open. Log trên Firestore sẽ đồng bộ khi bạn mở lại app |
| Bấm thông báo nhưng không mở task | Thường do app chưa đăng nhập. Đăng nhập rồi thử lại |
| Log `GET /tasks/{id} ... HTTP 404` | Bình thường với công việc do bạn tạo. API mock không lưu công việc đó. Màn chi tiết vẫn hiển thị vì đọc từ Firestore |
| `PERMISSION_DENIED` trong Firestore | Rules chưa được publish, hoặc chưa đăng nhập. Làm lại bước 2 |
| Không có thông báo, Logcat có `SKIPPED_OPTOUT` | Công tắc "Nhắc việc trước hạn" ở Hồ sơ đang tắt. Bật lại |
| Không có thông báo, Logcat có `SKIPPED_PERMISSION` | Quyền thông báo đang bị từ chối. Vào Settings của emulator → Apps → UTH SmartTasks → Notifications và bật lại |

Khi gặp lỗi khác, lọc Logcat theo `package:mine` để chỉ xem log của app.

---

## 9. Lưu ý bảo mật

- **Token REST API đã nằm trong lịch sử git** tại commit `eac87c8`, vì phiên bản đầu của project có hard-code token. Commit mới đã chuyển token sang `local.properties`, nhưng token cũ vẫn xem được trên GitHub. Nên đổi token nếu có thể.
- `local.properties`, `google-services.json` và thư mục `build/` đã được `.gitignore` bỏ qua. Trước khi push, chạy `git status` để chắc các file này không xuất hiện.
- Firestore rules chỉ bảo vệ dữ liệu khi đã publish. Bước 2 là bắt buộc để dữ liệu của user được bảo vệ.

---

## 10. Trạng thái và giới hạn

**Đã kiểm thử:**
- 15 unit test pass.
- Luồng nhắc việc local chạy trên emulator Pixel 8 (Android 15): lên lịch, hiện thông báo, bấm mở đúng task, ghi `opened`.
- Retry với backoff và fail-open khi offline.

**Chưa kiểm thử hoặc chưa làm:**
- Các kịch bản "từ chối quyền", "tắt nhắc việc", "hoàn thành hoặc xoá task huỷ lịch" đã có hướng dẫn nhưng chưa có bằng chứng kết quả đầy đủ.
- Push FCM và Cloud Function chưa được deploy và chưa chạy thật.
- SMS, email và iOS chỉ có trong thiết kế, không triển khai.
- Đăng nhập Google và đăng nhập ẩn danh trong app là giả lập.

---

## 11. Tác giả và nhánh

- Nhánh làm việc: `feature/notifications`, tách từ `main` tại commit `eac87c8`.
- Các commit theo Conventional Commits, ví dụ `feat: them module thong bao nhac viec theo Chuong 10`.
