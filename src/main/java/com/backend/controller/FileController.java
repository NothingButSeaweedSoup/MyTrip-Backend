package com.backend.controller;

import com.backend.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/file")
public class FileController {

    /** 图片压缩：最长边超过此值则按比例缩小 */
    private static final int MAX_IMAGE_DIMENSION = 1920;

    /** JPEG 压缩质量 (0~1) */
    private static final float JPEG_QUALITY = 0.8f;

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

        // 对图片进行压缩，非图片文件直接保存
        if (isImageExtension(ext)) {
            try (InputStream in = file.getInputStream()) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    BufferedImage compressed = compressImage(image);
                    writeCompressedImage(compressed, ext, target);
                } else {
                    file.transferTo(target.toFile());
                }
            }
        } else {
            file.transferTo(target.toFile());
        }

        return Result.success("/uploads/" + filename);
    }

    /** 判断文件扩展名是否为支持的图片格式 */
    private boolean isImageExtension(String ext) {
        if (ext == null) return false;
        String lower = ext.toLowerCase();
        return lower.equals(".jpg") || lower.equals(".jpeg")
                || lower.equals(".png") || lower.equals(".bmp")
                || lower.equals(".gif");
    }

    /** 按比例缩小图片（若超过最大尺寸） */
    private BufferedImage compressImage(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();

        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return source;
        }

        double scale = (double) MAX_IMAGE_DIMENSION / Math.max(width, height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return scaled;
    }

    /** 以压缩质量写入图片文件 */
    private void writeCompressedImage(BufferedImage image, String ext, Path target) throws IOException {
        String lower = ext.toLowerCase();

        // JPEG：使用可调节质量的写入器
        if (lower.equals(".jpg") || lower.equals(".jpeg")) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY);

                try (ImageOutputStream ios = ImageIO.createImageOutputStream(target.toFile())) {
                    writer.setOutput(ios);
                    writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
                }
                writer.dispose();
                return;
            }
        }

        // PNG / BMP / GIF：直接写入（PNG 的 Deflate 压缩由 ImageIO 自动处理）
        ImageIO.write(image, lower.replace(".", ""), target.toFile());
    }
}
