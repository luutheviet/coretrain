package vn.coretrain.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.coretrain.domain.Lesson;
import vn.coretrain.domain.Module;
import vn.coretrain.domain.Section;
import vn.coretrain.repo.LessonRepository;
import vn.coretrain.repo.ModuleRepository;
import vn.coretrain.repo.SectionRepository;

/** Kho bài học theo phân hệ → chương → bài — số bài luôn COUNT lúc đọc, không cache (AD-2). */
@Service
public class CatalogService {

    /** Thẻ phân hệ trên dashboard: phân hệ + số bài đếm thực tế. */
    public record ModuleCard(Module module, long lessonCount) {
    }

    /** Chương kèm danh sách bài của nó — dựng cây cho trang phân hệ (kiểu Udemy). */
    public record SectionGroup(Section section, List<Lesson> lessons) {
    }

    private final ModuleRepository moduleRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;

    public CatalogService(ModuleRepository moduleRepository, SectionRepository sectionRepository,
                          LessonRepository lessonRepository) {
        this.moduleRepository = moduleRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
    }

    /** 2 query cố định (module + count group-by) trong 1 transaction đọc — không N+1, snapshot nhất quán. */
    @Transactional(readOnly = true)
    public List<ModuleCard> moduleCards() {
        Map<Long, Long> counts = lessonRepository.countGroupByModule().stream()
                .collect(Collectors.toMap(
                        LessonRepository.ModuleLessonCount::getModuleId,
                        LessonRepository.ModuleLessonCount::getLessonCount));
        return moduleRepository.findAllByOrderBySortOrderAsc().stream()
                .map(m -> new ModuleCard(m, counts.getOrDefault(m.getId(), 0L)))
                .toList();
    }

    public Optional<Module> findModule(Long id) {
        return moduleRepository.findById(id);
    }

    /**
     * Cây phân hệ: các chương (theo thứ tự) kèm bài của từng chương — cho trang /modules/{id}.
     * 2 query cố định (sections + toàn bộ lesson của phân hệ) rồi gom theo chương trong bộ nhớ —
     * không N+1 theo số chương (AD-2). Bài trong chương giữ thứ tự code (stream sắp sẵn).
     */
    @Transactional(readOnly = true)
    public List<SectionGroup> sectionsWithLessons(Long moduleId) {
        Map<Long, List<Lesson>> lessonsBySection = lessonRepository.findByModuleIdOrderByCodeAsc(moduleId)
                .stream()
                .collect(Collectors.groupingBy(l -> l.getSection().getId()));
        return sectionRepository.findByModuleIdOrderBySortOrderAsc(moduleId).stream()
                .map(s -> new SectionGroup(s, lessonsBySection.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    /** Toàn bộ bài của phân hệ (phẳng, qua chương) — dùng cho đếm/kiểm tra. */
    public List<Lesson> lessonsOf(Long moduleId) {
        return lessonRepository.findByModuleIdOrderByCodeAsc(moduleId);
    }

    public Optional<Lesson> findLesson(Long id) {
        return lessonRepository.findById(id);
    }
}
