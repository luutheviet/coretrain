package vn.coretrain.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import vn.coretrain.repo.ModuleRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ModuleRepository moduleRepository;

    @Autowired
    vn.coretrain.repo.LessonRepository lessonRepository;

    @Autowired
    vn.coretrain.repo.UserRepository userRepository;

    @Test
    void dashboardHienDu7TenPhanHe() throws Exception {
        var result = mockMvc.perform(get("/").with(user("hocvien").roles("LEARNER")))
                .andExpect(status().isOk());
        for (String name : new String[]{"Tiền vay", "Chuyển tiền", "Tiền gửi", "CIF", "Teller", "GL", "Thẻ"}) {
            result.andExpect(content().string(org.hamcrest.Matchers.containsString(name)));
        }
    }

    @Test
    void userChuaXemBaiNaoDashboardVanChay() throws Exception {
        mockMvc.perform(get("/").with(user("hocvien").roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa xem bài nào")));
    }

    @Test
    void trangPhanHeHopLeHienDanhSachBai() throws Exception {
        var cif = moduleRepository.findByCode("CIF").orElseThrow();
        mockMvc.perform(get("/modules/" + cif.getId()).with(user("hocvien").roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CIF03")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quy trình tạo CIF Khách hàng tổ chức")));
    }

    @Test
    void phanHeKhongTonTaiTra404() throws Exception {
        mockMvc.perform(get("/modules/999999").with(user("hocvien").roles("LEARNER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void chuaDangNhapDashboardVaModulesBiChan() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/login"));
        mockMvc.perform(get("/modules/1")).andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/login"));
    }

    @Test
    void dashboardHienBaiXemGanNhatVaNutTiepTucHoc() throws Exception {
        var cif = moduleRepository.findByCode("CIF").orElseThrow();
        var lesson = lessonRepository.findByModuleIdOrderByCodeAsc(cif.getId()).get(0);
        var user = userRepository.findByUsername("hocvien").orElseThrow();
        user.setLastViewedLessonId(lesson.getId());
        userRepository.save(user);
        try {
            mockMvc.perform(get("/").with(user("hocvien").roles("LEARNER")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(lesson.getTitle())))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("TIẾP TỤC HỌC")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("/modules/" + cif.getId())));
        } finally {
            user.setLastViewedLessonId(null); // trả trạng thái — không rò sang test khác trong context chung
            userRepository.save(user);
        }
    }
}
