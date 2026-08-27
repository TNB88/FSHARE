# Ghi chú kỹ thuật TorraStream + QuickCode — 2026-08-28

## Thành phần phát hành

- `TorraStream.cs3`: bản v92 nguyên gốc từ `phisher98/cloudstream-extensions-phisher`; trong cùng gói có TorraStream (TMDB) và TorraStream-Anime.
- `TorraStreamQuickCode.cs3`: plugin companion v1 để nhập mã và ghi cấu hình Debrid dùng chung của hai nguồn.
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
- Plugin có sẵn URL Worker, timeout kết nối/đọc 10 giây, chỉ chấp nhận HTTPS và xóa ô mã sau khi áp dụng.

## SHA-256

- `TorraStream.cs3`: `e6f6a4d9910dbc4b775d7ddafb6c3a262e31942a430d853b5a41a392205450f8` (192127 byte).
- `TorraStreamQuickCode.cs3`: `05336eda5583c8d924e2f9265edf132befe792a76ff5c9c5e24822b13fe152d3` (12404 byte).

## Bảo mật và vận hành

- Các mã ngắn dễ bị đoán; nên chuyển sang mã ngẫu nhiên tối thiểu 12 ký tự và bật Rate Limiting/WAF cho `/v1/resolve`.
- Các TorBox key từng xuất hiện trong hội thoại nên được xoay vòng nếu nghi ngờ hội thoại hoặc thiết bị bị lộ.
- Không commit file chứa `USER_CONFIG_JSON` thật. Cập nhật secret bằng `wrangler secret put USER_CONFIG_JSON`.
- Plugin này dành cho CloudStream. Kodi có thể dùng cùng Worker nhưng cần add-on `.zip` riêng.
