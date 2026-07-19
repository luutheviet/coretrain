package vn.coretrain.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.coretrain.domain.Lesson;
import vn.coretrain.repo.LessonRepository;
import vn.coretrain.repo.ModuleRepository;
import vn.coretrain.service.CatalogService.ModuleCard;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional // rollback sau mỗi test — không rò dữ liệu (CIF99) sang DB context chung
class CatalogServiceTest {

    @Autowired
    CatalogService catalogService;

    @Autowired
    ModuleRepository moduleRepository;

    @Autowired
    LessonRepository lessonRepository;

    @Test
    void seedDu7PhanHeDungThuTuMockup() {
        List<ModuleCard> cards = catalogService.moduleCards();
        assertThat(cards).extracting(c -> c.module().getName())
                .containsExactly("Tiền vay", "Chuyển tiền", "Tiền gửi", "CIF", "Teller", "GL", "Thẻ");
    }

    @Test
    void moiPhanHeSeedItNhat1Bai() {
        assertThat(catalogService.moduleCards())
                .allSatisfy(c -> assertThat(c.lessonCount()).isGreaterThanOrEqualTo(1));
    }

    @Test
    void soBaiTrenTheKhopThucTe() {
        var cif = moduleRepository.findByCode("CIF").orElseThrow();
        long before = catalogService.moduleCards().stream()
                .filter(c -> c.module().getId().equals(cif.getId()))
                .findFirst().orElseThrow().lessonCount();

        lessonRepository.save(new Lesson(cif, "CIF99", "Bài kiểm thử đếm"));

        long after = catalogService.moduleCards().stream()
                .filter(c -> c.module().getId().equals(cif.getId()))
                .findFirst().orElseThrow().lessonCount();
        assertThat(after).isEqualTo(before + 1);
    }

    @Test
    void danhSachBaiTheoPhanHeXepTheoCode() {
        var cif = moduleRepository.findByCode("CIF").orElseThrow();
        var lessons = catalogService.lessonsOf(cif.getId());
        assertThat(lessons).isNotEmpty();
        assertThat(lessons).extracting(Lesson::getCode).isSorted();
        assertThat(lessons).allSatisfy(l -> assertThat(l.getModule().getId()).isEqualTo(cif.getId()));
    }
}
