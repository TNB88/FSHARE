# Ghi chú kỹ thuật — TorBox Việt cho CloudStream

Ngày hoàn thiện: 2026-09-03

Tác giả hiển thị: Bình Pro

Hỗ trợ: SĐT & Zalo 0907 657 980

## 1. Thành phần được thêm mới

- `TorBoxProvider.cs3`: gói cài trực tiếp trong CloudStream.
- `TorBoxProvider-v3.cs3`: gói phát hành hiện tại, dùng tên theo phiên bản để tránh cache GitHub Raw.
- `TorBoxProvider-source/`: toàn bộ mã nguồn để sửa và build lại.
- Một mục `TorBox Việt` trong `plugins.json` của repository FSHARE.

Plugin cũ `TorraStream.cs3` và `TorraStreamQuickCode.cs3` vẫn được giữ nguyên. Bản mới được thêm song song, không xóa và không thay nội dung của hai plugin cũ.

## 2. Luồng hoạt động

1. Trang chủ, tìm kiếm và trang chi tiết lấy dữ liệu từ TMDB với `language=vi-VN`.
2. Nếu TMDB chưa có phần mô tả tiếng Việt, plugin tự lấy mô tả tiếng Anh làm dự phòng.
3. Khi người dùng bấm phát, plugin gửi mã IMDb cùng mùa/tập sang Torrentio.
4. Torrentio dùng cấu hình `TorBox=<API key>` và chỉ những mục có URL phát trực tiếp mới được đưa vào danh sách nguồn.
5. Nguồn được sắp xếp theo độ phân giải rồi dung lượng: 4K trước, sau đó 1080p, 720p và nguồn không rõ chất lượng.
6. Tên nguồn hiển thị theo dạng `TorBox • 4K • HDR • HEVC • 18 GB`, kèm tên release ở dòng dưới.
7. Phụ đề được lấy từ addon OpenSubtitles Stremio và OpenSubtitles legacy tiếng Việt.
8. Phụ đề legacy `.gz` được giải nén theo yêu cầu qua HTTP loopback `127.0.0.1`; không mở cổng ra mạng LAN.
9. Plugin đặt ngôn ngữ tự chọn phụ đề của CloudStream thành `vi` khi tùy chọn `Tự động ưu tiên phụ đề tiếng Việt` được bật. Khi tắt, lựa chọn trước đó được khôi phục.
10. Trang chủ dùng `HomePageList(..., isHorizontalImages=false)` và ảnh `w600_and_h900_bestv2`, để CloudStream tạo thẻ poster dọc thay vì cắt poster vào khung ngang.
11. Phim lẻ trả về `TvSeriesLoadResponse` có đúng một tập tên `Tập 1 • Bấm để chọn nguồn`; đây là cách plugin tạo điểm bấm chọn nguồn mà không sửa giao diện lõi CloudStream.

## 3. Nhập mã nhanh và nơi lưu API key

Màn cài đặt của `TorBox Việt` có hai cách cấu hình:

- Nhập mã nhanh: POST tới Worker hiện có tại đường dẫn `/v1/resolve`, với provider cố định là `torbox`.
- Nhập API key TorBox trực tiếp: phương án dự phòng khi không dùng mã nhanh.

Cả hai cách đều lưu vào SharedPreferences của CloudStream:

- Tên file: `TorraStream`
- `debrid_provider=TorBox`
- `debrid_key=<API key của người dùng>`

Nhờ dùng đúng tên và khóa như TorraStream v92, một lần kích hoạt TorBox có thể dùng chung cho:

- TorBox Việt
- TorraStream
- TorraStream-Anime
- TorraStream QuickCode

API key không xuất hiện trong repository, `plugins.json`, gói `.cs3` hay ghi chú kỹ thuật. API key chỉ được nhận lúc chạy và lưu cục bộ trên thiết bị. Ô nhập dùng kiểu mật khẩu, trạng thái sau khi lưu chỉ báo “đã cấu hình” chứ không đọc ngược key ra màn hình.

## 4. Worker QuickCode dùng chung

Worker đang dùng:

`https://torrastream-quickcode.tongbinhnguyen9090.workers.dev`

Endpoint plugin gọi:

`POST /v1/resolve`

Nội dung yêu cầu:

```json
{
  "code": "ma-ca-nhan",
  "provider": "torbox"
}
```

Phản hồi hợp lệ cần có `provider=TorBox` và trường `key` dài tối thiểu 8 ký tự. Danh sách mã và API key thật vẫn nằm trong secret `USER_CONFIG_JSON` của Cloudflare Worker; tuyệt đối không chép secret này vào GitHub.

