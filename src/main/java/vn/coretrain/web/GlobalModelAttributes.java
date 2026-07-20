package vn.coretrain.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.coretrain.domain.User;
import vn.coretrain.service.AccountService;

/**
 * Header fragment cần username/role/displayName ở mọi trang RENDER (dashboard, module-lessons,
 * lesson) — gom vào 1 chỗ, query user đúng 1 lần/request. Scope tường minh (assignableTypes)
 * chỉ tới các controller thật sự render Model: KHÔNG áp cho AuthController (trang trước đăng
 * nhập) và KHÔNG áp cho LessonMediaController (endpoint @ResponseBody serve pdf/video — advice
 * chạy trước mỗi request sẽ tốn 1 query DB thừa mỗi chunk Range khi tua video mà không dùng tới).
 */
@ControllerAdvice(assignableTypes = {DashboardController.class, LessonController.class})
public class GlobalModelAttributes {

    private final AccountService accountService;

    public GlobalModelAttributes(AccountService accountService) {
        this.accountService = accountService;
    }

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
}
