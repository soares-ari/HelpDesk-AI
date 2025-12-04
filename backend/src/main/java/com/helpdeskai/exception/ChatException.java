package com.helpdeskai.exception;

/**
 * Exceção lançada quando ocorre erro durante o processamento de chat.
 * Inclui erros no pipeline RAG, busca vetorial, ou chamada ao LLM.
 */
public class ChatException extends RuntimeException {

    public ChatException(String message) {
        super(message);
    }

    public ChatException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construtor conveniente para erros relacionados a uma conversa específica.
     *
     * @param conversationId ID da conversa onde ocorreu o erro
     * @param message Mensagem de erro descritiva
     */
    public ChatException(Long conversationId, String message) {
        super(String.format("Erro na conversa ID %d: %s", conversationId, message));
    }

    /**
     * Construtor conveniente para erros relacionados a uma conversa específica com causa.
     *
     * @param conversationId ID da conversa onde ocorreu o erro
     * @param message Mensagem de erro descritiva
     * @param cause Causa raiz do erro
     */
    public ChatException(Long conversationId, String message, Throwable cause) {
        super(String.format("Erro na conversa ID %d: %s", conversationId, message), cause);
    }
}
