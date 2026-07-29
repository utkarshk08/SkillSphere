package com.skillsphere.service.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Stores validated application images and returns the path or URL persisted by the domain model.
 */
public interface ImageStorageService {

    String store(MultipartFile file, String folder);
}
