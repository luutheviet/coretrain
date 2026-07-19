package vn.coretrain.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.coretrain.domain.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByModuleIdOrderByCodeAsc(Long moduleId);

    boolean existsByCode(String code);

    /** Đếm số bài mọi phân hệ trong 1 query — cấm N+1 per thẻ, cấm cột cache (AD-2). */
    @Query("select l.module.id as moduleId, count(l) as lessonCount from Lesson l group by l.module.id")
    List<ModuleLessonCount> countGroupByModule();

    interface ModuleLessonCount {
        Long getModuleId();

        long getLessonCount();
    }
}
