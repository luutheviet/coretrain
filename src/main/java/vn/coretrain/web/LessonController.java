package vn.coretrain.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.coretrain.domain.Lesson;
import vn.coretrain.domain.User;
import vn.coretrain.service.AccountService;
import vn.coretrain.service.CatalogService;
import vn.coretrain.service.ProgressService;

/**
 * Trang bài học 3 tab + đánh dấu đã học. Việc serve file nhị phân (pdf/video, AD-6) tách sang
 * {@link LessonMediaController} — controller đó KHÔNG cần currentUser/username/role nên không
 * gánh {@link GlobalModelAttributes} (advice chỉ scope tới controller thật sự render trang).
 * web→service→repo: qua CatalogService/ProgressService/AccountService, KHÔNG gọi repo trực tiếp.
 */
@Controller
public class LessonController {

    private final CatalogService catalogService;
    private final ProgressService progressService;
    private final AccountService accountService;

    public LessonController(CatalogService catalogService, ProgressService progressService,
                           AccountService accountService) {
        this.catalogService = catalogService;
        this.progressService = progressService;
        this.accountService = accountService;
    }

    @GetMapping("/lessons/{id}")
    public String lesson(@PathVariable Long id, Model model,
                         @ModelAttribute("currentUser") User currentUser) {
        Lesson lesson = requireLesson(id);
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
     * enforce ở SecurityConfig + @PreAuthorize trong ProgressService; MANAGER/EDITOR không
     * đánh dấu tiến độ học viên).
     */
    @PostMapping("/lessons/{id}/complete")
    public String markCompleted(@PathVariable Long id, @ModelAttribute("currentUser") User currentUser,
                                RedirectAttributes redirectAttributes) {
        requireLesson(id);
        if (currentUser == null) {
            // Authentication còn hạn nhưng dòng User đã mất (xóa/desync) — lỗi có kiểm soát, không NPE.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ");
        }
        boolean first = progressService.markCompleted(currentUser.getId(), id);
        redirectAttributes.addFlashAttribute("message", first
                ? "Đã ghi nhận — bài này tính vào tiến độ của bạn."
                : "Bài này bạn đã học rồi.");
        return "redirect:/lessons/" + id;
    }

    /** Bài lạ → 404 (error.html chung, không stacktrace) — dùng chung cho mọi endpoint của controller này. */
    private Lesson requireLesson(Long id) {
        return catalogService.findLesson(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có bài học này"));
    }
}
