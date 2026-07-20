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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.coretrain.domain.Lesson;
import vn.coretrain.domain.User;
import vn.coretrain.service.AccountService;
import vn.coretrain.service.CatalogService;
import vn.coretrain.service.FileStorageService;
import vn.coretrain.service.ProgressService;

/**
 * Trang bài học 3 tab + serve file nội dung (AD-6). Dùng id-indirection (URL không chứa filename)
 * — auth tự nhiên + không lộ path. web→service→repo: qua CatalogService/FileStorageService,
 * KHÔNG gọi repo trực tiếp (paradigm spine).
 */
@Controller
public class LessonController {

    /** Chunk 1MB cho video Range — đủ để <video> tua mượt, không nạp cả file mỗi lần. */
    private static final long VIDEO_CHUNK = 1_000_000L;

    private final CatalogService catalogService;
    private final FileStorageService storage;
    private final ProgressService progressService;
    private final AccountService accountService;

    public LessonController(CatalogService catalogService, FileStorageService storage,
                           ProgressService progressService, AccountService accountService) {
        this.catalogService = catalogService;
        this.storage = storage;
        this.progressService = progressService;
        this.accountService = accountService;
    }

    @GetMapping("/lessons/{id}")
    public String lesson(@PathVariable Long id, Model model,
                         @ModelAttribute("currentUser") User currentUser) {
        Lesson lesson = catalogService.findLesson(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có bài học này"));
        model.addAttribute("lesson", lesson);
        model.addAttribute("module", lesson.getModule());
        String breadcrumb = (lesson.getMenuPath() != null && !lesson.getMenuPath().isBlank())
                ? lesson.getMenuPath()
                : lesson.getModule().getName() + " → " + lesson.getSection().getTitle();
        model.addAttribute("breadcrumb", breadcrumb);
        // Navigation state "bài xem gần nhất" (ngoại lệ AD-2) — ghi khi mở bài, chỉ khi đổi giá trị.
        if (currentUser != null) {
            accountService.updateLastViewedLesson(currentUser.getUsername(), id);
            model.addAttribute("completed", progressService.isCompleted(currentUser.getId(), id));
        }
        return "lesson";
    }

    /**
     * Đánh dấu bài đã học (AC #1) — cơ chế duy nhất, thủ công. Idempotent (AC #3: xem/bấm lại
     * không đếm trùng). PRG: flash + redirect (F5 không gửi lại POST). Chỉ LEARNER (AD-4 —
     * enforce ở SecurityConfig; MANAGER/EDITOR không đánh dấu tiến độ học viên).
     */
    @PostMapping("/lessons/{id}/complete")
    public String markCompleted(@PathVariable Long id, @ModelAttribute("currentUser") User currentUser,
                                RedirectAttributes redirectAttributes) {
        catalogService.findLesson(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có bài học này"));
        boolean first = progressService.markCompleted(currentUser.getId(), id);
        redirectAttributes.addFlashAttribute("message", first
                ? "Đã ghi nhận — bài này tính vào tiến độ của bạn."
                : "Bài này bạn đã học rồi.");
        return "redirect:/lessons/" + id;
    }

    /**
     * Tài liệu tab Tài liệu. PDF → nhúng inline; định dạng khác (docx, pptx...) → attachment (tải về)
     * vì browser không xem trực tiếp được. File null/mất → 404 (template đã ẩn/fallback).
     */
    @GetMapping("/lessons/{id}/pdf")
    @ResponseBody
    public ResponseEntity<Resource> document(@PathVariable Long id) {
        Lesson lesson = catalogService.findLesson(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có bài học này"));
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
     */
    @GetMapping("/lessons/{id}/video")
    @ResponseBody
    public ResponseEntity<ResourceRegion> video(@PathVariable Long id,
                                                @RequestHeader HttpHeaders headers) {
        Path file = videoFile(id);
        Resource video = new PathResource(file);
        long length = fileLength(file);
        List<HttpRange> ranges = headers.getRange();

        if (ranges.isEmpty()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(new ResourceRegion(video, 0, length));
        }
        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(length);
        long end = range.getRangeEnd(length);
        long count = Math.min(VIDEO_CHUNK, end - start + 1);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(new ResourceRegion(video, start, count));
    }

    /** Resolve video của bài; bài lạ → 404, cột path null hoặc file mất trên đĩa → 404 (không 500). */
    private Path videoFile(Long lessonId) {
        Lesson lesson = catalogService.findLesson(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có bài học này"));
        return storage.resolve(lesson.getVideoPath())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có file nội dung"));
    }

    private long fileLength(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
