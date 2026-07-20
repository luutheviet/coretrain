package vn.coretrain.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.coretrain.domain.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /** Bài trong 1 chương, theo mã. */
    List<Lesson> findBySectionIdOrderByCodeAsc(Long sectionId);

    /** Toàn bộ bài của 1 phân hệ (qua chương), theo mã — dùng cho đếm/kiểm tra. */
    @Query("select l from Lesson l where l.section.module.id = :moduleId order by l.code asc")
    List<Lesson> findByModuleIdOrderByCodeAsc(Long moduleId);

    boolean existsByCode(String code);

    /** Đếm số bài mọi phân hệ trong 1 query (qua chương) — cấm N+1 per thẻ, cấm cột cache (AD-2). */
    @Query("select l.section.module.id as moduleId, count(l) as lessonCount "
            + "from Lesson l group by l.section.module.id")
    List<ModuleLessonCount> countGroupByModule();

    interface ModuleLessonCount {
        Long getModuleId();

        long getLessonCount();
    }
}
