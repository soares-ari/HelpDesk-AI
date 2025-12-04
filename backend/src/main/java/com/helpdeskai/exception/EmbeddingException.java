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

    /**
     * Construtor conveniente para erros de API OpenAI.
     *
     * @param operation Operação que falhou (ex: "geração de embedding")
     * @param cause Causa raiz do erro
     */
    public EmbeddingException(String operation, Throwable cause) {
        super(String.format("Erro na %s: %s", operation,
                          cause.getMessage() != null ? cause.getMessage() : "Erro desconhecido"),
              cause);
    }
}
