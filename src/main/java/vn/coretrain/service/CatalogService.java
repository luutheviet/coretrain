package vn.coretrain.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.coretrain.domain.Lesson;
import vn.coretrain.domain.Module;
import vn.coretrain.repo.LessonRepository;
import vn.coretrain.repo.ModuleRepository;

/** Kho bài học theo phân hệ — số bài luôn COUNT lúc đọc, không cache (AD-2). */
@Service
public class CatalogService {

    /** Thẻ phân hệ trên dashboard: phân hệ + số bài đếm thực tế. */
    public record ModuleCard(Module module, long lessonCount) {
    }

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;

    public CatalogService(ModuleRepository moduleRepository, LessonRepository lessonRepository) {
        this.moduleRepository = moduleRepository;
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

    public List<Lesson> lessonsOf(Long moduleId) {
        return lessonRepository.findByModuleIdOrderByCodeAsc(moduleId);
    }

    public Optional<Lesson> findLesson(Long id) {
        return lessonRepository.findById(id);
    }
}
