# Ghi chú kỹ thuật TorraStream + QuickCode — cập nhật 2026-09-03

## Thành phần phát hành

- `TorraStream.cs3`: bản v94; trong cùng gói có TorraStream (TMDB) và TorraStream-Anime.
- `TorraStreamQuickCode.cs3`: plugin companion v2 để nhập mã và ghi cấu hình Debrid dùng chung của hai nguồn.
- Worker: `https://torrastream-quickcode.tongbinhnguyen9090.workers.dev`
- Cloudflare Worker Version ID: `b7f473b8-fdb2-4709-bacb-2b5856271190`.

## Cơ chế cấu hình

TorraStream v92 đọc SharedPreferences tên `TorraStream` với hai khóa:

- `debrid_provider`: giá trị `TorBox` hoặc `RealDebrid`.
- `debrid_key`: API key của dịch vụ đã chọn.

QuickCode gửi `POST /v1/resolve` qua HTTPS, nhận đúng một key rồi ghi hai khóa trên vào bộ nhớ cục bộ CloudStream. API key không nằm trong `.cs3`, mã nguồn, manifest repository hay GitHub Actions.

Worker đọc cấu hình từ Cloudflare Worker Secret `USER_CONFIG_JSON`. Không dùng KV công khai. Real-Debrid hiện chưa được cấu hình vì dữ liệu đầu vào chỉ là placeholder; ba mã hiện tại chỉ dùng được với TorBox.

## Kiểm thử

- `GET /health`: `ok=true`, `version=2`.
- Ba mã TorBox hiện tại: trả provider `TorBox`, key đúng định dạng UUID; giá trị key không được in vào log kiểm thử.
- Mã không tồn tại: HTTP 404.
- `TorraStreamQuickCode.cs3` giải nén hợp lệ, manifest nhận class `com.tnb88.torrastreamquickcode.TorraStreamQuickCodePlugin` và JADX đọc được `classes.dex`.
- Plugin v2 ẩn hoàn toàn URL Worker khỏi giao diện, timeout kết nối/đọc 10 giây và xóa ô mã sau khi áp dụng.
- Khi mã được áp dụng thành công, trường API key trong cài đặt TorraStream được ẩn để người dùng khác không đọc hoặc sao chép được.
- Giao diện hiển thị thông tin hỗ trợ: Bình Pro — Zalo 0907 657 980.
- Kiểm thử ADB trên Phim Rạp tại `192.168.1.21`: QuickCode v2 cài thành công; giao diện không còn ô URL Worker; mã `ti` báo đã cấu hình TorBox cho TorraStream và TorraStream-Anime; mở lại TorraStream Settings không còn hiển thị `Enter API Key / URL`/`debrid_key_input`; không ghi nhận crash AndroidRuntime.

### Sửa tương thích TorraStream-Anime v94

Trên PhimRạp 4.8.0, v93 lỗi khi mở chi tiết anime với hai dấu vết ADB:

```text
NullPointerException at TorraStreamAnime.kt:160
NoSuchMethodError: AniListApi$Recommendation.getMediaRecommendation()
```

Nguyên nhân:

- `data.format!!` làm crash khi AniList không trả trường `format`.
- Plugin được build với CloudStream pre-release có kiểu trả về của `getMediaRecommendation()` khác kiểu trong APK PhimRạp 4.8.0. Cùng tên hàm nhưng descriptor JVM khác nên Android coi là không tồn tại.

Cách sửa v94:

- Đổi sang `val mediaFormat = data.format.orEmpty()` và dùng biến này để phân biệt anime movie/series.
- Không đọc model `AniListApi.Recommendation` nội bộ của app trong trang chi tiết. Mục đề xuất là tùy chọn nên được bỏ khỏi query và response; tìm kiếm, tập, nguồn phát, TorBox và phụ đề không bị ảnh hưởng.
- Giữ file v93 và mã nguồn v93 để rollback; phát hành thêm `TorraStream-v94.cs3` và `TorraStream-v94-source/`.
- `plugins.json` tăng version lên 94 và trỏ URL versioned mới để tránh cache GitHub Raw.

## SHA-256

- `TorraStream.cs3` v94: `3adefcbf330410964aef603e4531d56e2a486cf39d54f0ba6b7abc2b64d8406a` (198705 byte).
- `TorraStreamQuickCode.cs3` v2: `ad37ca941849f74f847173460858cc01383d637d81b8fce74da6fe52dd99f97e` (13547 byte).

## Bảo mật và vận hành

- Các mã ngắn dễ bị đoán; nên chuyển sang mã ngẫu nhiên tối thiểu 12 ký tự và bật Rate Limiting/WAF cho `/v1/resolve`.
- Các TorBox key từng xuất hiện trong hội thoại nên được xoay vòng nếu nghi ngờ hội thoại hoặc thiết bị bị lộ.
- Không commit file chứa `USER_CONFIG_JSON` thật. Cập nhật secret bằng `wrangler secret put USER_CONFIG_JSON`.
- Plugin này dành cho CloudStream. Kodi có thể dùng cùng Worker nhưng cần add-on `.zip` riêng.
