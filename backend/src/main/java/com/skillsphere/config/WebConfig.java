package com.skillsphere.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Makes application-uploaded files reachable through a predictable URL.
 *
 * The database stores a relative path while this configuration maps /uploads/** to the local
 * upload directory. Cloud deployments switch to Cloudinary and do not create this handler.
 */
@Configuration
@ConditionalOnProperty(name = "app.file.storage", havingValue = "local", matchIfMissing = true)
public class WebConfig implements WebMvcConfigurer {

    private final Path uploadDirectory;

    public WebConfig(@Value("${app.file.upload-dir}") String uploadDirectory) {
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    void createUploadDirectory() {
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create the upload directory: " + uploadDirectory, exception);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDirectory.toUri().toString());
    }
}
