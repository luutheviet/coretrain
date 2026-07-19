package vn.coretrain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Phân hệ Core Banking — bảng DB seed 7 dòng (spine: mở rộng = thêm dòng, không sửa code).
 * Mọi màn hình theo-phân-hệ render động theo số dòng bảng này.
 */
@Entity
@Table(name = "modules")
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Module() {
    }

    public Module(String code, String name, String icon, int sortOrder) {
        this.code = code;
        this.name = name;
        this.icon = icon;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
