package vn.coretrain.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.coretrain.domain.LessonCompletion;

/** Đọc sự kiện "đã học" — mọi tiến độ derive từ đây lúc đọc (AD-2), không cột cache. */
public interface LessonCompletionRepository extends JpaRepository<LessonCompletion, Long> {

    /** Idempotent guard: bài này user đã đánh dấu học chưa. */
    boolean existsByUserIdAndLessonId(Long userId, Long lessonId);

    /** Tổng số bài đã học của 1 user — cho tiến độ tổng thể (Story 2.4 dùng lại). */
    long countByUserId(Long userId);

    /** Số bài đã học theo từng phân hệ (join qua chương) — 1 query group-by, không N+1. */
    @Query("select c.lesson.section.module.id as moduleId, count(c) as done "
            + "from LessonCompletion c where c.user.id = :userId "
            + "group by c.lesson.section.module.id")
    List<ModuleDoneCount> doneGroupByModule(Long userId);

    interface ModuleDoneCount {
        Long getModuleId();

        long getDone();
    }
}
