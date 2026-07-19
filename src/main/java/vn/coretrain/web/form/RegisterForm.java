package vn.coretrain.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form tự đăng ký — KHÔNG có trường vai: luôn là LEARNER (AD-4). */
public class RegisterForm {

    @NotBlank(message = "Tên đăng nhập không được trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập 3-50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Tên đăng nhập chỉ gồm chữ không dấu, số, dấu chấm, gạch")
    private String username;

    /* BCrypt (Security 7) từ chối mật khẩu >72 byte — giới hạn 64 ký tự để tiếng Việt có dấu vẫn an toàn. */
    @NotBlank(message = "Mật khẩu không được trống")
    @Size(min = 6, max = 64, message = "Mật khẩu 6-64 ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
