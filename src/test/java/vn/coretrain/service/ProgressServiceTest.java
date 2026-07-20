package vn.coretrain.service;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import vn.coretrain.domain.Lesson;
import vn.coretrain.domain.User;
import vn.coretrain.repo.LessonCompletionRepository;
import vn.coretrain.repo.LessonRepository;
import vn.coretrain.repo.ModuleRepository;
import vn.coretrain.repo.UserRepository;
import vn.coretrain.service.ProgressService.ModuleProgress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * KHÔNG dùng {@code @Transactional} class-level: markCompleted() insert qua
 * {@link LessonCompletionWriter} trong transaction REQUIRES_NEW riêng (cô lập race — xem class
 * đó) nên COMMIT THẬT, không rollback theo transaction bao ngoài của test. Dọn tường minh bằng
 * {@link AfterEach} thay vì dựa rollback ngầm.
 */
@SpringBootTest
@WithMockUser(roles = "LEARNER") // markCompleted() có @PreAuthorize("hasRole('LEARNER')") — AD-4
class ProgressServiceTest {

    @Autowired
    ProgressService progressService;

    @Autowired
    LessonCompletionRepository completionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ModuleRepository moduleRepository;

    @Autowired
    LessonRepository lessonRepository;

    private Long hocvienId() {
        return userRepository.findByUsername("hocvien").orElseThrow().getId();
    }

    private List<Lesson> cifLessons() {
        Long cifId = moduleRepository.findByCode("CIF").orElseThrow().getId();
        return lessonRepository.findByModuleIdOrderByCodeAsc(cifId);
    }

    @AfterEach
    void xoaCompletionCuaHocvien() {
        Long userId = hocvienId();
        completionRepository.findAll().stream()
                .filter(c -> c.getUser().getId().equals(userId))
                .forEach(c -> completionRepository.deleteById(c.getId()));
    }

    @Test
    void markCompletedLanDauGhiNhan() {
        Long userId = hocvienId();
        Long lessonId = cifLessons().get(0).getId();

        boolean first = progressService.markCompleted(userId, lessonId);

        assertThat(first).isTrue();
        assertThat(progressService.isCompleted(userId, lessonId)).isTrue();
        assertThat(completionRepository.countByUserId(userId)).isEqualTo(1);
    }

    @Test
    void markCompletedLaiKhongDemTrung() {
        Long userId = hocvienId();
        Long lessonId = cifLessons().get(0).getId();

        progressService.markCompleted(userId, lessonId);
        boolean second = progressService.markCompleted(userId, lessonId); // bấm lại

        assertThat(second).isFalse(); // đã học rồi
        assertThat(completionRepository.countByUserId(userId)).isEqualTo(1); // vẫn 1 dòng (AC #3)
    }

    @Test
    void tienDoPhanHeDungVaKhongVuot100() {
        Long userId = hocvienId();
        Long cifId = moduleRepository.findByCode("CIF").orElseThrow().getId();
        List<Lesson> lessons = cifLessons();

        // Học hết mọi bài CIF (gọi cả lần trùng để chắc chắn không đội quá 100)
        for (Lesson l : lessons) {
            progressService.markCompleted(userId, l.getId());
            progressService.markCompleted(userId, l.getId());
        }

        ModuleProgress cif = progressService.moduleProgress(userId).stream()
                .filter(p -> p.module().getId().equals(cifId))
                .findFirst().orElseThrow();

        assertThat(cif.done()).isEqualTo(cif.total());
        assertThat(cif.percent()).isEqualTo(100); // học hết → 100, không vượt
    }

    @Test
    void chuaHocPhanHeNaoPercent0() {
        Long userId = hocvienId();
        assertThat(progressService.moduleProgress(userId))
                .allSatisfy(p -> assertThat(p.percent()).isEqualTo(0));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerKhongDuocGoiMarkCompleted() {
        // AD-4 defense-in-depth: @PreAuthorize chặn ngay tại service, không chỉ ở route
        Long userId = hocvienId();
        Long lessonId = cifLessons().get(0).getId();

        assertThatThrownBy(() -> progressService.markCompleted(userId, lessonId))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(completionRepository.countByUserId(userId)).isEqualTo(0);
    }

    @Test
    @WithAnonymousUser
    void chuaDangNhapKhongDuocGoiMarkCompleted() {
        Long userId = hocvienId();
        Long lessonId = cifLessons().get(0).getId();

        assertThatThrownBy(() -> progressService.markCompleted(userId, lessonId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
