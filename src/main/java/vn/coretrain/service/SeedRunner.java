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
import vn.coretrain.domain.Section;
import vn.coretrain.domain.User;
import vn.coretrain.repo.LessonRepository;
import vn.coretrain.repo.ModuleRepository;
import vn.coretrain.repo.SectionRepository;
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
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService storage;

    public SeedRunner(UserRepository userRepository, ModuleRepository moduleRepository,
                      SectionRepository sectionRepository, LessonRepository lessonRepository,
                      PasswordEncoder passwordEncoder, FileStorageService storage) {
        this.userRepository = userRepository;
        this.moduleRepository = moduleRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.passwordEncoder = passwordEncoder;
        this.storage = storage;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsers();
        seedModules();
        seedSections();
        seedLessons();
        seedLessonContent();
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

    /**
     * Chương (tầng giữa phân hệ và bài). 2 phân hệ sâu CIF + TIEN_VAY có nhiều chương (AD-9);
     * các phân hệ còn lại 1 chương để thẻ vẫn có bài. Mở rộng = thêm dòng.
     */
    private void seedSections() {
        seedSectionIfMissing("CIF", "CIF-C1", "Chương 1: Nhập môn CIF", 1);
        seedSectionIfMissing("CIF", "CIF-C2", "Chương 2: Nghiệp vụ tạo CIF", 2);
        seedSectionIfMissing("TIEN_VAY", "LN-C1", "Chương 1: Tổng quan Tiền vay", 1);
        seedSectionIfMissing("CHUYEN_TIEN", "FT-C1", "Chương 1: Tổng quan Chuyển tiền", 1);
        seedSectionIfMissing("TIEN_GUI", "DEP-C1", "Chương 1: Tổng quan Tiền gửi", 1);
        seedSectionIfMissing("TELLER", "TEL-C1", "Chương 1: Tổng quan Teller", 1);
        seedSectionIfMissing("GL", "GL-C1", "Chương 1: Tổng quan GL", 1);
        seedSectionIfMissing("THE", "CRD-C1", "Chương 1: Tổng quan Thẻ", 1);
    }

    private void seedSectionIfMissing(String moduleCode, String code, String title, int sortOrder) {
        if (sectionRepository.existsByCode(code)) {
            return;
        }
        Module module = moduleRepository.findByCode(moduleCode)
                .orElseThrow(() -> new IllegalStateException("Seed lỗi: không tồn tại phân hệ " + moduleCode));
        sectionRepository.save(new Section(module, code, title, sortOrder));
        log.info("Seed: tạo chương {} ({})", code, moduleCode);
    }

    /** Bài học tối thiểu title-only, giọng mockup — bộ dữ liệu sâu (3 tab + media) là Story 1.6. */
    private void seedLessons() {
        seedLessonIfMissing("CIF-C1", "CIF01", "Tổng quan phân hệ CIF — Quản lý hồ sơ khách hàng");
        seedLessonIfMissing("CIF-C1", "CIF02", "Quy trình tạo CIF Khách hàng cá nhân");
        seedLessonIfMissing("CIF-C2", "CIF03", "Quy trình tạo CIF Khách hàng tổ chức");
        seedLessonIfMissing("LN-C1", "LN01", "Tổng quan phân hệ Tiền vay — Vòng đời khoản vay");
        seedLessonIfMissing("FT-C1", "FT01", "Tổng quan phân hệ Chuyển tiền — Luồng MAKER-CHECKER");
        seedLessonIfMissing("DEP-C1", "DEP01", "Tổng quan phân hệ Tiền gửi — Sản phẩm và sổ tiết kiệm");
        seedLessonIfMissing("TEL-C1", "TEL01", "Tổng quan phân hệ Teller — Giao dịch quầy");
        seedLessonIfMissing("GL-C1", "GL01", "Tổng quan phân hệ GL — Sổ cái và bút toán");
        seedLessonIfMissing("CRD-C1", "CRD01", "Tổng quan phân hệ Thẻ — Phát hành và tra soát");
    }

    private void seedLessonIfMissing(String sectionCode, String code, String title) {
        if (lessonRepository.existsByCode(code)) {
            return;
        }
        // Fail-fast: typo mã chương trong seed là bug phải lộ ngay lúc khởi động, không nuốt im lặng
        Section section = sectionRepository.findByCode(sectionCode)
                .orElseThrow(() -> new IllegalStateException("Seed lỗi: không tồn tại chương " + sectionCode));
        lessonRepository.save(new Lesson(section, code, title));
        log.info("Seed: tạo bài học {} ({})", code, sectionCode);
    }

    /**
     * Nội dung đủ 3 tab cho CIF03 (bài ngôi sao mockup) — chứng minh trang bài học chạy end-to-end
     * ngay từ cài đặt sạch. Bộ dữ liệu sâu 7 phân hệ + media nặng là Story 1.6.
     * Idempotent: chỉ set khi cột còn trống (pdfPath null) — chạy lại không copy đè.
     */
    private void seedLessonContent() {
        lessonRepository.findByModuleIdOrderByCodeAsc(
                        moduleRepository.findByCode("CIF")
                                .orElseThrow(() -> new IllegalStateException("Seed lỗi: thiếu phân hệ CIF"))
                                .getId())
                .stream()
                .filter(l -> "CIF03".equals(l.getCode()))
                .findFirst()
                .filter(l -> l.getPdfPath() == null) // đã set rồi thì thôi (idempotent)
                .ifPresent(cif03 -> {
                    String pdf = storage.storeFromClasspath("seed/sample-cif03.pdf", "lessons/cif03.pdf");
                    String video = storage.storeFromClasspath("seed/sample-cif03.mp4", "lessons/cif03.mp4");
                    cif03.setPdfPath(pdf);
                    cif03.setVideoPath(video);
                    cif03.setMenuPath("Khách hàng → Tạo mới CIF → Khách hàng tổ chức");
                    cif03.setProcessContent("""
                            Bước 1. Vào phân hệ CIF → chọn "Tạo mới CIF" → loại "Khách hàng tổ chức".
                            Bước 2. Nhập thông tin pháp nhân: tên tổ chức, mã số thuế, giấy phép kinh doanh.
                            Bước 3. Nhập thông tin người đại diện và người liên hệ.
                            Bước 4. Đính kèm hồ sơ pháp lý theo checklist.
                            Bước 5. Kiểm tra trùng CIF theo mã số thuế trước khi lưu.
                            Bước 6. Lưu và chuyển duyệt (MAKER-CHECKER) — chờ phê duyệt để kích hoạt CIF.""");
                    lessonRepository.save(cif03);
                    log.info("Seed: gán nội dung 3 tab cho CIF03");
                });
    }
}
