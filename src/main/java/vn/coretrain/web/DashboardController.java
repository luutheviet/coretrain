package vn.coretrain.web;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import vn.coretrain.domain.Lesson;
import vn.coretrain.domain.Module;
import vn.coretrain.domain.User;
import vn.coretrain.service.AccountService;
import vn.coretrain.service.CatalogService;

@Controller
public class DashboardController {

    private final AccountService accountService;
    private final CatalogService catalogService;

    public DashboardController(AccountService accountService, CatalogService catalogService) {
        this.accountService = accountService;
        this.catalogService = catalogService;
    }

    /** Query user đúng 1 lần/request; header fragment cần username/role/displayName ở mọi trang. */
    @ModelAttribute
    public void userInfo(Authentication authentication, Model model) {
        if (authentication == null) {
            return;
        }
        String username = authentication.getName();
        User user = accountService.findByUsername(username).orElse(null);
        model.addAttribute("currentUser", user);
        model.addAttribute("username", username);
        model.addAttribute("displayName", user != null ? user.getFullName() : username);
        model.addAttribute("role", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst().orElse(""));
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("cards", catalogService.moduleCards());
        Lesson lastViewed = Optional.ofNullable((User) model.getAttribute("currentUser"))
                .map(User::getLastViewedLessonId)
                .flatMap(catalogService::findLesson)
                .orElse(null);
        model.addAttribute("lastViewed", lastViewed);
        return "dashboard";
    }

    @GetMapping("/modules/{id}")
    public String moduleLessons(@PathVariable Long id, Model model) {
        Module module = catalogService.findModule(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có phân hệ này"));
        var lessons = catalogService.lessonsOf(id);
        model.addAttribute("module", module);
        model.addAttribute("lessons", lessons);
        model.addAttribute("lessonCount", lessons.size());
        return "module-lessons";
    }
}
