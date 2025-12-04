package com.helpdeskai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidade Message - Representa uma mensagem no chat (user ou assistant)
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageRole role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Citações (chunks usados para gerar a resposta)
     * Armazenado como JSONB no PostgreSQL
     */
    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Citation> citations;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Enum para tipo de mensagem
     */
    public enum MessageRole {
        USER,       // Mensagem do usuário
        ASSISTANT   // Mensagem do assistente (IA)
    }

    /**
     * Classe interna para citações
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Citation {
        private Long chunkId;
        private String content;
        private Double similarityScore;
        private ChunkMetadataDTO metadata;
    }

    /**
     * DTO para metadata do chunk (usado nas citações)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkMetadataDTO {
        private Long documentId;
        private String documentName;
        private Integer page;
        private String section;
    }
}
