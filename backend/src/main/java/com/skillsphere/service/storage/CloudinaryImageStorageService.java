package com.skillsphere.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.skillsphere.exception.FileUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.file.storage", havingValue = "cloudinary")
public class CloudinaryImageStorageService implements ImageStorageService {

    private final Cloudinary cloudinary;
    private final String rootFolder;

    public CloudinaryImageStorageService(
            Cloudinary cloudinary,
            @Value("${app.cloudinary.folder}") String rootFolder
    ) {
        this.cloudinary = cloudinary;
        this.rootFolder = rootFolder;
    }

    @Override
    public String store(MultipartFile file, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", rootFolder + "/" + folder,
                            "resource_type", "image",
                            "unique_filename", true,
                            "overwrite", false
                    )
            );
            Object secureUrl = result.get("secure_url");
            if (secureUrl == null || secureUrl.toString().isBlank()) {
                throw new FileUploadException("Cloud image storage did not return a secure URL.");
            }
            return secureUrl.toString();
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof FileUploadException fileUploadException) {
                throw fileUploadException;
            }
            throw new FileUploadException("Unable to store the image in Cloudinary.", exception);
        }
    }
}
