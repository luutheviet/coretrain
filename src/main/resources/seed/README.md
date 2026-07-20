# Dữ liệu seed mẫu

Chỗ chứa dữ liệu seed mẫu (bài học, câu hỏi, media, template Excel). `SeedRunner` copy media từ đây
sang `app.storage.dir` lúc khởi động (idempotent) rồi gán path vào entity.

## File hiện có

- `sample-cif03.pdf` — tài liệu mẫu cho bài CIF03 (tab Tài liệu).
- `sample-cif03.mp4` — video mẫu cho bài CIF03 (tab Video). **Placeholder** dựng không cần ffmpeg,
  đủ để chứng minh cơ chế phát + HTTP Range. Story 1.6 thay bằng clip thật.

## Chuẩn soạn nội dung (AC #3, Story 1.3)

- **Video: mp4 codec H.264** (nén nhẹ, ≤50MB/file — Story 1.6) để `<video>` phát mượt trên laptop
  demo không cần internet. Codec lạ → trình duyệt không phát; luôn kèm nút "Tải file về" (đã có sẵn).
- **Tài liệu: PDF** nhúng inline; file hỏng vẫn có nút "Mở ngoài / Tải file", không bao giờ trắng trang.
