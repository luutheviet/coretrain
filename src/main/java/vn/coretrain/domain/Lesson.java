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

/** Bài học — Story 1.2 chỉ cần code+title; cột nội dung 3 tab do Story 1.3 thêm. */
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* EAGER có chủ đích: template đọc module của bài ngoài transaction (open-in-view=false)
       — LAZY ở đây là bẫy LazyInitializationException chờ nổ. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "module_id")
    private Module module;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    protected Lesson() {
    }

    public Lesson(Module module, String code, String title) {
        this.module = module;
        this.code = code;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public Module getModule() {
        return module;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }
}
