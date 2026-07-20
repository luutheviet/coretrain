package vn.coretrain.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.coretrain.domain.Module;
import vn.coretrain.repo.LessonCompletionRepository;
import vn.coretrain.repo.LessonRepository;
import vn.coretrain.repo.UserRepository;

/**
 * Chỗ tính tiến độ học DUY NHẤT — derive lúc đọc từ bảng sự kiện {@code lesson_completion} (AD-2).
 * Story 2.4 (tiến độ tổng thể + lịch sử) và ScoringService (điểm học, AD-3) GỌI LẠI service này,
 * KHÔNG tự tính % chỗ khác.
 */
@Service
public class ProgressService {

    /** Tiến độ 1 phân hệ của 1 user: done ≤ total nên percent ∈ [0,100] (không bao giờ vượt 100%). */
    public record ModuleProgress(Module module, long total, long done, int percent) {
    }

    private final LessonCompletionRepository completionRepository;
    private final CatalogService catalogService;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final LessonCompletionWriter completionWriter;

    public ProgressService(LessonCompletionRepository completionRepository, CatalogService catalogService,
                           UserRepository userRepository, LessonRepository lessonRepository,
                           LessonCompletionWriter completionWriter) {
        this.completionRepository = completionRepository;
        this.catalogService = catalogService;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.completionWriter = completionWriter;
    }

    /**
     * Đánh dấu bài đã học — idempotent: đã học rồi thì bỏ qua (không đếm trùng, AC #3).
     * AD-4 defense-in-depth: chặn cả ở service (route đã chặn ở SecurityConfig) — nếu sau này có
     * entry point khác gọi thẳng service này, MANAGER/EDITOR vẫn không ghi được completion.
     * @return true nếu vừa ghi nhận lần đầu, false nếu đã học từ trước.
     */
    @Transactional
    @PreAuthorize("hasRole('LEARNER')")
    public boolean markCompleted(Long userId, Long lessonId) {
        if (completionRepository.existsByUserIdAndLessonId(userId, lessonId)) {
            return false;
        }
        try {
            // Insert chạy transaction riêng (LessonCompletionWriter, REQUIRES_NEW) — thua race
            // (unique constraint) chỉ rollback transaction insert đó, không lây sang transaction này.
            completionWriter.insert(userRepository.getReferenceById(userId), lessonRepository.getReferenceById(lessonId));
            return true;
        } catch (DataIntegrityViolationException race) {
            // Hai request cùng lúc cùng lọt qua check exists → unique constraint chặn dòng thứ hai.
            // Coi như đã học, không throw (nút bấm nhanh 2 lần không được 500).
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean isCompleted(Long userId, Long lessonId) {
        return completionRepository.existsByUserIdAndLessonId(userId, lessonId);
    }

    /**
     * Tiến độ theo từng phân hệ của user — tự fetch tổng bài qua {@link CatalogService#moduleCards()}.
     * Dùng khi caller CHƯA sẵn danh sách module card; nếu đã có (vd DashboardController), gọi
     * overload {@link #moduleProgress(Long, List)} để khỏi query lại.
     */
    @Transactional(readOnly = true)
    public List<ModuleProgress> moduleProgress(Long userId) {
        return moduleProgress(userId, catalogService.moduleCards());
    }

    /**
     * Tiến độ theo từng phân hệ, tính trên danh sách module card ĐÃ CÓ SẴN (tránh gọi lại
     * {@code moduleCards()} khi caller vừa fetch xong) — số đã học từ group-by (1 query).
     * done ≤ total nên percent kẹp trong [0,100].
     */
    @Transactional(readOnly = true)
    public List<ModuleProgress> moduleProgress(Long userId, List<CatalogService.ModuleCard> cards) {
        Map<Long, Long> doneByModule = completionRepository.doneGroupByModule(userId).stream()
                .collect(Collectors.toMap(
                        LessonCompletionRepository.ModuleDoneCount::getModuleId,
                        LessonCompletionRepository.ModuleDoneCount::getDone));
        return cards.stream()
                .map(card -> {
                    long total = card.lessonCount();
                    long done = Math.min(doneByModule.getOrDefault(card.module().getId(), 0L), total);
                    int percent = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
                    return new ModuleProgress(card.module(), total, done, percent);
                })
                .toList();
    }
}
