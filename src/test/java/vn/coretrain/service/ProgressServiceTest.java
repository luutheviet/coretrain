package vn.coretrain.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.coretrain.domain.Lesson;
import vn.coretrain.domain.User;
import vn.coretrain.repo.LessonCompletionRepository;
import vn.coretrain.repo.LessonRepository;
import vn.coretrain.repo.ModuleRepository;
import vn.coretrain.repo.UserRepository;
import vn.coretrain.service.ProgressService.ModuleProgress;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional // rollback sau mỗi test — completion không rò sang DB context chung
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
}
