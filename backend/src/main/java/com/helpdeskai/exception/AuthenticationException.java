package com.helpdeskai.exception;

/**
 * Exceção lançada quando ocorre erro de autenticação.
 * Inclui credenciais inválidas, token JWT expirado/inválido, etc.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
