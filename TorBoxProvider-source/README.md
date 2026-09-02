# TorBox Việt

Plugin CloudStream dành cho TorBox:

- Danh mục và mô tả phim tiếng Việt từ TMDB.
- Poster dùng khung dọc 2:3 và ảnh TMDB 600x900 để hạn chế bị cắt mất nội dung.
- Phim lẻ, phim bộ và anime.
- Phim lẻ được trình bày thành một mục `Tập 1 • Bấm để chọn nguồn` để người dùng chủ động mở bảng nguồn.
- Danh sách nguồn sắp xếp 4K/1080p trước, hiện codec và dung lượng để dễ chọn.
- Nguồn phát trực tiếp do Torrentio trả về qua tài khoản TorBox của người dùng.
- Tự ưu tiên phụ đề tiếng Việt; kết hợp OpenSubtitles v3 và kho OpenSubtitles legacy tiếng Việt.
- Phụ đề Việt `.gz` được giải nén thành SRT bằng máy chủ loopback chỉ chạy trên điện thoại.
- Nhập mã nhanh dùng chung Worker và SharedPreferences với `TorraStreamQuickCode`.
- Có ô nhập API key trực tiếp làm phương án dự phòng.

## Bảo mật

Repository không chứa API key TorBox của người dùng và không chứa bảng mã kích hoạt. API key chỉ được lưu cục bộ trong SharedPreferences `TorraStream`, cùng định dạng với TorraStream v92.

Proxy phụ đề chỉ lắng nghe tại `127.0.0.1` trên một cổng ngẫu nhiên, không mở ra mạng LAN. Nguồn phụ đề loopback dành cho trình phát ngay trên điện thoại; khi casting, plugin chỉ gửi các URL phụ đề trực tiếp.

## Hỗ trợ

Bình Pro — SĐT & Zalo: 0907 657 980.
