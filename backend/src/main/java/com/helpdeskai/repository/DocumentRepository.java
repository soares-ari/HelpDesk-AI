package com.helpdeskai.repository;

import com.helpdeskai.entity.Document;
import com.helpdeskai.entity.Document.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para Document
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Busca documentos de um usuário
     *
     * @param userId ID do usuário
     * @return Lista de documentos
     */
    List<Document> findByUserId(Long userId);

    /**
     * Busca documentos por status
     *
     * @param status Status do documento
     * @return Lista de documentos
     */
    List<Document> findByStatus(DocumentStatus status);

    /**
     * Busca documentos de um usuário com status específico
     *
     * @param userId ID do usuário
     * @param status Status do documento
     * @return Lista de documentos
     */
    List<Document> findByUserIdAndStatus(Long userId, DocumentStatus status);

    /**
     * Conta documentos de um usuário
     *
     * @param userId ID do usuário
     * @return Número de documentos
     */
    long countByUserId(Long userId);

    /**
     * Busca documentos completados de um usuário
     *
     * @param userId ID do usuário
     * @return Lista de documentos completados
     */
    @Query("SELECT d FROM Document d WHERE d.user.id = :userId AND d.status = 'COMPLETED' ORDER BY d.uploadedAt DESC")
    List<Document> findCompletedDocumentsByUserId(@Param("userId") Long userId);
}
