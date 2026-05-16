package com.backend.controller;

import com.backend.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/file")
public class FileController {

    @Value("${upload.path:/data/uploads}")
    private String uploadPath;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        Path dir = Paths.get(uploadPath);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());

        return Result.success("/uploads/" + filename);
    }
}
