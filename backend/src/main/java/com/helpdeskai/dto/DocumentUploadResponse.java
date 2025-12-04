package com.helpdeskai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para resposta de upload de documento
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadResponse {

    private Long documentId;
    private String filename;
    private Long fileSize;
    private String mimeType;
    private String status;
    private Integer totalChunks;
    private LocalDateTime uploadedAt;
    private String message;
}
