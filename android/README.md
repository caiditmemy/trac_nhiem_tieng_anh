# NamBeDe Android (Sprint 1, sinh viên)

Đăng nhập, tổng quan (lớp, bài chưa làm, streak, điểm), danh sách bài tập, làm bài (`ui/exercises/TakeExercise.kt`), kết quả, tài khoản. Gọi gateway tại `http://10.0.2.2:8202/api/` (emulator); điện thoại thật: `-PnambedeApiUrl=http://<IP máy>:8202/api/`.

```bash
echo "sdk.dir=/home/box/Android/Sdk" > local.properties   # đã gitignore
./gradlew --no-daemon testDebugUnitTest verifyPaparazziDebug assembleDebug
./gradlew recordPaparazziDebug    # ghi lại ảnh chụp màn hình khi giao diện đổi
```

`ScreenshotTest` (Paparazzi) chụp login, home, home-dark, exercises, take-exercise, exercise-result, profile vào `app/src/test/snapshots/images`.
