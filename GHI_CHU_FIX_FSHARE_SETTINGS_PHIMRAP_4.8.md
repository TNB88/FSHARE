# Nhật ký kỹ thuật: sửa Fshare Settings trên PhimRap 4.8.0

Ngày kiểm tra: 2026-08-05

## Phạm vi

- Ứng dụng: PhimRap 4.8.0
- Package Android: `com.lagradost.cloudstream3`
- Version code: `68`
- Plugin lỗi: `FshareProvider` v57
- Thiết bị kiểm tra: Samsung SM-F946B qua ADB

## Triệu chứng

1. Fshare v57 cài và hiện trong danh sách extension.
2. Bấm biểu tượng bánh răng của Fshare làm PhimRap thoát/crash.
3. Khi ứng dụng tự mở lại, Cloudstream báo `Chế độ an toàn được bật`, nên không thể vào màn hình nhập tài khoản Fshare.

Safe Mode không phải là kiểm tra tài khoản Fshare. Đây là hậu quả của crash trước đó; Cloudstream ghi nhận lỗi cuối rồi tắt extension ở lần khởi động kế tiếp.

## Stack trace đã tái hiện trực tiếp

```text
FATAL EXCEPTION: main
android.content.res.Resources$NotFoundException: Resource ID #0x0
    at android.content.res.Resources.getLayout(Resources.java:1324)
    at com.anhdaden.FshareSettingsFragment.getLayout(FshareSettingsFragment.kt:30)
    at com.anhdaden.FshareSettingsFragment.onCreateView(FshareSettingsFragment.kt:55)
```

## Nguyên nhân gốc

`FshareSettingsFragment` lấy layout theo tên bằng:

```text
resources.getIdentifier("settings", "layout", "com.anhdaden")
```

Manifest của plugin khai báo `"requiresResources": true`, nhưng gói v57 sau lần rebuild chỉ còn:

```text
classes.dex
manifest.json
```

Các thành phần bắt buộc đã bị rơi khỏi gói:

- `resources.arsc`
- `res/layout/*.xml`
- `res/drawable/*.xml`

Vì không còn bảng resource và layout, `getIdentifier(...)` trả `0`. Lệnh `getLayout(0)` ném `Resources$NotFoundException`, làm cả ứng dụng crash và kích hoạt Safe Mode.

Lỗi này không nằm trong cơ chế lấy menu/phim/link Fshare Cine. `classes.dex` v57 và phần sửa Fshare Cine vẫn hoạt động; lỗi phát sinh ở bước đóng gói lại `.cs3` làm mất resource giao diện.

## Cách sửa v58

1. Giữ nguyên `classes.dex` của v57 để bảo toàn toàn bộ sửa chữa Fshare Cine và đăng nhập Fshare.
2. Khôi phục resource gốc tương thích của Fshare:
   - `resources.arsc`
   - `res/layout/add_link.xml`
   - `res/layout/check_account.xml`
   - `res/layout/list_link.xml`
   - `res/layout/loading.xml`
   - `res/layout/login.xml`
   - `res/layout/manager.xml`
   - `res/layout/provider.xml`
   - `res/layout/settings.xml`
   - các drawable `delete_icon`, `edit_icon`, `outline`, `telegram`
3. Tăng version trong `manifest.json` từ `57` lên `58`.
4. Đóng gói bằng Java `jar`, bảo đảm đường dẫn entry dùng dấu `/` chuẩn Android.
5. Cập nhật `plugins.json` với version, kích thước và SHA-256 mới.

## Kiểm tra bắt buộc trước khi phát hành

Chạy kiểm tra danh sách file trong `.cs3`. Kết quả phải có ít nhất:

```text
classes.dex
manifest.json
resources.arsc
res/layout/settings.xml
res/layout/login.xml
res/layout/check_account.xml
res/drawable/edit_icon.xml
res/drawable/outline.xml
```

Không phát hành nếu gói chỉ có `classes.dex` và `manifest.json`.

Không được có entry kiểu Windows như `res\layout\settings.xml`. Android AssetManager chỉ nhận đường dẫn ZIP dùng dấu `/`, ví dụ `res/layout/settings.xml`.

Sau khi đóng gói, luôn xác nhận ba giá trị trong `plugins.json` khớp file thật:

- `version`
- `fileSize`
- `fileHash` SHA-256

## Quy trình thử hồi quy trên thiết bị

1. Cài/cập nhật Fshare lên v58.
2. Đóng rồi mở lại PhimRap một lần để thoát trạng thái Safe Mode do crash cũ.
3. Vào Extension > Fshare > bánh răng.
4. Xác nhận màn hình Settings mở được, có Login và nhập được tài khoản/mật khẩu.
5. Kiểm tra thêm Add folder, List folder, List provider và Check account vì các màn hình này dùng các layout resource riêng.
6. Xem logcat, bảo đảm không còn `Resources$NotFoundException`, `Resource ID #0x0` hoặc `FATAL EXCEPTION` từ `FshareSettingsFragment`.
7. Mở Fshare Cine, kiểm tra menu, danh sách phim, chi tiết và link phát để chắc chắn phần v57 không bị lùi.

## Điều cần tránh về sau

- Không dùng quy trình decode/rebuild chỉ giữ phần smali khi plugin có `requiresResources: true`.
- Không lấy thành công của decompile `classes.dex` làm bằng chứng gói `.cs3` hoàn chỉnh; resource phải được kiểm tra độc lập.
- Không sửa Safe Mode bằng cách xóa dữ liệu ứng dụng. Việc đó làm mất repository, thiết lập và tài khoản nhưng không xử lý nguyên nhân crash.
- Khi bánh răng extension làm ứng dụng thoát, phải tái hiện với logcat sạch trước. Dòng đầu tiên của stack trace thường chỉ ra layout/resource hoặc class gây lỗi.

## Bản phát hành sửa lỗi

- Version: `58`
- File size: `176545` bytes
- SHA-256: `1752891f94fd1b6226d476898315745a573a2ebb14c19333cc0fdbf9da38f777`

## Kết quả xác nhận trực tiếp trên PhimRap 4.8.0

- Repository nhận đúng Fshare `v58`, hiển thị kích thước làm tròn `177 kB`.
- Đã đóng hộp thoại Safe Mode và khởi động sạch ứng dụng; không xóa dữ liệu PhimRap.
- Bấm bánh răng Fshare mở được `Fshare Settings` với đủ các mục Login, Add folder, List folder, List provider và Check account.
- Bấm Login mở được hộp thoại `Login Fshare`.
- Ba ô Username, Password và Mã đăng nhập VIP đều `enabled=true`, `focusable=true`.
- Đã đặt con trỏ vào Username và bàn phím Android hiện bình thường; không nhập hoặc ghi lại thông tin tài khoản người dùng.
- Logcat sạch sau khi mở Settings và Login: không còn `Resource ID #0x0`, `Resources$NotFoundException`, `FATAL EXCEPTION` hoặc crash từ `FshareSettingsFragment`.
