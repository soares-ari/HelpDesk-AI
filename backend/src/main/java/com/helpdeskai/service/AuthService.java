package com.helpdeskai.service;

import com.helpdeskai.dto.AuthRequest;
import com.helpdeskai.dto.AuthResponse;
import com.helpdeskai.dto.RegisterRequest;
import com.helpdeskai.entity.User;
import com.helpdeskai.exception.AuthenticationException;
import com.helpdeskai.exception.ResourceNotFoundException;
import com.helpdeskai.repository.UserRepository;
import com.helpdeskai.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service responsible for user authentication and registration.
 */
@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registra um novo usuário no sistema.
     *
     * @param request Dados de registro
     * @return Resposta com token JWT e informações do usuário
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Tentativa de registro para email: {}", request.getEmail());

        // Verificar se email já existe
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Tentativa de registro com email já existente: {}", request.getEmail());
            throw new AuthenticationException("Email já está em uso");
        }

        // Validar senha (opcional - pode adicionar regras mais complexas)
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new AuthenticationException("Senha deve ter pelo menos 6 caracteres");
        }

        // Hash da senha com BCrypt
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Criar usuário
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .createdAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        log.info("Usuário registrado com sucesso. ID: {}, Email: {}", user.getId(), user.getEmail());

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    /**
     * Autentica um usuário existente.
     *
     * @param request Credenciais de login
     * @return Resposta com token JWT e informações do usuário
     */
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        log.info("Tentativa de login para email: {}", request.getEmail());

        // Buscar usuário por email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Tentativa de login com email não encontrado: {}", request.getEmail());
                    return new AuthenticationException("Credenciais inválidas");
                });

        // Verificar senha
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Tentativa de login com senha incorreta para: {}", request.getEmail());
            throw new AuthenticationException("Credenciais inválidas");
        }

        log.info("Login bem-sucedido para usuário ID: {}", user.getId());

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    /**
     * Gets user from JWT token.
     *
     * @param token JWT token
     * @return User entity
     */
    @Transactional(readOnly = true)
    public User getUserFromToken(String token) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * Validates a JWT token.
     *
     * @param token JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }
}
