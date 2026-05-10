package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.chat.FileUploadResponse;
import com.rc1.mantra_svc.security.UserPrincipal;
import com.rc1.mantra_svc.service.chat.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

/**
 * File upload and download for chat attachments.
 *
 * POST /api/chat/files          — upload a file, returns FileUploadResponse
 * GET  /api/chat/files/{fileId} — download / inline-display a file
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<FileUploadResponse> upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("file") FilePart file) {
        return fileStorageService.store(file);
    }

    @GetMapping("/{fileId}")
    public Mono<ResponseEntity<Resource>> download(@PathVariable String fileId) {
        return Mono.fromCallable(() -> {
            Path path = fileStorageService.resolve(fileId);
            Resource resource = new FileSystemResource(path);
            String contentType = determineContentType(path.getFileName().toString());
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
        });
    }

    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}
