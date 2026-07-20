package vn.coretrain.web;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.coretrain.domain.Lesson;
import vn.coretrain.domain.Section;
import vn.coretrain.domain.User;
import vn.coretrain.repo.LessonCompletionRepository;
import vn.coretrain.repo.LessonRepository;
import vn.coretrain.repo.ModuleRepository;
import vn.coretrain.repo.SectionRepository;
import vn.coretrain.repo.UserRepository;
// Module không import trực tiếp — phantom lesson gắn vào Section (chương)

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class LessonFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ModuleRepository moduleRepository;

    @Autowired
    LessonRepository lessonRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    LessonCompletionRepository completionRepository;

    private Lesson lessonByCode(String moduleCode, String lessonCode) {
        Long moduleId = moduleRepository.findByCode(moduleCode).orElseThrow().getId();
        return lessonRepository.findByModuleIdOrderByCodeAsc(moduleId).stream()
                .filter(l -> lessonCode.equals(l.getCode()))
                .findFirst().orElseThrow();
    }

    @Test
    void baiDu3NoiDungHienDu3Tab() throws Exception {
        Lesson cif03 = lessonByCode("CIF", "CIF03");
        mockMvc.perform(get("/lessons/" + cif03.getId()).with(user("hocvien").roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Tài liệu")))
                .andExpect(content().string(Matchers.containsString("Video Hướng Dẫn")))
                .andExpect(content().string(Matchers.containsString("Quy trình")))
                .andExpect(content().string(Matchers.containsString("CIF03")))
                .andExpect(content().string(Matchers.containsString("Khách hàng → Tạo mới CIF")));
    }

    @Test
    void baiTitleOnlyThieuNoiDungKhongVoTrang() throws Exception {
        Lesson cif01 = lessonByCode("CIF", "CIF01"); // seed title-only, chưa có nội dung
        mockMvc.perform(get("/lessons/" + cif01.getId()).with(user("hocvien").roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("chưa có nội dung")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Video Hướng Dẫn"))));
    }

    @Test
    void pdfServeInlineDungContentType() throws Exception {
        Lesson cif03 = lessonByCode("CIF", "CIF03");
        mockMvc.perform(get("/lessons/" + cif03.getId() + "/pdf").with(user("hocvien").roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString("inline")));
    }

    @Test
    void videoRangeTra206KemContentRange() throws Exception {
        Lesson cif03 = lessonByCode("CIF", "CIF03");
        mockMvc.perform(get("/lessons/" + cif03.getId() + "/video")
                        .header(HttpHeaders.RANGE, "bytes=0-99")
                        .with(user("hocvien").roles("LEARNER")))
                .andExpect(status().isPartialContent())
                .andExpect(header().exists(HttpHeaders.CONTENT_RANGE))
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"));
    }

    @Test
    void fileMatTrenDiaEndpoint404NhungTrangVan200() throws Exception {
        Section lnSection = sectionRepository.findByCode("LN-C1").orElseThrow();
        Lesson phantom = new Lesson(lnSection, "LN_PHANTOM", "Bài trỏ file đã mất");
        phantom.setPdfPath("lessons/khong-ton-tai.pdf"); // path set nhưng file không có trên đĩa
        phantom = lessonRepository.save(phantom);
        try {
            // Endpoint media: 404 (không 500)
            mockMvc.perform(get("/lessons/" + phantom.getId() + "/pdf").with(user("hocvien").roles("LEARNER")))
                    .andExpect(status().isNotFound());
            // Trang bài vẫn render 200 — có nút Mở ngoài/Tải, không trắng
            mockMvc.perform(get("/lessons/" + phantom.getId()).with(user("hocvien").roles("LEARNER")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("Tài liệu")));
        } finally {
            lessonRepository.deleteById(phantom.getId()); // dọn — không rò sang test khác trong context chung
        }
    }

    @Test
    void docKhongPhaiPdfHienDangTaiVe() throws Exception {
        Section lnSection = sectionRepository.findByCode("LN-C1").orElseThrow();
        Lesson doc = new Lesson(lnSection, "LN_DOC", "Bài tài liệu Word");
        // Trỏ vào 1 file có thật trên đĩa (video seed) nhưng đuôi khác .pdf → coi như doc không xem inline
        doc.setPdfPath("lessons/cif03.mp4");
        doc = lessonRepository.save(doc);
        try {
            // Trang: tab Tài liệu hiện thẻ tải về, không nhúng inline
            mockMvc.perform(get("/lessons/" + doc.getId()).with(user("hocvien").roles("LEARNER")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("không xem trực tiếp")))
                    .andExpect(content().string(Matchers.containsString("Tải file về")));
            // Endpoint: attachment (tải về), không phải application/pdf
            mockMvc.perform(get("/lessons/" + doc.getId() + "/pdf").with(user("hocvien").roles("LEARNER")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString("attachment")));
        } finally {
            lessonRepository.deleteById(doc.getId());
        }
    }

    @Test
    void baiKhongTonTaiTra404() throws Exception {
        mockMvc.perform(get("/lessons/999999").with(user("hocvien").roles("LEARNER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void chuaDangNhapBiChanVeLogin() throws Exception {
        mockMvc.perform(get("/lessons/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ---------- Story 1.4: đã học + last_viewed ----------

    @Test
    void moBaiGhiLastViewedChoUser() throws Exception {
        Lesson cif02 = lessonByCode("CIF", "CIF02");
        User user = userRepository.findByUsername("hocvien").orElseThrow();
        Long before = user.getLastViewedLessonId();
        try {
            mockMvc.perform(get("/lessons/" + cif02.getId()).with(user("hocvien").roles("LEARNER")))
                    .andExpect(status().isOk());
            assertThat(userRepository.findByUsername("hocvien").orElseThrow().getLastViewedLessonId())
                    .isEqualTo(cif02.getId());
        } finally {
            User u = userRepository.findByUsername("hocvien").orElseThrow();
            u.setLastViewedLessonId(before);
            userRepository.save(u);
        }
    }

    @Test
    void danhDauDaHocRedirectVaGhiNhan() throws Exception {
        Lesson cif01 = lessonByCode("CIF", "CIF01");
        Long userId = userRepository.findByUsername("hocvien").orElseThrow().getId();
        try {
            mockMvc.perform(post("/lessons/" + cif01.getId() + "/complete")
                            .with(user("hocvien").roles("LEARNER")).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/lessons/" + cif01.getId()));
            assertThat(completionRepository.existsByUserIdAndLessonId(userId, cif01.getId())).isTrue();

            // Mở lại trang → nút đổi thành "Đã học"
            mockMvc.perform(get("/lessons/" + cif01.getId()).with(user("hocvien").roles("LEARNER")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("Đã học")));
        } finally {
            completionRepository.findAll().stream()
                    .filter(c -> c.getUser().getId().equals(userId) && c.getLesson().getId().equals(cif01.getId()))
                    .forEach(c -> completionRepository.deleteById(c.getId()));
        }
    }

    @Test
    void danhDauBaiKhongTonTaiTra404() throws Exception {
        mockMvc.perform(post("/lessons/999999/complete")
                        .with(user("hocvien").roles("LEARNER")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void managerKhongDuocDanhDauDaHoc() throws Exception {
        Lesson cif01 = lessonByCode("CIF", "CIF01");
        // AD-4: MANAGER chỉ đọc — POST nghiệp vụ bị chặn server-side (403), không tạo completion
        mockMvc.perform(post("/lessons/" + cif01.getId() + "/complete")
                        .with(user("quanly").roles("MANAGER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void nutDanhDauKhongHienChoManager() throws Exception {
        Lesson cif03 = lessonByCode("CIF", "CIF03");
        mockMvc.perform(get("/lessons/" + cif03.getId()).with(user("quanly").roles("MANAGER")))
                .andExpect(status().isOk())
                // Không có form/nút đánh dấu (class mark-done); comment tĩnh trong template không tính
                .andExpect(content().string(Matchers.not(Matchers.containsString("mark-done"))));
    }

    @Test
    void danhDauMaCurrentUserNullTra401KhongNpe() throws Exception {
        // Auth hợp lệ (role LEARNER) nhưng username không có dòng User trong DB (desync) —
        // GlobalModelAttributes trả currentUser=null; controller phải 401 có kiểm soát, không NPE/500.
        Lesson cif01 = lessonByCode("CIF", "CIF01");
        mockMvc.perform(post("/lessons/" + cif01.getId() + "/complete")
                        .with(user("khong_ton_tai").roles("LEARNER")).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Story 1.4 code review fix: video Range biên ----------

    @Test
    void videoRangeVuotDoDaiFileTra416() throws Exception {
        // sample-cif03.mp4 seed = 2084 bytes — start vượt xa length phải là 416, không phải 500
        Lesson cif03 = lessonByCode("CIF", "CIF03");
        mockMvc.perform(get("/lessons/" + cif03.getId() + "/video")
                        .header(HttpHeaders.RANGE, "bytes=999999999-")
                        .with(user("hocvien").roles("LEARNER")))
                .andExpect(status().is(416))
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, Matchers.startsWith("bytes */")));
    }

    @Test
    void videoRangeSaiDinhDangCoiNhuKhongCoRangeTra200() throws Exception {
        Lesson cif03 = lessonByCode("CIF", "CIF03");
        mockMvc.perform(get("/lessons/" + cif03.getId() + "/video")
                        .header(HttpHeaders.RANGE, "bytes=abc-def")
                        .with(user("hocvien").roles("LEARNER")))
                .andExpect(status().isOk()); // không 500 — coi như không có Range, trả nguyên file
    }
}
