package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.InternalServerException;
import com.example.smart_booking_system.service.FileStorageService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation; // Thư mục gốc (ví dụ: ./uploads/images)

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            "png", "jpg", "jpeg", "gif", "bmp", "webp","heic", "heif"
    );

    public FileStorageServiceImpl(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new InternalServerException("Không thể tạo thư mục để lưu trữ file: " + ex.getMessage());
        }
    }

    private boolean isImageFile(MultipartFile file) {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    @Override
    public String storeImageFile(MultipartFile file, String subDirectory) {
        try {
            if (file.isEmpty()) {
                throw new BadRequestException("File rỗng, không thể lưu.");
            }
            if (!isImageFile(file)) {
                throw new BadRequestException("File không hợp lệ. Chỉ chấp nhận file ảnh (png, jpg, jpeg, gif, bmp).");
            }

            Path finalStorageLocation;
            String relativePathPrefix;

            // Xử lý trường hợp subDirectory rỗng hoặc null
            if (subDirectory == null || subDirectory.trim().isEmpty()) {
                // 1. Nếu không có thư mục con -> lưu vào gốc
                finalStorageLocation = this.fileStorageLocation;
                relativePathPrefix = ""; // Không có tiền tố
            } else {
                // 2. Nếu có thư mục con
                finalStorageLocation = this.fileStorageLocation.resolve(subDirectory).normalize();
                relativePathPrefix = subDirectory + "/";

                if (!Files.exists(finalStorageLocation)) {
                    Files.createDirectories(finalStorageLocation);
                }
            }

            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString() + "." + extension;

            // Lưu file vào đúng vị trí (gốc hoặc thư mục con)
            Path targetLocation = finalStorageLocation.resolve(fileName).normalize().toAbsolutePath();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // Trả về đường dẫn tương đối
            return relativePathPrefix + fileName;

        } catch (IOException ex) {
            throw new InternalServerException("Không thể lưu trữ file. Vui lòng thử lại!: " + ex.getMessage());
        } catch (BadRequestException ex) {
            throw ex;
        }
    }
    @Override
    public void deleteFile(String relativeFilePath) {
        // Nếu đường dẫn rỗng hoặc null, không làm gì cả
        if (relativeFilePath == null || relativeFilePath.trim().isEmpty()) {
            return;
        }

        try {
            // Tạo đường dẫn tuyệt đối (ví dụ: /app/uploads/images/avatars/abc.png)
            Path fullPath = this.fileStorageLocation.resolve(relativeFilePath).normalize();

            // === KIỂM TRA BẢO MẬT (Path Traversal) ===
            // Đảm bảo file bị xóa phải nằm BÊN TRONG thư mục upload
            if (!fullPath.startsWith(this.fileStorageLocation.normalize())) {
                System.err.println("Cảnh báo bảo mật: Cố gắng xóa file bên ngoài thư mục upload: " + relativeFilePath);
                return;
            }

            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                System.out.println("Đã xóa file cũ thành công: " + relativeFilePath);
            }
        } catch (InvalidPathException e) {
            System.err.println("Đường dẫn file cũ không hợp lệ, không thể xóa: " + relativeFilePath + " - " + e.getMessage());
        } catch (IOException e) {
            // Không ném lỗi ra ngoài, vì hành động chính (upload) đã thành công
            // Chỉ ghi log lại
            System.err.println("Không thể xóa file cũ: " + relativeFilePath + " - " + e.getMessage());
        }
    }

    @Override
    public String storeImageFile(MultipartFile file) {
        // Gọi hàm 2 tham số, truyền vào thư mục con là rỗng ("")
        return storeImageFile(file, "");
    }
}