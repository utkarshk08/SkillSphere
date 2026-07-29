package com.skillsphere.service.storage;

import com.cloudinary.Cloudinary;
import com.skillsphere.config.CloudinaryConfig;
import com.skillsphere.exception.FileUploadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void localStoragePersistsAnAllowedImageAndReturnsItsRelativePath() throws IOException {
        LocalImageStorageService service = new LocalImageStorageService(temporaryDirectory.toString());
        byte[] content = new byte[]{1, 2, 3, 4};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                content
        );

        String storedPath = service.store(file, "profiles");

        assertThat(storedPath).startsWith("profiles/").endsWith(".png");
        assertArrayEquals(content, Files.readAllBytes(temporaryDirectory.resolve(storedPath)));
    }

    @Test
    void localStorageRejectsUnsupportedContentTypes() {
        LocalImageStorageService service = new LocalImageStorageService(temporaryDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.txt",
                "text/plain",
                "not-an-image".getBytes()
        );

        assertThrows(FileUploadException.class, () -> service.store(file, "profiles"));
    }

    @Test
    void cloudinaryStorageWrapsUploadReadFailures() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("read failed"));
        CloudinaryImageStorageService service = new CloudinaryImageStorageService(
                mock(Cloudinary.class),
                "skillsphere"
        );

        assertThrows(FileUploadException.class, () -> service.store(file, "profiles"));
    }

    @Test
    void storagePropertySelectsExactlyOneProvider() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(
                        LocalImageStorageService.class,
                        CloudinaryConfig.class,
                        CloudinaryImageStorageService.class
                );

        runner.withPropertyValues(
                        "app.file.storage=local",
                        "app.file.upload-dir=target/provider-test"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ImageStorageService.class);
                    assertThat(context.getBean(ImageStorageService.class))
                            .isInstanceOf(LocalImageStorageService.class);
                });

        runner.withPropertyValues(
                        "app.file.storage=cloudinary",
                        "app.cloudinary.url=cloudinary://key:secret@demo",
                        "app.cloudinary.folder=skillsphere"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ImageStorageService.class);
                    assertThat(context.getBean(ImageStorageService.class))
                            .isInstanceOf(CloudinaryImageStorageService.class);
                });
    }
}
