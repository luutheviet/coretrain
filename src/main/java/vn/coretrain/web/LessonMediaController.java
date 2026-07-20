package vn.coretrain.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import vn.coretrain.domain.Lesson;
import vn.coretrain.service.CatalogService;
import vn.coretrain.service.FileStorageService;

/**
 * Serve file nội dung bài học (AD-6) — pdf/video, tách khỏi {@link LessonController} vì các
 * endpoint này chỉ trả body nhị phân, không render Model/currentUser nên KHÔNG cần
 * {@link GlobalModelAttributes} (advice không list controller này trong assignableTypes —
 * tránh 1 query user thừa mỗi chunk Range khi client tua video).
 * Dùng id-indirection (URL không chứa filename) — auth tự nhiên + không lộ path.
 */
@Controller
public class LessonMediaController {

    /** Chunk 1MB cho video Range — đủ để <video> tua mượt, không nạp cả file mỗi lần. */
    private static final long VIDEO_CHUNK = 1_000_000L;

    private final CatalogService catalogService;
    private final FileStorageService storage;

    public LessonMediaController(CatalogService catalogService, FileStorageService storage) {
        this.catalogService = catalogService;
        this.storage = storage;
    }

    /**
     * Tài liệu tab Tài liệu. PDF → nhúng inline; định dạng khác (docx, pptx...) → attachment (tải về)
     * vì browser không xem trực tiếp được. File null/mất → 404 (template đã ẩn/fallback).
     */
    @GetMapping("/lessons/{id}/pdf")
    @ResponseBody
    public ResponseEntity<Resource> document(@PathVariable Long id) {
        Lesson lesson = requireLesson(id);
        Path file = storage.resolve(lesson.getPdfPath())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có file nội dung"));
        boolean pdf = lesson.isPdfDocument();
        MediaType contentType = pdf ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;
        String disposition = pdf
                ? "inline"
                : "attachment; filename=\"" + safeFileName(lesson.getDocumentFileName()) + "\"";
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(new PathResource(file));
    }

    /** Loại ký tự phá header (xuống dòng, dấu ngoặc kép) khỏi tên file trong Content-Disposition. */
    private String safeFileName(String name) {
        return name == null ? "tai-lieu" : name.replaceAll("[\"\\r\\n]", "_");
    }

    /**
     * Video mp4 hỗ trợ HTTP Range (AD-6: tua được). Có header Range → 206 Partial Content + Content-Range
     * (ResourceRegionHttpMessageConverter tự ghi); không Range → 200 nguyên file. Accept-Ranges: bytes.
     * Range sai định dạng → coi như không có Range (RFC 7233 cho phép bỏ qua, trả nguyên file thay vì
     * lỗi); Range hợp lệ nhưng start vượt quá độ dài file → 416 kèm Content-Range: bytes * / length.
     */
    @GetMapping("/lessons/{id}/video")
    @ResponseBody
    public ResponseEntity<ResourceRegion> video(@PathVariable Long id,
                                                @RequestHeader HttpHeaders headers) {
        Path file = videoFile(id);
        Resource video = new PathResource(file);
        long length = fileLength(file);
        List<HttpRange> ranges;
        try {
            ranges = headers.getRange();
        } catch (IllegalArgumentException malformed) {
            ranges = List.of();
        }

        if (ranges.isEmpty()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(new ResourceRegion(video, 0, length));
        }
        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(length);
        if (start >= length) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + length)
                    .body(null);
        }
        long end = range.getRangeEnd(length);
        long count = Math.min(VIDEO_CHUNK, end - start + 1);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(new ResourceRegion(video, start, count));
    }

    /** Resolve video của bài; bài lạ → 404, cột path null hoặc file mất trên đĩa → 404 (không 500). */
    private Path videoFile(Long lessonId) {
        Lesson lesson = requireLesson(lessonId);
        return storage.resolve(lesson.getVideoPath())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có file nội dung"));
    }

    /** Bài lạ → 404 (error.html chung, không stacktrace). */
    private Lesson requireLesson(Long id) {
        return catalogService.findLesson(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có bài học này"));
    }

    private long fileLength(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
