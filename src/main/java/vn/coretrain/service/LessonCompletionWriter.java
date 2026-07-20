package vn.coretrain.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.coretrain.domain.Lesson;
import vn.coretrain.domain.LessonCompletion;
import vn.coretrain.domain.User;
import vn.coretrain.repo.LessonCompletionRepository;

/**
 * Insert 1 dòng {@code lesson_completion} trong transaction RIÊNG (REQUIRES_NEW) — tách khỏi
 * {@link ProgressService#markCompleted}. Lý do: entity dùng GenerationType.IDENTITY nên save()
 * flush INSERT ngay lập tức; nếu 2 request đua nhau cùng insert, dòng thua unique constraint sẽ
 * ném exception ngay tại statement đó. Theo tài liệu Hibernate, Session vừa ném exception nên bị
 * coi là hỏng — nếu insert này chạy chung transaction với markCompleted(), catch cục bộ không
 * đảm bảo transaction ngoài vẫn commit sạch. Cô lập trong REQUIRES_NEW: transaction insert tự
 * rollback độc lập khi thua race, không lây sang transaction đọc/redirect bên ngoài.
 */
@Component
public class LessonCompletionWriter {

    private final LessonCompletionRepository completionRepository;

    public LessonCompletionWriter(LessonCompletionRepository completionRepository) {
        this.completionRepository = completionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(User user, Lesson lesson) {
        completionRepository.save(new LessonCompletion(user, lesson));
    }
}
