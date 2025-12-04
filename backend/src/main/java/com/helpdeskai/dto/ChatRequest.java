package com.helpdeskai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de chat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    @NotBlank(message = "Mensagem é obrigatória")
    @Size(max = 2000, message = "Mensagem não pode ter mais de 2000 caracteres")
    private String message;

    /**
     * ID da conversa (opcional)
     * Se não fornecido, uma nova conversa será criada
     */
    private Long conversationId;
}
