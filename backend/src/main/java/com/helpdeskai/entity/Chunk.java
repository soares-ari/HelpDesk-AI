package com.helpdeskai.entity;

import com.helpdeskai.config.VectorType;
import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entidade Chunk - Representa um pedaço de texto com embedding vetorial
 * Esta é a entidade core do sistema RAG
 */
@Entity
@Table(name = "chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Embedding vetorial (1536 dimensões para text-embedding-3-small)
     * Usa pgvector para armazenamento e busca eficiente
     */
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    @Type(VectorType.class)
    private PGvector embedding;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    /**
     * Metadata em formato JSON
     * Exemplo: {page: 5, section: "API Reference", startChar: 1000, endChar: 3500}
     */
    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private ChunkMetadata metadata;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Classe interna para metadata estruturada
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChunkMetadata {
        private Integer page;
        private String section;
        private Integer startChar;
        private Integer endChar;
        private String documentType;
        private String language;
        private Boolean hasCodeBlock;
    }
}
