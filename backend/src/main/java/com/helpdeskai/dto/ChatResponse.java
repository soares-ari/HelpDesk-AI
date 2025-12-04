package com.helpdeskai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para resposta de chat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    private String message;
    private Long conversationId;
    private List<CitationDTO> citations;
    private LocalDateTime timestamp;

    /**
     * DTO para citações (chunks usados)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CitationDTO {
        private Long chunkId;
        private String content;
        private Double similarityScore;
        private MetadataDTO metadata;
    }

    /**
     * DTO para metadata do chunk
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetadataDTO {
        private Integer page;
        private String section;
        private String documentName;
        private Long documentId;
    }
}
