package vn.coretrain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** BCrypt hash, không bao giờ plaintext. */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String fullName;

    /** Navigation state — ngoại lệ AD-2, dùng từ Story 1.4 (TIẾP TỤC HỌC). */
    @Column(name = "last_viewed_lesson_id")
    private Long lastViewedLessonId;

    protected User() {
    }

    public User(String username, String password, Role role, String fullName) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public Long getLastViewedLessonId() {
        return lastViewedLessonId;
    }

    public void setLastViewedLessonId(Long lastViewedLessonId) {
        this.lastViewedLessonId = lastViewedLessonId;
    }
}
