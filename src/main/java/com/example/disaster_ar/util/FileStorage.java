package com.example.disaster_ar.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;

@Component
public class FileStorage {

    @Value("${file.upload.dir}")
    private String uploadDir;

    public String save(MultipartFile file) {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path target = Paths.get(uploadDir).resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString().replace("\\", "/");
        } catch (Exception e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }
}
