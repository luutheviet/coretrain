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

/**
 * Chương — tầng giữa Phân hệ và Bài học (cấu trúc kiểu Udemy: Phân hệ → Chương → Bài học 3-tab).
 * Nhóm các bài trong một phân hệ thành cụm có thứ tự.
 */
@Entity
@Table(name = "sections")
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* EAGER: template/breadcrumb đọc module của chương ngoài transaction (open-in-view=false). */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "module_id")
    private Module module;

    /** Mã duy nhất để seed idempotent (vd "CIF-C1"). */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Section() {
    }

    public Section(Module module, String code, String title, int sortOrder) {
        this.module = module;
        this.code = code;
        this.title = title;
        this.sortOrder = sortOrder;
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

    public int getSortOrder() {
        return sortOrder;
    }
}
