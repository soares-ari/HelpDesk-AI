package com.helpdeskai.service;

import com.helpdeskai.dto.AuthRequest;
import com.helpdeskai.dto.AuthResponse;
import com.helpdeskai.dto.RegisterRequest;
import com.helpdeskai.entity.User;
import com.helpdeskai.exception.AuthenticationException;
import com.helpdeskai.exception.ResourceNotFoundException;
import com.helpdeskai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Serviço responsável por autenticação e registro de usuários.
 *
 * NOTA: Este service depende de JwtTokenProvider que ainda não foi implementado.
 * Por enquanto, os métodos que precisam de JWT estão com TODOs marcados.
 */
@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // TODO: Adicionar JwtTokenProvider quando implementar camada de security
    // private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        // TODO: Gerar token JWT quando JwtTokenProvider estiver implementado
        String token = generateTemporaryToken(user);

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

        // TODO: Gerar token JWT quando JwtTokenProvider estiver implementado
        String token = generateTemporaryToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    /**
     * Obtém usuário a partir de um token JWT.
     *
     * @param token Token JWT
     * @return Usuário correspondente
     */
    @Transactional(readOnly = true)
    public User getUserFromToken(String token) {
        // TODO: Implementar quando JwtTokenProvider estiver pronto
        // Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // Por enquanto, retorna erro
        throw new AuthenticationException(
                "Método getUserFromToken não implementado - aguardando JwtTokenProvider");
    }

    /**
     * Valida se um token JWT é válido.
     *
     * @param token Token JWT
     * @return true se válido, false caso contrário
     */
    public boolean validateToken(String token) {
        // TODO: Implementar quando JwtTokenProvider estiver pronto
        // return jwtTokenProvider.validateToken(token);

        // Por enquanto, retorna false
        log.warn("Método validateToken não implementado - aguardando JwtTokenProvider");
        return false;
    }

    /**
     * MÉTODO TEMPORÁRIO: Gera um "token" simples para testes.
     * DEVE SER REMOVIDO quando JwtTokenProvider estiver implementado.
     *
     * @param user Usuário
     * @return Token temporário (não é JWT real)
     */
    private String generateTemporaryToken(User user) {
        // ATENÇÃO: Isto NÃO é seguro! É apenas para permitir compilação e testes básicos
        // Um JWT real deve ser gerado com jjwt library
        String tempToken = String.format("TEMP_TOKEN_%d_%s", user.getId(), user.getEmail());

        log.warn("⚠️ USANDO TOKEN TEMPORÁRIO NÃO SEGURO: {}", tempToken);
        log.warn("⚠️ IMPLEMENTAR JwtTokenProvider ANTES DE USAR EM PRODUÇÃO!");

        return tempToken;
    }
}
