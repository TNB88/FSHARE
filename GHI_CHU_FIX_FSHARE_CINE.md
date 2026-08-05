# Ghi chú fix nguồn Fshare Cine v57

## Triệu chứng

- Nguồn `Fshare Cine` trong Cloudstream không tải được menu hoặc danh sách phim.
- Addon Kodi `plugin.video.thuviencine` vẫn tải được menu, danh sách, trang download và link Fshare.

## Nguyên nhân

`ThuVienCineProvider` của bản v56 cố định domain:

`https://thuviencine.tongbinhnguyen9090.workers.dev`

Domain Worker này hiện trả trang thông báo tạm ngưng hoạt động thay vì giao diện thư viện phim. Vì vậy các selector HTML của provider không tìm thấy `div[id^="post-"]`, trang chi tiết hoặc nút download.

Addon Kodi không cố định domain. Service của addon đọc `default_home_page` từ:

`https://thuviencine.github.io/plugin.video.thuviencine/config.json`

Domain đang hoạt động là:

`https://thuviencine.uk`

## Thay đổi

- Thay lớp `ThuVienCineProvider` bằng cơ chế domain động của provider gốc.
- Domain được đọc từ `https://raw.githubusercontent.com/Datj0000/domain/refs/heads/main/thuviencine.txt` và hiện trả `https://thuviencine.uk`.
- Dùng `CloudflareKiller` khi tải menu, tìm kiếm, trang chi tiết và trang download.
- Loại bỏ luồng chuẩn hóa URL chết vốn luôn ép URL về Worker đã ngừng phục vụ nội dung.
- Giữ nguyên các provider khác, giao diện Settings và bản sửa đăng nhập Fshare (`app_key`/`User-Agent`) của v56.
- Tăng version plugin từ 56 lên 57 và cập nhật `fileSize`/SHA-256 trong `plugins.json`.

## Kiểm tra

- Giải mã lại gói v57 bằng JADX thành công.
- Manifest hợp lệ và báo version 57.
- Domain động trả `https://thuviencine.uk`.
- Kiểm tra trực tiếp thành công: trang danh sách trả 24 phim; phim mẫu tải được trang chi tiết; `/download?id=9951` trả một link Fshare hợp lệ.
- Bytecode đăng nhập vẫn giữ `app_key` và `User-Agent` đã sửa ở v56.

Không kiểm thử đăng nhập/phát video bằng tài khoản thật vì không lưu hoặc sử dụng thông tin đăng nhập của người dùng trong quá trình build.
