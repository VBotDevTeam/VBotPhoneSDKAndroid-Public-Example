# VBotPhone-Android-Public-example

Ứng dụng mẫu minh họa cách tích hợp **VBot Phone SDK** vào ứng dụng Android để thực hiện và nhận cuộc gọi VoIP (SIP).

---

## Tính năng

- **Gọi đi (Outgoing call):** Quay số qua bàn phím hoặc chọn thành viên trong danh bạ mẫu.
- **Nhận cuộc gọi đến (Incoming call):** Nhận push notification thời gian thực qua Firebase Cloud Messaging (FCM).
- **Chọn Hotline:** Lấy danh sách Hotline từ tổng đài và chọn Hotline làm đầu số gọi đi.
- **Điều khiển cuộc gọi:** 
  - Trả lời / Từ chối cuộc gọi đến.
  - Bật / Tắt Microphone (Mute).
  - Bật / Tắt Loa ngoài (Speakerphone).
  - Gửi phím số DTMF trong cuộc gọi.
  - Kết thúc cuộc gọi (Hangup).
- **Quản lý kết nối tài khoản:** Kết nối (Connect) và Ngắt kết nối (Disconnect) qua Token và Firebase Push Token.
- **Theo dõi trạng thái:** Hiển thị chi tiết trạng thái cuộc gọi (`Incoming`, `Calling`, `Connecting`, `Confirmed`, `Disconnected`...).

---

## Yêu cầu môi trường

- **Android SDK:** `minSdk 23` (Android 6.0+), `compileSdk 34` (Android 14+).
- **JDK:** Java 17.
- **Gradle:** Gradle 8.7 & Android Gradle Plugin (AGP) 8.5.0+.
- **Android Studio:** Bản Iguana, Jellyfish, Koala hoặc mới hơn.
- **Firebase Project:** Cần thiết lập dự án Firebase để nhận push notification cuộc gọi đến.

---

## Cài đặt & Chạy ứng dụng

### 1. Clone repository

```bash
git clone https://github.com/VBotDevTeam/VBotPhone-Android-Public-example.git
cd VBotPhone-Android-Public-example
```

### 2. Mở bằng Android Studio

1. Khởi động **Android Studio**.
2. Chọn **Open** và duyệt tới thư mục `VBotPhone-Android-Public-example`.
3. Chờ Gradle đồng bộ (Sync) các dependencies.

### 3. Build từ dòng lệnh (tuỳ chọn)

```bash
./gradlew assembleDebug
```

File APK sẽ được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

---

## Cấu hình tích hợp SDK vào ứng dụng của bạn

### 1. Khai báo Maven Repository

