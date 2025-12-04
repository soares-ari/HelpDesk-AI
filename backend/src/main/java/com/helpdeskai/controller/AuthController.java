package com.helpdeskai.controller;

import com.helpdeskai.dto.AuthRequest;
import com.helpdeskai.dto.AuthResponse;
import com.helpdeskai.dto.RegisterRequest;
import com.helpdeskai.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * Handles user registration and login.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     *
     * @param request Registration data
     * @return JWT token and user info
     */
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new user account and returns JWT token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());

        AuthResponse response = authService.register(request);

        log.info("User registered successfully: {}", response.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate existing user (login).
     *
     * @param request Login credentials
     * @return JWT token and user info
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and returns JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login request received for email: {}", request.getEmail());

        AuthResponse response = authService.login(request);

        log.info("User logged in successfully: {}", response.getEmail());

        return ResponseEntity.ok(response);
    }

    /**
     * Validate JWT token (optional endpoint for frontend).
     *
     * @param authorization Authorization header with Bearer token
     * @return Validation result
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token", description = "Checks if provided JWT token is valid")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestHeader("Authorization") String authorization) {

        String token = extractTokenFromHeader(authorization);

        if (token == null) {
            return ResponseEntity.badRequest()
                    .body(new TokenValidationResponse(false, "No token provided"));
        }

        boolean isValid = authService.validateToken(token);

        return ResponseEntity.ok(new TokenValidationResponse(isValid,
                isValid ? "Token is valid" : "Token is invalid or expired"));
    }

    /**
     * Extract JWT token from Authorization header.
     */
    private String extractTokenFromHeader(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    /**
     * DTO for token validation response.
     */
    private record TokenValidationResponse(boolean valid, String message) {}
}
