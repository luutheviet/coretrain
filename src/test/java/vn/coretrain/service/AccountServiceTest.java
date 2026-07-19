package vn.coretrain.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.coretrain.domain.Role;
import vn.coretrain.domain.User;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AccountServiceTest {

    @Autowired
    AccountService accountService;

    @Test
    void dangKyMoiLuonLaLearner() {
        User u = accountService.registerLearner("nv_moi", "matkhau123", "Nhân Viên Mới");
        assertThat(u.getRole()).isEqualTo(Role.LEARNER);
        assertThat(u.getPassword()).startsWith("$2");
    }

    @Test
    void usernameTrungBiPhatHien() {
        assertThat(accountService.usernameTaken("hocvien")).isTrue();
        assertThat(accountService.usernameTaken("khong_ton_tai")).isFalse();
    }
}
