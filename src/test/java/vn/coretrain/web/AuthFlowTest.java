package vn.coretrain.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import vn.coretrain.domain.Role;
import vn.coretrain.repo.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Test
    void chuaDangNhapBiChuyenVeLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void dangNhapDungMatKhauVaoApp() throws Exception {
        mockMvc.perform(formLogin("/login").user("hocvien").password("coretrain123"))
                .andExpect(authenticated().withRoles("LEARNER"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void caBaVaiDemoDangNhapDuocDungVai() throws Exception {
        mockMvc.perform(formLogin("/login").user("soanbai").password("coretrain123"))
                .andExpect(authenticated().withRoles("EDITOR"));
        mockMvc.perform(formLogin("/login").user("quanly").password("coretrain123"))
                .andExpect(authenticated().withRoles("MANAGER"));
    }

    @Test
    void saiMatKhauBiTuChoiVeLoginError() throws Exception {
        mockMvc.perform(formLogin("/login").user("hocvien").password("sai-mat-khau"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void trangLoginVaRegisterKhongCanDangNhap() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void dangKyQuaFormLuonRaLearner() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "nv_form")
                        .param("password", "matkhau123")
                        .param("fullName", "Nhân Viên Form"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertThat(userRepository.findByUsername("nv_form")).hasValueSatisfying(
                u -> assertThat(u.getRole()).isEqualTo(Role.LEARNER));
    }

    @Test
    void dangKyUsernameCoKhoangTrangHoacKyTuLaBiTuChoi() throws Exception {
        long before = userRepository.count();
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "nv moi ✿")
                        .param("password", "matkhau123")
                        .param("fullName", "Nhân Viên Lạ"))
                .andExpect(status().isOk()); // re-render form với lỗi pattern
        assertThat(userRepository.count()).isEqualTo(before);
    }

    @Test
    void daDangNhapVaoLoginRegisterBiChuyenVeTrangChu() throws Exception {
        mockMvc.perform(get("/login").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("hocvien").roles("LEARNER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(get("/register").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("hocvien").roles("LEARNER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void dangKyThieuThongTinKhongTaoUser() throws Exception {
        long before = userRepository.count();
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "x")
                        .param("password", "1")
                        .param("fullName", ""))
                .andExpect(status().isOk()); // quay lại form với lỗi
        assertThat(userRepository.count()).isEqualTo(before);
    }
}
