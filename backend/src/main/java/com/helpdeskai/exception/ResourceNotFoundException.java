package com.helpdeskai.exception;

/**
 * Exceção lançada quando um recurso solicitado não é encontrado no sistema.
 * Por exemplo: Document, User, Conversation ou Chunk não existente.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construtor conveniente para recursos não encontrados por ID.
     *
     * @param resourceType Tipo do recurso (ex: "Document", "User")
     * @param resourceId ID do recurso não encontrado
     */
    public ResourceNotFoundException(String resourceType, Long resourceId) {
        super(String.format("%s com ID %d não encontrado", resourceType, resourceId));
    }

    /**
     * Construtor conveniente para recursos não encontrados por campo específico.
     *
     * @param resourceType Tipo do recurso
     * @param fieldName Nome do campo
     * @param fieldValue Valor do campo
     */
    public ResourceNotFoundException(String resourceType, String fieldName, Object fieldValue) {
        super(String.format("%s com %s '%s' não encontrado", resourceType, fieldName, fieldValue));
    }
}
