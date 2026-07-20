package vn.coretrain.web;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.stream.Collectors;
import vn.coretrain.domain.Lesson;
import vn.coretrain.domain.Module;
import vn.coretrain.domain.User;
import vn.coretrain.service.CatalogService;
import vn.coretrain.service.ProgressService;

@Controller
public class DashboardController {

    private final CatalogService catalogService;
    private final ProgressService progressService;

    public DashboardController(CatalogService catalogService, ProgressService progressService) {
        this.catalogService = catalogService;
        this.progressService = progressService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        User currentUser = (User) model.getAttribute("currentUser");
        model.addAttribute("cards", catalogService.moduleCards());
        Lesson lastViewed = Optional.ofNullable(currentUser)
                .map(User::getLastViewedLessonId)
                .flatMap(catalogService::findLesson)
                .orElse(null);
        model.addAttribute("lastViewed", lastViewed);
        // % tiến độ theo phân hệ (Story 1.4) — map moduleId → percent, chỉ hiện khi > 0 trên thẻ.
        Map<Long, Integer> progress = currentUser == null
                ? Map.of()
                : progressService.moduleProgress(currentUser.getId()).stream()
                        .collect(Collectors.toMap(p -> p.module().getId(), ProgressService.ModuleProgress::percent));
        model.addAttribute("progress", progress);
        return "dashboard";
    }

    @GetMapping("/modules/{id}")
    public String moduleLessons(@PathVariable Long id, Model model) {
        Module module = catalogService.findModule(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có phân hệ này"));
        var sections = catalogService.sectionsWithLessons(id);
        int lessonCount = sections.stream().mapToInt(g -> g.lessons().size()).sum();
        model.addAttribute("module", module);
        model.addAttribute("sections", sections);
        model.addAttribute("lessonCount", lessonCount);
        return "module-lessons";
    }
}
