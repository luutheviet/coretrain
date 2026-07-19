package vn.coretrain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.coretrain.domain.Lesson;
import vn.coretrain.domain.Module;
import vn.coretrain.domain.Role;
import vn.coretrain.domain.User;
import vn.coretrain.repo.LessonRepository;
import vn.coretrain.repo.ModuleRepository;
import vn.coretrain.repo.UserRepository;

/**
 * AD-9: một cơ chế seed duy nhất. Kiểm tra TỪNG bản ghi (không dựa count tổng)
 * nên tự vá được seed dở dang; @Transactional để seed là tất-cả-hoặc-không.
 * Story sau (bộ dữ liệu sâu 1.6, câu hỏi 2.6...) MỞ RỘNG runner này — không tạo runner thứ hai.
 */
@Component
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedRunner(UserRepository userRepository, ModuleRepository moduleRepository,
                      LessonRepository lessonRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsers();
        seedModules();
        seedLessons();
    }

    private void seedUsers() {
        seedUserIfMissing("hocvien", Role.LEARNER, "Nguyễn Văn Học");
        seedUserIfMissing("soanbai", Role.EDITOR, "Trần Thị Soạn");
        seedUserIfMissing("quanly", Role.MANAGER, "Lê Văn Quản");
    }

    private void seedUserIfMissing(String username, Role role, String fullName) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(new User(username, passwordEncoder.encode("coretrain123"), role, fullName));
        log.info("Seed: tạo tài khoản demo {} ({})", username, role);
    }

    /** 7 phân hệ theo mockup — đúng tên, icon, thứ tự. Thêm phân hệ mới = thêm dòng ở đây. */
    private void seedModules() {
        seedModuleIfMissing("TIEN_VAY", "Tiền vay", "💰", 1);
        seedModuleIfMissing("CHUYEN_TIEN", "Chuyển tiền", "💸", 2);
        seedModuleIfMissing("TIEN_GUI", "Tiền gửi", "🏦", 3);
        seedModuleIfMissing("CIF", "CIF", "👤", 4);
        seedModuleIfMissing("TELLER", "Teller", "🧾", 5);
        seedModuleIfMissing("GL", "GL", "📒", 6);
        seedModuleIfMissing("THE", "Thẻ", "💳", 7);
    }

    private void seedModuleIfMissing(String code, String name, String icon, int sortOrder) {
        if (moduleRepository.existsByCode(code)) {
            return;
        }
        moduleRepository.save(new Module(code, name, icon, sortOrder));
        log.info("Seed: tạo phân hệ {}", name);
    }

    /** Bài học tối thiểu title-only, giọng mockup — bộ dữ liệu sâu (3 tab + media) là Story 1.6. */
    private void seedLessons() {
        seedLessonIfMissing("CIF", "CIF01", "Tổng quan phân hệ CIF — Quản lý hồ sơ khách hàng");
        seedLessonIfMissing("CIF", "CIF02", "Quy trình tạo CIF Khách hàng cá nhân");
        seedLessonIfMissing("CIF", "CIF03", "Quy trình tạo CIF Khách hàng tổ chức");
        seedLessonIfMissing("TIEN_VAY", "LN01", "Tổng quan phân hệ Tiền vay — Vòng đời khoản vay");
        seedLessonIfMissing("CHUYEN_TIEN", "FT01", "Tổng quan phân hệ Chuyển tiền — Luồng MAKER-CHECKER");
        seedLessonIfMissing("TIEN_GUI", "DEP01", "Tổng quan phân hệ Tiền gửi — Sản phẩm và sổ tiết kiệm");
        seedLessonIfMissing("TELLER", "TEL01", "Tổng quan phân hệ Teller — Giao dịch quầy");
        seedLessonIfMissing("GL", "GL01", "Tổng quan phân hệ GL — Sổ cái và bút toán");
        seedLessonIfMissing("THE", "CRD01", "Tổng quan phân hệ Thẻ — Phát hành và tra soát");
    }

    private void seedLessonIfMissing(String moduleCode, String code, String title) {
        if (lessonRepository.existsByCode(code)) {
            return;
        }
        // Fail-fast: typo mã phân hệ trong seed là bug phải lộ ngay lúc khởi động, không nuốt im lặng
        Module module = moduleRepository.findByCode(moduleCode)
                .orElseThrow(() -> new IllegalStateException("Seed lỗi: không tồn tại phân hệ " + moduleCode));
        lessonRepository.save(new Lesson(module, code, title));
        log.info("Seed: tạo bài học {} ({})", code, moduleCode);
    }
}
