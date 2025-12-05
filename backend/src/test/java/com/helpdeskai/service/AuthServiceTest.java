package com.helpdeskai.service;

import com.helpdeskai.dto.AuthRequest;
import com.helpdeskai.dto.AuthResponse;
import com.helpdeskai.dto.RegisterRequest;
import com.helpdeskai.entity.User;
import com.helpdeskai.exception.AuthenticationException;
import com.helpdeskai.exception.ResourceNotFoundException;
import com.helpdeskai.repository.UserRepository;
import com.helpdeskai.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests registration, login, token validation, and error scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private AuthRequest validAuthRequest;
    private User mockUser;
    private String mockToken;

    @BeforeEach
    void setUp() {
        // Setup valid register request
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setName("Test User");
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPassword("password123");

        // Setup valid auth request
        validAuthRequest = new AuthRequest();
        validAuthRequest.setEmail("test@example.com");
        validAuthRequest.setPassword("password123");

        // Setup mock user
        mockUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .createdAt(LocalDateTime.now())
                .build();

        // Setup mock token
        mockToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
    }

    // ===========================
    // Registration Tests
    // ===========================

    @Test
    @DisplayName("Should successfully register new user")
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.getPassword())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtTokenProvider.generateToken(mockUser.getId(), mockUser.getEmail())).thenReturn(mockToken);

        // Act
        AuthResponse response = authService.register(validRegisterRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(mockToken);
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getName()).isEqualTo("Test User");

        // Verify interactions
        verify(userRepository).existsByEmail(validRegisterRequest.getEmail());
        verify(passwordEncoder).encode(validRegisterRequest.getPassword());
        verify(userRepository).save(argThat(user ->
                user.getName().equals("Test User") &&
                user.getEmail().equals("test@example.com") &&
                user.getPasswordHash().equals("$2a$12$hashedPassword")
        ));
        verify(jwtTokenProvider).generateToken(1L, "test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegister_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email já está em uso");

        // Verify no further interactions
        verify(userRepository).existsByEmail(validRegisterRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when password is null")
    void testRegister_NullPassword() {
        // Arrange
        validRegisterRequest.setPassword(null);
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Senha deve ter pelo menos 6 caracteres");

        verify(userRepository).existsByEmail(validRegisterRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should throw exception when password is too short")
    void testRegister_ShortPassword() {
        // Arrange
        validRegisterRequest.setPassword("12345"); // Only 5 characters
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Senha deve ter pelo menos 6 caracteres");

        verify(userRepository).existsByEmail(validRegisterRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
    }

    // ===========================
    // Login Tests
    // ===========================

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLogin_Success() {
        // Arrange
        when(userRepository.findByEmail(validAuthRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(validAuthRequest.getPassword(), mockUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(mockUser.getId(), mockUser.getEmail())).thenReturn(mockToken);

        // Act
        AuthResponse response = authService.login(validAuthRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(mockToken);
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getName()).isEqualTo("Test User");

        // Verify interactions
        verify(userRepository).findByEmail(validAuthRequest.getEmail());
        verify(passwordEncoder).matches(validAuthRequest.getPassword(), mockUser.getPasswordHash());
        verify(jwtTokenProvider).generateToken(1L, "test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testLogin_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(validAuthRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validAuthRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Credenciais inválidas");

        verify(userRepository).findByEmail(validAuthRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).generateToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void testLogin_IncorrectPassword() {
        // Arrange
        when(userRepository.findByEmail(validAuthRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(validAuthRequest.getPassword(), mockUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validAuthRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Credenciais inválidas");

        verify(userRepository).findByEmail(validAuthRequest.getEmail());
        verify(passwordEncoder).matches(validAuthRequest.getPassword(), mockUser.getPasswordHash());
        verify(jwtTokenProvider, never()).generateToken(anyLong(), anyString());
    }

    // ===========================
    // Token Validation Tests
    // ===========================

    @Test
    @DisplayName("Should get user from valid token")
    void testGetUserFromToken_Success() {
        // Arrange
        when(jwtTokenProvider.getUserIdFromToken(mockToken)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // Act
        User result = authService.getUserFromToken(mockToken);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("Test User");

        verify(jwtTokenProvider).getUserIdFromToken(mockToken);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when user not found from token")
    void testGetUserFromToken_UserNotFound() {
        // Arrange
        when(jwtTokenProvider.getUserIdFromToken(mockToken)).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.getUserFromToken(mockToken))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("999");

        verify(jwtTokenProvider).getUserIdFromToken(mockToken);
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("Should validate valid token")
    void testValidateToken_ValidToken() {
        // Arrange
        when(jwtTokenProvider.validateToken(mockToken)).thenReturn(true);

        // Act
        boolean result = authService.validateToken(mockToken);

        // Assert
        assertThat(result).isTrue();
        verify(jwtTokenProvider).validateToken(mockToken);
    }

    @Test
    @DisplayName("Should invalidate invalid token")
    void testValidateToken_InvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.here";
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // Act
        boolean result = authService.validateToken(invalidToken);

        // Assert
        assertThat(result).isFalse();
        verify(jwtTokenProvider).validateToken(invalidToken);
    }

    // ===========================
    // Edge Case Tests
    // ===========================

    @Test
    @DisplayName("Should handle empty email in registration")
    void testRegister_EmptyEmail() {
        // Arrange
        validRegisterRequest.setEmail("");
        when(userRepository.existsByEmail("")).thenReturn(false);

        // Act & Assert - Should still process but likely fail at DB level
        // This tests that service layer doesn't crash
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtTokenProvider.generateToken(anyLong(), anyString())).thenReturn(mockToken);

        AuthResponse response = authService.register(validRegisterRequest);

        assertThat(response).isNotNull();
        verify(userRepository).existsByEmail("");
    }

    @Test
    @DisplayName("Should handle password with exactly 6 characters")
    void testRegister_MinimumPasswordLength() {
        // Arrange
        validRegisterRequest.setPassword("123456"); // Exactly 6 characters
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtTokenProvider.generateToken(anyLong(), anyString())).thenReturn(mockToken);

        // Act
        AuthResponse response = authService.register(validRegisterRequest);

        // Assert
        assertThat(response).isNotNull();
        verify(passwordEncoder).encode("123456");
    }
}
