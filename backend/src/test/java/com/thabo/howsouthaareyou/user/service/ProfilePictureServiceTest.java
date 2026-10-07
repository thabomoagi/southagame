package com.thabo.howsouthaareyou.user.service;

import com.thabo.howsouthaareyou.common.exception.BadRequestException;
import com.thabo.howsouthaareyou.config.UploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfilePictureServiceTest {

    @TempDir
    Path tempDir;

    private ProfilePictureService service;

    @BeforeEach
    void setUp() {
        UploadProperties properties = new UploadProperties();
        properties.setDir(tempDir.toString());
        properties.setUrlPrefix("/uploads");
        service = new ProfilePictureService(properties);
    }

    private MockMultipartFile pngFile() throws Exception {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return new MockMultipartFile("file", "avatar.png", "image/png", baos.toByteArray());
    }

    private MockMultipartFile oversizedDimensionPngFile() throws Exception {
        BufferedImage image = new BufferedImage(513, 32, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return new MockMultipartFile("file", "avatar.png", "image/png", baos.toByteArray());
    }

    @Test
    void store_convertsImageToWebp_andReturnsPublicUrl() throws Exception {
        UUID userId = UUID.randomUUID();

        String url = service.store(pngFile(), userId);

        // Public URL format: /uploads/user_{id}_{timestamp}.webp
        assertThat(url).startsWith("/uploads/user_").endsWith(".webp");

        String filename = url.substring(url.lastIndexOf('/') + 1);
        Path saved = tempDir.resolve(filename);
        assertThat(saved).exists();

        // Verify actual WebP container header: RIFF....WEBP
        byte[] bytes = Files.readAllBytes(saved);
        assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("RIFF");
        assertThat(new String(bytes, 8, 4, StandardCharsets.US_ASCII)).isEqualTo("WEBP");

        // The user id is embedded in the filename.
        assertThat(filename).startsWith("user_" + userId + "_");
    }

    @Test
    void store_rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.store(empty, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No image");
    }

    @Test
    void store_rejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "note.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.store(file, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported image type");
    }

    @Test
    void store_rejectsNonImagePayload() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png",
                "this is definitely not an image".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.store(file, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid image");
    }

    @Test
    void store_resizesOversizedImageToMaxDimensionAndStoresSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();

        String url = service.store(oversizedDimensionPngFile(), userId);

        assertThat(url).startsWith("/uploads/user_").endsWith(".webp");

        String filename = url.substring(url.lastIndexOf('/') + 1);
        Path saved = tempDir.resolve(filename);
        assertThat(saved).exists();

        BufferedImage resultImage = ImageIO.read(saved.toFile());
        assertThat(resultImage).isNotNull();
        assertThat(resultImage.getWidth()).isLessThanOrEqualTo(512);
        assertThat(resultImage.getHeight()).isLessThanOrEqualTo(512);
    }
}