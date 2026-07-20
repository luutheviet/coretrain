package vn.coretrain.service;

import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.coretrain.domain.Role;
import vn.coretrain.domain.User;
import vn.coretrain.repo.UserRepository;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean usernameTaken(String username) {
        return userRepository.existsByUsername(normalize(username));
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /** AD-4: tự đăng ký luôn gán LEARNER cứng — vai cao chỉ do seed cấp. */
    @Transactional
    public User registerLearner(String username, String rawPassword, String fullName) {
        return userRepository.save(new User(normalize(username),
                passwordEncoder.encode(rawPassword), Role.LEARNER, fullName.trim()));
    }

    /**
     * Ghi navigation state "bài xem gần nhất" (ngoại lệ AD-2 — không phải điểm) khi mở bài học.
     * Chỉ UPDATE khi giá trị đổi — tránh 1 write mỗi lần GET. User không tồn tại → no-op (GET
     * không được vỡ vì navigation state).
     */
    @Transactional
    public void updateLastViewedLesson(String username, Long lessonId) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (!lessonId.equals(user.getLastViewedLessonId())) {
                user.setLastViewedLessonId(lessonId);
                userRepository.save(user);
            }
        });
    }

    private String normalize(String username) {
        return username == null ? null : username.trim();
    }
}
