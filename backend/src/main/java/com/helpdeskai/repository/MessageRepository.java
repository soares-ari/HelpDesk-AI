package com.helpdeskai.repository;

import com.helpdeskai.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para Message
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Busca mensagens de uma conversa ordenadas por data (mais antiga primeiro)
     *
     * @param conversationId ID da conversa
     * @return Lista de mensagens
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Conta mensagens de uma conversa
     *
     * @param conversationId ID da conversa
     * @return Número de mensagens
     */
    long countByConversationId(Long conversationId);

    /**
     * Conta total de mensagens de um usuário (através das conversas)
     *
     * @param userId ID do usuário
     * @return Número total de mensagens
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Busca últimas N mensagens de uma conversa
     *
     * @param conversationId ID da conversa
     * @param limit Número de mensagens
     * @return Lista de mensagens
     */
    @Query(value = "SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Message> findLastNMessages(@Param("conversationId") Long conversationId, @Param("limit") int limit);
}
