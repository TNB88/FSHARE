# Ghi chú kỹ thuật TorraStream v93

## Mục tiêu bản tùy chỉnh

- Giữ nguyên phim lẻ dạng `MovieLoadResponse`; không ép phim lẻ thành tập.
- Lấy tên phim, mô tả, thể loại và thông tin tập từ TMDB bằng `language=vi-VN`.
- Dùng genre id `16` để nhận diện hoạt hình, vì tên thể loại đã được dịch sang tiếng Việt.
- Ưu tiên phụ đề Việt từ OpenSubtitles legacy, sau đó mới bổ sung phụ đề từ `opensubtitles-v3.strem.io`.
- Phụ đề legacy dạng `.gz` được giải nén qua HTTP loopback `127.0.0.1` thành SRT cho trình phát nội bộ. Khi cast thì bỏ qua loopback vì TV không truy cập được localhost của điện thoại.
- Khi plugin nạp, `subs_auto_select` của CloudStream được đặt thành JSON string `"vi"` để tự chọn tiếng Việt nếu có.

## Phát hành

- Internal name: `TorraStream`
- Version: `93`
- Tệp chống cache: `TorraStream-v93.cs3`
- Mã nguồn để sửa/build lại: thư mục `TorraStream-v93-source/`
- SHA-256: `d7c7416d57381beb62e0c2fca56ff5da42931d1600a92119e84736631e592a5f`
- Kích thước: `199465` byte

## Kiểm tra đã thực hiện

- Gradle `:TorraStream:make`: thành công.
- `manifest.json` trong gói: `com.phisher98.TorraStreamProvider`, version `93`.
- Thử TMDB với phim `The Matrix`: trả về tên `Ma Trận`, mô tả và thể loại tiếng Việt.
- Thử OpenSubtitles legacy với IMDb `tt0133093`: HTTP 200 và có kết quả phụ đề Việt.