Trong `settings.gradle` (hoặc `settings.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Repository Maven tĩnh chính thức của VBot Phone SDK Android
        maven(url = "https://raw.githubusercontent.com/VBotDevTeam/VBotPhoneSDKAndroid-Public/main/")
    }
}
```

### 2. Thêm Dependency

Trong `app/build.gradle`:

```groovy
dependencies {
    // VBot Phone SDK Android (phiên bản mới nhất)
    implementation 'com.github.VBotDevTeam:VBotPhoneSDKAndroid-Public:1.1.5'

    // Firebase Messaging (để nhận cuộc gọi đến qua Push Notification)
    implementation platform('com.google.firebase:firebase-bom:33.1.2')
    implementation 'com.google.firebase:firebase-messaging-ktx'
}
```

> **Lưu ý:** Từ phiên bản `1.1.0` trở lên, toàn bộ các thư viện bên thứ ba (OkHttp, Retrofit, RxJava, Gson...) đã được đóng gói và che giấu (shaded) bên trong SDK. Ứng dụng tích hợp **không** cần khai báo thêm và sẽ không gặp xung đột thư viện.

### 3. Cấu hình Firebase (`google-services.json`)

1. Vào [Firebase Console](https://console.firebase.google.com/) và tải file `google-services.json` của ứng dụng bạn.
2. Sao chép và ghi đè file `app/google-services.json`.
3. Đảm bảo `applicationId` trong `app/build.gradle` khớp với `package_name` trên Firebase.

### 4. Cấu hình quyền trong `AndroidManifest.xml`

Thêm các quyền cần thiết:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.DISABLE_KEYGUARD" />
```

---

## Cấu trúc Project

```text
VBotPhone-Android-Public-example/
├── gradle/wrapper/                 # Gradle Wrapper 8.7
├── build.gradle                    # Top-level build script
├── settings.gradle.kts             # Cấu hình Maven repository
├── app/
│   ├── build.gradle                # Cấu hình build & dependencies
│   ├── google-services.json        # File cấu hình Firebase mẫu
│   └── src/main/
│       ├── AndroidManifest.xml     # Khai báo permissions, Activity, FCM Service
│       ├── java/com/vpmedia/sdkvbot/example/
│       │   ├── MainActivity.kt          # Màn hình chính điều khiển cuộc gọi
│       │   ├── SelectMemberActivity.kt  # Màn hình chọn tài khoản mẫu
│       │   ├── VBotFcmService.kt        # Service đón Push Notification FCM
│       │   ├── TestData.kt              # Dữ liệu tài khoản kiểm thử
│       │   └── Helpers.kt               # Hàm tiện ích giao diện
│       └── res/                         # Giao diện (Layouts, Drawables, Values)
```

---

## Hướng dẫn sử dụng SDK (Code mẫu)

### 1. Khởi tạo SDK

```kotlin
val client = VBotClient(applicationContext)

// Khởi tạo cấu hình môi trường (PRODUCTION hoặc STAGING)
val config = VBotConfig(VBotEnvironment.PRODUCTION)
client.setup(config)
```

### 2. Đăng ký lắng nghe sự kiện (`ClientListener`)

```kotlin
val listener = object : ClientListener() {
    override fun onUserConnected(displayName: String) {
        Log.d("VBot", "Đã kết nối người dùng: $displayName")
    }

    override fun onCallState(state: CallState) {
        Log.d("VBot", "Trạng thái cuộc gọi: $state")
        // Các trạng thái: Incoming, Calling, Early, Connecting, Confirmed, Disconnected...
    }

    override fun onCallEnded(reason: VBotEndCallReason) {
        Log.d("VBot", "Cuộc gọi kết thúc, lý do: $reason (mã: ${reason.code})")
    }

    override fun onErrorCode(erCode: Int, message: String) {
        Log.e("VBot", "Lỗi SDK: $erCode - $message")
    }
}

client.addListener(listener)
```

### 3. Kết nối tài khoản

SDK áp dụng cơ chế **Lazy SIP** (không duy trì kết nối nền tiêu tốn pin, chỉ kết nối SIP khi có cuộc gọi):

```kotlin
client.connect(userToken, fcmPushToken) { displayName, error ->
    if (error == null) {
        Log.d("VBot", "Kết nối thành công: $displayName")
    } else {
        Log.e("VBot", "Lỗi kết nối: ${error.message}")
    }
}
```

### 4. Thực hiện cuộc gọi đi

```kotlin
val hotline = "024xxxxxxx" // Đầu số hotline (hoặc để trống nếu dùng hotline mặc định)
val targetPhone = "0987654321" // Số người nhận
val externalCallId = UUID.randomUUID().toString() // Mã định danh cuộc gọi phía bạn

client.startOutgoingCall(hotline, targetPhone, externalCallId)
```

### 5. Thực hiện cuộc gọi qua Call Session (Masking)

Nếu hệ thống của bạn yêu cầu bảo mật thông tin khách hàng (không để lộ số điện thoại thật ra phía client), Backend của bạn sẽ gọi Open API `/api/call-sessions` để lấy `sessionId`. Sau đó, truyền `sessionId` thay cho số điện thoại:

```kotlin
val sessionId = "session_id_tu_backend"
client.startOutgoingCall(hotline, sessionId, externalCallId)
```

### 6. Nhận cuộc gọi đến qua FCM Service

Trong `FirebaseMessagingService`:

```kotlin
class VBotFcmService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val dataMap = HashMap(message.data)
        
        val client = VBotClient(applicationContext)
        if (!client.isSetup()) {
            client.setup(VBotConfig(VBotEnvironment.PRODUCTION))
        }
        
        // Đẩy payload push notification vào SDK để kích hoạt màn hình / chuông cuộc gọi đến
        client.notificationCall(dataMap)
    }
}
```

### 6. Điều khiển trong cuộc gọi

```kotlin
// Trả lời cuộc gọi đến
client.answerCall()

// Từ chối cuộc gọi đến (isBusy: true báo máy bận)
client.declineIncomingCall(true)

// Mute / Bật mic
client.muteCall(true)  // true: Mute mic, false: Mở mic

// Bật / Tắt loa ngoài
client.onOffSpeaker(true) // true: Loa ngoài, false: Loa trong (earpiece)

// Gửi tín hiệu phím số DTMF
client.sendDTMF("1")

// Kết thúc cuộc gọi hiện tại
client.endCall()
```

---

## Xử lý sự cố thường gặp (Troubleshooting)

### 1. Không nhận được cuộc gọi đến khi tắt màn hình
- Đảm bảo thiết bị đã được cấp quyền hiển thị thông báo (`POST_NOTIFICATIONS` trên Android 13+).
- Kiểm tra thiết bị có đang bật chế độ tiết kiệm pin hoặc hạn chế chạy ngầm (Battery Optimization) đối với ứng dụng hay không.
- Kiểm tra Firebase Push Token đã được gửi thành công lên server qua `client.connect(...)`.

### 2. Lỗi quyền Microphone
- Ứng dụng phải xin quyền runtime `android.permission.RECORD_AUDIO` trước khi thực hiện hoặc nhận cuộc gọi.

### 3. Tương thích 16 KB Page Size trên Android 15
- Phiên bản **1.1.5** của VBot Phone SDK Android đã được tích hợp thư viện native biên dịch tương thích chuẩn 16 KB page size, sẵn sàng hoạt động trên Android 15+.

---

## Liên hệ & Hỗ trợ

- **Website:** [https://vbot.vn](https://vbot.vn)
