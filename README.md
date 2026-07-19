# CORETRAIN — Webapp đào tạo nội bộ Core Banking

Phase 1 — demo dự thi đổi mới sáng tạo, tháng 8/2026.

## Stack (đã chốt trong Architecture Spine)

| Thành phần | Phiên bản | Lý do |
| --- | --- | --- |
| Java | 21 LTS | Dev đã thạo, LTS phổ biến |
| Spring Boot | 4.1.x (webmvc, thymeleaf, data-jpa, security, validation) | Dev đã thạo Spring Boot; bản đang được support |
| H2 Database | file mode (BOM quản lý) | Zero-install — 1 file jar chạy mọi nơi laptop/cloud |
| Maven | wrapper kèm repo | Build chuẩn, không cần cài |

Kiến trúc: monolith SSR (Thymeleaf), package theo lớp `vn.coretrain.{domain,repo,service,web}`.
Chi tiết: `../_bmad-output/planning-artifacts/architecture/architecture-daotao-2026-07-18/ARCHITECTURE-SPINE.md` (tính từ thư mục `coretrain/`).

## Cài đặt & chạy từ máy sạch

Yêu cầu duy nhất: JDK 21.

```bash
cd coretrain
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Mở http://localhost:8080 — seed tự chạy lần đầu (DB rỗng), không cần bước thủ công nào.

Đóng gói chạy độc lập:

```bash
./mvnw package
java -jar target/coretrain-0.0.1-SNAPSHOT.jar
```

Dữ liệu (H2 file + uploads) nằm trong `./data/` **tính theo thư mục đang đứng khi chạy lệnh** — luôn chạy từ thư mục `coretrain/` để dùng đúng DB cũ. Backup trước demo: copy nguyên thư mục `data/`.

> ⚠️ **Ngày demo:** copy project ra thư mục NGOÀI OneDrive rồi chạy — OneDrive sync file H2 `.mv.db` đang mở có nguy cơ lock/hỏng DB giữa buổi demo.

## Tài khoản demo (tạo tự động từ seed — KHÔNG tạo tay ngày thi)

| Username | Mật khẩu | Vai |
| --- | --- | --- |
| `hocvien` | `coretrain123` | Học viên (LEARNER) |
| `soanbai` | `coretrain123` | Người soạn/duyệt (EDITOR) |
| `quanly` | `coretrain123` | Quản lý — chỉ quan sát (MANAGER) |

Tài khoản tự đăng ký qua form luôn là Học viên; vai cao chỉ do seed cấp.

## Test

```bash
./mvnw test
```
