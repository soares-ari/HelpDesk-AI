package com.helpdeskai.repository;

import com.helpdeskai.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para Conversation
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Busca conversas de um usuário ordenadas por data (mais recente primeiro)
     *
     * @param userId ID do usuário
     * @return Lista de conversas
     */
    List<Conversation> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Conta conversas de um usuário
     *
     * @param userId ID do usuário
     * @return Número de conversas
     */
    long countByUserId(Long userId);
}
