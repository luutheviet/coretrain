package vn.coretrain.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.coretrain.domain.Role;
import vn.coretrain.repo.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SeedRunnerTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    vn.coretrain.repo.ModuleRepository moduleRepository;

    @Autowired
    vn.coretrain.repo.LessonRepository lessonRepository;

    @Autowired
    SeedRunner seedRunner;

    @Test
    void seedTaoDu3TaiKhoanDemo3Vai() {
        assertThat(userRepository.findByUsername("hocvien")).hasValueSatisfying(
                u -> assertThat(u.getRole()).isEqualTo(Role.LEARNER));
        assertThat(userRepository.findByUsername("soanbai")).hasValueSatisfying(
                u -> assertThat(u.getRole()).isEqualTo(Role.EDITOR));
        assertThat(userRepository.findByUsername("quanly")).hasValueSatisfying(
                u -> assertThat(u.getRole()).isEqualTo(Role.MANAGER));
    }

    @Test
    void seedIdempotent_chayLaiKhongNhanDoi() {
        long usersBefore = userRepository.count();
        long modulesBefore = moduleRepository.count();
        long lessonsBefore = lessonRepository.count();
        seedRunner.run(new org.springframework.boot.DefaultApplicationArguments());
        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(moduleRepository.count()).isEqualTo(modulesBefore);
        assertThat(lessonRepository.count()).isEqualTo(lessonsBefore);
    }

    @Test
    void matKhauSeedKhongLuuPlaintext() {
        assertThat(userRepository.findByUsername("hocvien")).hasValueSatisfying(u -> {
            assertThat(u.getPassword()).doesNotContain("coretrain123");
            assertThat(u.getPassword()).startsWith("$2"); // BCrypt prefix
        });
    }
}
