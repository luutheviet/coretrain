package vn.coretrain.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * AD-4: phân quyền server-side. Security 7 — chỉ lambda DSL;
 * @EnableMethodSecurity mở @PreAuthorize cho tầng service ở các story sau.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // PDF nhúng inline (tab Tài liệu, Story 1.3) cần frame cùng-origin;
                // mặc định Security là DENY → <object>/<iframe> bị ERR_BLOCKED_BY_RESPONSE.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/login", "/register", "/error").permitAll()
                        // AD-4: đánh dấu đã học là POST nghiệp vụ của học viên — MANAGER (chỉ đọc)
                        // và EDITOR không được. Enforce server-side (ẩn nút trên view không đủ).
                        .requestMatchers(HttpMethod.POST, "/lessons/*/complete").hasRole("LEARNER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/")
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
