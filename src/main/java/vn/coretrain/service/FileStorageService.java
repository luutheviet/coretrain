package vn.coretrain.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AD-6: một cơ chế lưu/đọc file DUY NHẤT. PDF/video/ảnh QR nằm dưới {@code app.storage.dir}
 * (ngoài jar); DB chỉ giữ relative path. Story sau (media seed 1.6, QR 4.4) mở rộng service này —
 * cấm tự chế cách đọc/ghi file khác.
 *
 * Bảo mật: mọi relative path phải nằm TRONG base dir sau khi normalize (chống {@code ../} traversal).
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path base;

    public FileStorageService(@Value("${app.storage.dir}") String storageDir) {
        this.base = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    /**
     * Resolve relative path thành file đọc được dưới base dir.
     *
     * @return path nếu file tồn tại + đọc được; {@link Optional#empty()} nếu path rỗng hoặc file không
     *     tồn tại/không đọc được (controller fallback, KHÔNG throw).
     * @throws SecurityException nếu path thoát ra ngoài base dir (traversal).
     */
    public Optional<Path> resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return Optional.empty();
        }
        Path resolved = base.resolve(relativePath).normalize();
        if (!resolved.startsWith(base)) {
            throw new SecurityException("Path thoát ra ngoài storage dir: " + relativePath);
        }
        if (!Files.exists(resolved) || !Files.isReadable(resolved) || Files.isDirectory(resolved)) {
            return Optional.empty();
        }
        return Optional.of(resolved);
    }

    /**
     * Copy file mẫu từ classpath vào storage dir nếu chưa tồn tại (idempotent — dùng cho seed).
     *
     * @return relative path đã lưu (để gán vào entity).
     */
    public String storeFromClasspath(String classpathResource, String relativeTarget) {
        Path target = base.resolve(relativeTarget).normalize();
        if (!target.startsWith(base)) {
            throw new SecurityException("Target thoát ra ngoài storage dir: " + relativeTarget);
        }
        try {
            if (Files.exists(target)) {
                return relativeTarget; // idempotent
            }
            Files.createDirectories(target.getParent());
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
                if (in == null) {
                    throw new IllegalStateException("Không tìm thấy seed resource: " + classpathResource);
                }
                Files.copy(in, target);
            }
            log.info("Seed: copy media {} -> {}", classpathResource, target);
            return relativeTarget;
        } catch (IOException e) {
            throw new UncheckedIOException("Lỗi copy seed media " + classpathResource, e);
        }
    }
}
