# Cloudflare Worker cho TorraStream QuickCode

Worker nhận `POST /v1/resolve`, tra mã cá nhân trong secret `USER_CONFIG_JSON` và chỉ trả API key của dịch vụ được yêu cầu. Không đặt API key trong GitHub, `plugins.json`, `wrangler.jsonc`, URL hoặc query string.

## Triển khai

1. Cài Node.js, đăng nhập Cloudflare bằng `npx wrangler login`.
2. Nên đổi mọi API key từng bị chia sẻ và tạo mã ngẫu nhiên dài tối thiểu 12 ký tự cho từng người.
3. Chạy `npx wrangler secret put USER_CONFIG_JSON` trong thư mục này, rồi dán JSON thật theo cấu trúc `users.example.json`.
4. Chạy `npx wrangler deploy`.
5. Trong cài đặt plugin **TorraStream QuickCode**, giữ URL đã điền sẵn, chọn TorBox hoặc RealDebrid và nhập mã cá nhân.

Kiểm tra không lộ khóa:

```text
GET https://torrastream-quickcode.tongbinhnguyen9090.workers.dev/health
```

Kết quả mong đợi: `{"ok":true,"version":2}`.

## Bảo mật vận hành

- Không dùng tên ngắn như `ti`, `hung`, `anh` làm mã; đó là mật khẩu và rất dễ đoán.
- Bật Cloudflare Rate Limiting/WAF cho `/v1/resolve`.
- Worker chỉ nhận POST JSON, trả `Cache-Control: no-store`, không ghi API key ra log.
- Thu hồi mã ngay khi thiết bị thất lạc; đổi API key nếu nghi ngờ bị lộ.
- API key vẫn phải được trả về thiết bị vì TorraStream cần dùng trực tiếp. Worker giúp tránh công khai khóa trong GitHub, nhưng không thể bảo vệ khóa khỏi người đang kiểm soát thiết bị đã kích hoạt.
