package com.helpdeskai.exception;

/**
 * Exceção lançada quando ocorre erro durante o processamento de documentos.
 * Inclui erros na extração de texto (Tika), chunking ou armazenamento.
 */
public class DocumentProcessingException extends RuntimeException {

    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construtor conveniente para erros relacionados a um documento específico.
     *
     * @param documentId ID do documento que falhou no processamento
     * @param message Mensagem de erro descritiva
     */
    public DocumentProcessingException(Long documentId, String message) {
        super(String.format("Erro ao processar documento ID %d: %s", documentId, message));
    }

    /**
     * Construtor conveniente para erros relacionados a um documento específico com causa.
     *
     * @param documentId ID do documento que falhou no processamento
     * @param message Mensagem de erro descritiva
     * @param cause Causa raiz do erro
     */
    public DocumentProcessingException(Long documentId, String message, Throwable cause) {
        super(String.format("Erro ao processar documento ID %d: %s", documentId, message), cause);
    }
}
