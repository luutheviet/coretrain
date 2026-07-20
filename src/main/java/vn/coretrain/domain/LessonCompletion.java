package vn.coretrain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Sự kiện gốc "đã học" (AD-2) — mỗi (user, lesson) tối đa 1 dòng nhờ unique constraint,
 * nên đánh dấu lại KHÔNG đếm trùng (idempotent ở tầng DB, chống cả race hai request).
 * Mọi con số tiến độ derive lúc đọc từ bảng này — CẤM cột tổng hợp/cache.
 */
@Entity
@Table(name = "lesson_completion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
public class LessonCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* LAZY: chỉ cần id để đếm/kiểm tra, không load full user/lesson. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    /** UTC (convention "Thời gian") — thời điểm học viên bấm "đã học". */
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected LessonCompletion() {
    }

    public LessonCompletion(User user, Lesson lesson) {
        this.user = user;
        this.lesson = lesson;
        this.completedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
