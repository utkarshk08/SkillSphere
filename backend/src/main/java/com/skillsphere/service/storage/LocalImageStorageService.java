package com.skillsphere.service.storage;

import com.skillsphere.exception.FileUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.file.storage", havingValue = "local", matchIfMissing = true)
public class LocalImageStorageService implements ImageStorageService {

    private final Path uploadDirectory;

    public LocalImageStorageService(@Value("${app.file.upload-dir}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String folder) {
        String storedName = UUID.randomUUID() + extensionFor(file);
        Path categoryDirectory = uploadDirectory.resolve(folder).normalize();
        Path destination = categoryDirectory.resolve(storedName).normalize();
        if (!destination.startsWith(uploadDirectory)) {
            throw new FileUploadException("Invalid image storage path.");
        }

        try {
            Files.createDirectories(categoryDirectory);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | IllegalStateException exception) {
            throw new FileUploadException("Unable to store the image.", exception);
        }
        return folder + "/" + storedName;
    }

    private String extensionFor(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new FileUploadException("Image type must be JPG, PNG, or WEBP.");
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new FileUploadException("Image type must be JPG, PNG, or WEBP.");
        };
    }
}
