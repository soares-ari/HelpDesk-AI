package com.helpdeskai.repository;

import com.helpdeskai.entity.Chunk;
import com.pgvector.PGvector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para Chunk com suporte a busca vetorial (pgvector)
 */
@Repository
public interface ChunkRepository extends JpaRepository<Chunk, Long> {

    /**
     * Busca chunks por similaridade vetorial usando cosine similarity
     *
     * O operador <=> do pgvector calcula a distância de cosine
     * Menor distância = maior similaridade
     * Score de similaridade = 1 - distância
     *
     * @param queryEmbedding Embedding da pergunta do usuário
     * @param topK Número de chunks mais similares a retornar
     * @param threshold Score mínimo de similaridade (0.0 a 1.0)
     * @return Lista de chunks ordenados por similaridade (maior primeiro)
     */
    @Query(value = """
        SELECT c.*,
               1 - (c.embedding <=> CAST(:queryEmbedding AS vector)) AS similarity_score
        FROM chunks c
        WHERE 1 - (c.embedding <=> CAST(:queryEmbedding AS vector)) >= :threshold
        ORDER BY c.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<Object[]> findSimilarChunks(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("topK") int topK,
        @Param("threshold") double threshold
    );

    /**
     * Busca simplificada de chunks similares (usando parâmetros padrão)
     * Retorna apenas ID, conteúdo e metadata (sem embedding para evitar erros de conversão)
     *
     * @param queryEmbedding Embedding da pergunta
     * @param topK Número de resultados
     * @return Lista de arrays com [id, content, metadata, chunk_index, document_id, created_at, document_filename]
     */
    @Query(value = """
        SELECT c.id, c.content, c.metadata, c.chunk_index, c.document_id, c.created_at, d.filename
        FROM chunks c
        JOIN documents d ON c.document_id = d.id
        ORDER BY c.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<Object[]> findTopKSimilarChunks(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("topK") int topK
    );

    /**
     * Busca todos os chunks de um documento específico
     *
     * @param documentId ID do documento
     * @return Lista de chunks ordenados por índice
     */
    @Query("SELECT c FROM Chunk c WHERE c.document.id = :documentId ORDER BY c.chunkIndex")
    List<Chunk> findByDocumentId(@Param("documentId") Long documentId);

    /**
     * Conta chunks de um documento
     *
     * @param documentId ID do documento
     * @return Número de chunks
     */
    long countByDocumentId(Long documentId);

    /**
     * Deleta todos os chunks de um documento
     * Útil para reindexar um documento
     *
     * @param documentId ID do documento
     */
    @Modifying
    @Query("DELETE FROM Chunk c WHERE c.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * Verifica se existe algum chunk com embedding nulo
     * Útil para debug
     *
     * @return true se existe chunk sem embedding
     */
    @Query("SELECT COUNT(c) > 0 FROM Chunk c WHERE c.embedding IS NULL")
    boolean existsChunkWithoutEmbedding();
}