## 5. Cấu trúc mã nguồn

- `TorBoxPlugin.kt`: đăng ký provider và mở màn cài đặt.
- `TorBoxConfig.kt`: đọc, lưu, xóa cấu hình TorBox dùng chung.
- `TorBoxProvider.kt`: TMDB, danh mục, tìm kiếm, chi tiết, tập phim, Torrentio, nguồn phát và phụ đề.
- `VietnameseSubtitleProxy.kt`: giải nén OpenSubtitles `.gz` và phục vụ SRT tại loopback trên điện thoại.
- `TorBoxSettingsFragment.kt`: nhập mã nhanh, nhập API key, tùy chọn phụ đề Việt và xóa cấu hình.
- `torbox_settings.xml`: giao diện cài đặt.
- `build.gradle.kts`: metadata và phụ thuộc của plugin.

## 6. Cách build lại

Dùng project mẫu plugin chính thức của CloudStream. Chép thư mục `TorBoxProvider-source` thành module `TorBoxProvider`, rồi chạy:

```powershell
.\gradlew.bat TorBoxProvider:make
```

Gói tạo ra tại:

`TorBoxProvider/build/TorBoxProvider.cs3`

Nếu CloudStream pre-release được biên dịch bằng Kotlin mới hơn, phiên bản `kotlin-gradle-plugin` của project build phải tương thích. Lần build ngày 2026-09-03 dùng Kotlin 2.3.0 vì thư viện CloudStream hiện tại có metadata Kotlin 2.3.0.

Sau mỗi lần build:

1. Tăng `version` trong `TorBoxProvider-source/build.gradle.kts`.
2. Chép `.cs3` mới ra gốc repository.
3. Cập nhật `version`, `fileSize` và `fileHash` trong `plugins.json`.
   Khi thay gói ngay sau lúc phát hành, nên dùng tên file theo phiên bản như `TorBoxProvider-v3.cs3`; GitHub Raw có thể bỏ qua query string và vẫn trả gói cũ từ cache.
4. Kiểm tra `plugins.json` đọc được bằng trình phân tích JSON.
5. Mở `.cs3` như ZIP và kiểm tra có `manifest.json`, `classes.dex`, `resources.arsc` và layout cài đặt.
6. Không commit bất kỳ API key người dùng hay nội dung secret Worker nào.

## 7. Kiểm tra trước khi phát hành bản này

- Build Gradle hoàn tất với `BUILD SUCCESSFUL`.
- Manifest gói: `pluginClassName=com.tnb88.torboxprovider.TorBoxPlugin`, `name=TorBoxProvider`, `version=3`.
- Phản hồi “TB error / Invalid TorBox ApiKey/Token” của Torrentio được lọc bỏ, để CloudStream báo kiểm tra kích hoạt thay vì hiện một nguồn giả không phát được.
- Thử OpenSubtitles legacy với một phim mẫu trả 9 phụ đề Việt; file `.gz` giải nén thành SRT hợp lệ.
- Manifest CloudStream cho phép `usesCleartextTraffic=true`, cần thiết để trình phát đọc SRT từ loopback `http://127.0.0.1`.
- Gói chứa đầy đủ `classes.dex`, tài nguyên và layout cài đặt.
- TMDB thử nghiệm trả danh sách và tiêu đề tiếng Việt.
- Health check Worker trả `ok=true`, `version=2`.
- SHA-256 và dung lượng trong `plugins.json` khớp với file `.cs3`.
- Gói v3 có dung lượng `41659` byte và SHA-256 `726109fdb7333350377ddcc76b50fedb3ef78f8a72647834fab4db2a081f235b`.

## 8. Lưu ý vận hành

- Plugin cần tài khoản TorBox hợp lệ và API key còn hiệu lực.
- Nguồn phim phụ thuộc vào TMDB, Torrentio, TorBox và addon phụ đề; một phim chưa có IMDb hoặc chưa có nguồn TorBox trực tiếp có thể không phát được.
- Khi đổi API key trong TorBox Việt, TorraStream cũng nhận cấu hình mới vì hai plugin cố ý dùng chung SharedPreferences.
- Nút xóa chỉ xóa cấu hình nếu provider hiện tại là TorBox, để tránh vô tình xóa cấu hình Real-Debrid của người dùng.
- Chỉ dùng plugin với nội dung mà người dùng có quyền truy cập.
