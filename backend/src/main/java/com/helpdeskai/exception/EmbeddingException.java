package com.helpdeskai.exception;

/**
 * Exceção lançada quando ocorre erro na geração de embeddings.
 * Inclui erros de comunicação com a API OpenAI, rate limiting, etc.
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
