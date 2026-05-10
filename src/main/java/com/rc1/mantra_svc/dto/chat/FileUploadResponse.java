package com.rc1.mantra_svc.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for a file upload. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FileUploadResponse {
    private String fileId;
    private String originalName;
    private String contentType;
    private long sizeBytes;
    private String url;
    private String thumbnailUrl;
}
