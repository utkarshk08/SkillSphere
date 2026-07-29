package com.skillsphere.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(name = "app.file.storage", havingValue = "cloudinary")
public class CloudinaryConfig {

    @Bean
    Cloudinary cloudinary(@Value("${app.cloudinary.url}") String cloudinaryUrl) {
        if (!StringUtils.hasText(cloudinaryUrl)) {
            throw new IllegalStateException("CLOUDINARY_URL is required when FILE_STORAGE=cloudinary.");
        }
        Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
        cloudinary.config.secure = true;
        return cloudinary;
    }
}
