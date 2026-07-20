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

    /* EAGER có chủ đích: template đọc section/module của bài ngoài transaction (open-in-view=false)
       — LAZY ở đây là bẫy LazyInitializationException chờ nổ. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "section_id")
    private Section section;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    /* Nội dung 3 tab (Story 1.3) — đều nullable: null = "chưa có nội dung loại đó".
       pdfPath/videoPath là RELATIVE path dưới app.storage.dir (AD-6: DB giữ path, file ở filesystem);
       processContent là text từng bước lưu thẳng DB (nội dung học thuần, không phải file). */
    @Column(name = "pdf_path")
    private String pdfPath;

    @Column(name = "video_path")
    private String videoPath;

    @Column(name = "process_content", columnDefinition = "text")
    private String processContent;

    /** Breadcrumb đường dẫn menu nghiệp vụ (vd "Khách hàng → Tạo mới CIF → ..."); null → dùng tên phân hệ. */
    @Column(name = "menu_path")
    private String menuPath;

    protected Lesson() {
    }

    public Lesson(Section section, String code, String title) {
        this.section = section;
        this.code = code;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public Section getSection() {
        return section;
    }

    /** Tiện ích: phân hệ của bài = phân hệ của chương chứa nó (mọi read-side cũ dùng được như trước). */
    public Module getModule() {
        return section.getModule();
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getProcessContent() {
        return processContent;
    }

    public void setProcessContent(String processContent) {
        this.processContent = processContent;
    }

    public String getMenuPath() {
        return menuPath;
    }

    public void setMenuPath(String menuPath) {
        this.menuPath = menuPath;
    }

    public boolean hasDocument() {
        return pdfPath != null && !pdfPath.isBlank();
    }

    /** Chỉ PDF xem inline được; định dạng khác (docx, pptx...) → hiện dạng tải về. */
    public boolean isPdfDocument() {
        return hasDocument() && pdfPath.toLowerCase().endsWith(".pdf");
    }

    /** Tên file để hiển thị + đặt Content-Disposition khi tải. */
    public String getDocumentFileName() {
        if (!hasDocument()) {
            return null;
        }
        int slash = Math.max(pdfPath.lastIndexOf('/'), pdfPath.lastIndexOf('\\'));
        return slash >= 0 ? pdfPath.substring(slash + 1) : pdfPath;
    }

    /** Đuôi file viết hoa để hiển thị (vd "DOCX"); rỗng nếu không có đuôi. */
    public String getDocumentExtension() {
        String name = getDocumentFileName();
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toUpperCase() : "";
    }

    public boolean hasVideo() {
        return videoPath != null && !videoPath.isBlank();
    }

    public boolean hasProcess() {
        return processContent != null && !processContent.isBlank();
    }
}
