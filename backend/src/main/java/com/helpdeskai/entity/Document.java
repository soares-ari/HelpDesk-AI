package com.helpdeskai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade Document - Representa um documento PDF indexado
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "total_chunks")
    private Integer totalChunks;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        if (status == null) {
            status = DocumentStatus.PROCESSING;
        }
    }

    /**
     * Enum para status do documento
     */
    public enum DocumentStatus {
        PROCESSING,  // Em processamento
        COMPLETED,   // Processamento concluído com sucesso
        FAILED       // Falha no processamento
    }
}
