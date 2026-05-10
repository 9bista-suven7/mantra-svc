package com.rc1.mantra_svc.service.chat;

import com.rc1.mantra_svc.config.AppProperties;
import com.rc1.mantra_svc.dto.chat.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Handles file/image uploads for chat attachments.
 * Files are stored in app.upload.dir and served via /api/chat/files/{fileId}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AppProperties appProperties;

    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final java.util.Set<String> ALLOWED_TYPES = java.util.Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain"
    );

    public Mono<FileUploadResponse> store(FilePart filePart) {
        String contentType = filePart.headers().getContentType() != null
            ? filePart.headers().getContentType().toString() : "application/octet-stream";

        if (!ALLOWED_TYPES.contains(contentType)) {
            return Mono.error(new IllegalArgumentException("File type not allowed: " + contentType));
        }

        String fileId = UUID.randomUUID().toString();
        String extension = getExtension(filePart.filename());
        String storedName = fileId + (extension.isEmpty() ? "" : "." + extension);

        return Mono.fromCallable(() -> {
            Path uploadDir = Paths.get(appProperties.getUpload().getDir());
            Files.createDirectories(uploadDir);
            return uploadDir.resolve(storedName);
        })
        .flatMap(destPath -> DataBufferUtils.write(filePart.content(), destPath)
            .then(Mono.fromCallable(() -> Files.size(destPath)))
            .map(size -> {
                if (size > MAX_BYTES) {
                    try { Files.delete(destPath); } catch (Exception ignored) {}
                    throw new IllegalArgumentException("File exceeds 10 MB limit");
                }
                boolean isImage = contentType.startsWith("image/");
                String url = "/api/chat/files/" + fileId;
                return FileUploadResponse.builder()
                    .fileId(fileId)
                    .originalName(filePart.filename())
                    .contentType(contentType)
                    .sizeBytes(size)
                    .url(url)
                    .thumbnailUrl(isImage ? url : null)
                    .build();
            }))
        .onErrorMap(e -> {
            log.error("File upload failed: {}", e.getMessage());
            return e;
        });
    }

    public Path resolve(String fileId) {
        Path dir = Paths.get(appProperties.getUpload().getDir());
        try {
            return Files.list(dir)
                .filter(p -> p.getFileName().toString().startsWith(fileId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(fileId));
        } catch (java.io.IOException e) {
            throw new ResourceNotFoundException(fileId);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    // local helper to avoid import
    static class ResourceNotFoundException extends RuntimeException {
        ResourceNotFoundException(String id) { super("File not found: " + id); }
    }
}
