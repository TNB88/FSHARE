# TorraStream QuickCode

Plugin companion mã nguồn mở cho TorraStream v92. Plugin nhập mã qua Cloudflare Worker rồi ghi hai khóa SharedPreferences mà TorraStream và TorraStream-Anime dùng chung:

- `debrid_provider`: `TorBox` hoặc `RealDebrid`
- `debrid_key`: API key nhận qua HTTPS

Plugin gốc không bị sửa, nhờ đó có thể thay phiên bản TorraStream mới mà không mất chức năng nhập mã nhanh.

URL Worker sản xuất được nhúng trong mã và không hiện trên màn hình cài đặt. Người dùng chỉ cần chọn TorBox/RealDebrid, nhập mã cá nhân và khởi động lại CloudStream. Sau khi áp dụng mã, plugin đánh dấu cấu hình được quản lý và tự ẩn trường API key trong màn hình cài đặt TorraStream.

Mã nguồn Kotlin và layout nằm trong thư mục này. Worker nằm tại `cloudflare/torrastream-quickcode`. Plugin `.cs3` này chỉ dành cho CloudStream; Kodi cần add-on Python riêng nhưng có thể dùng chung Worker.
