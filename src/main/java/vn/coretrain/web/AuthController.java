package vn.coretrain.web;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.coretrain.service.AccountService;
import vn.coretrain.web.form.RegisterForm;

@Controller
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Authentication authentication, Model model) {
        if (isAuthenticated(authentication)) {
            return "redirect:/";
        }
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult binding,
                           RedirectAttributes redirect) {
        // Báo trùng ngay cả khi trường khác đang lỗi — chỉ cần username không có lỗi riêng
        if (!binding.hasFieldErrors("username") && accountService.usernameTaken(form.getUsername())) {
            binding.rejectValue("username", "taken", "Tên đăng nhập đã tồn tại");
        }
        if (binding.hasErrors()) {
            return "register";
        }
        try {
            accountService.registerLearner(form.getUsername(), form.getPassword(), form.getFullName());
        } catch (DataIntegrityViolationException e) {
            // Race 2 request cùng username vượt qua check — trả lỗi form thay vì 500
            binding.rejectValue("username", "taken", "Tên đăng nhập đã tồn tại");
            return "register";
        }
        redirect.addFlashAttribute("message", "Đăng ký thành công. Mời đăng nhập.");
        return "redirect:/login";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
