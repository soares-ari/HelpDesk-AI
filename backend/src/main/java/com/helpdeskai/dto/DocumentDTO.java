package com.helpdeskai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para representar um documento
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDTO {

    private Long id;
    private String filename;
    private Long fileSize;
    private String mimeType;
    private Integer totalChunks;
    private String status;
    private LocalDateTime uploadedAt;
}
