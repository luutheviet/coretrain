package vn.coretrain.service;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit thuần — không Spring; kiểm cơ chế resolve + chống path traversal (AD-6). */
class FileStorageServiceTest {

    @TempDir
    Path base;

    FileStorageService storage;

    @BeforeEach
    void setUp() throws Exception {
        storage = new FileStorageService(base.toString());
        Files.createDirectories(base.resolve("lessons"));
        Files.writeString(base.resolve("lessons/a.pdf"), "hello");
    }

    @Test
    void resolveFileTonTaiTraPath() {
        assertThat(storage.resolve("lessons/a.pdf")).hasValueSatisfying(
                p -> assertThat(p).isEqualTo(base.resolve("lessons/a.pdf").normalize()));
    }

    @Test
    void resolveFileMatTraEmpty() {
        assertThat(storage.resolve("lessons/khong-co.pdf")).isEmpty();
    }

    @Test
    void resolvePathRongTraEmpty() {
        assertThat(storage.resolve(null)).isEmpty();
        assertThat(storage.resolve("  ")).isEmpty();
    }

    @Test
    void resolveThuMucTraEmpty() {
        assertThat(storage.resolve("lessons")).isEmpty();
    }

    @Test
    void traversalThoatBaseDirNemLoi() {
        assertThatThrownBy(() -> storage.resolve("../secret.txt"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> storage.resolve("lessons/../../secret.txt"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void storeFromClasspathCopyVaIdempotent() {
        String rel = storage.storeFromClasspath("seed/sample-cif03.pdf", "lessons/copied.pdf");
        assertThat(rel).isEqualTo("lessons/copied.pdf");
        assertThat(base.resolve(rel)).exists();
        long size = base.resolve(rel).toFile().length();
        // Chạy lại không lỗi, không đổi file
        assertThat(storage.storeFromClasspath("seed/sample-cif03.pdf", "lessons/copied.pdf")).isEqualTo(rel);
        assertThat(base.resolve(rel).toFile().length()).isEqualTo(size);
    }
}
