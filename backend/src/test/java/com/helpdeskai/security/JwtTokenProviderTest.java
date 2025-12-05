package com.helpdeskai.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider buildProvider(String secret, long expirationMs) {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", secret);
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", expirationMs);
        ReflectionTestUtils.setField(provider, "issuer", "helpdesk-ai");
        ReflectionTestUtils.setField(provider, "audience", "helpdesk-ai-users");
        return provider;
    }

    @Test
    void generateAndValidateToken_shouldReturnClaims() {
        JwtTokenProvider provider = buildProvider(
                "super-secret-key-32-bytes-minimum-123456",
                3_600_000L
        );

        String token = provider.generateToken(42L, "user@test.com");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserIdFromToken(token)).isEqualTo(42L);
        assertThat(provider.getEmailFromToken(token)).isEqualTo("user@test.com");
    }

    @Test
    void validateToken_shouldFailWithDifferentSecret() {
        JwtTokenProvider providerA = buildProvider(
                "super-secret-key-32-bytes-minimum-123456",
                3_600_000L
        );
        String token = providerA.generateToken(1L, "user@test.com");

        JwtTokenProvider providerB = buildProvider(
                "another-secret-32-bytes-minimum-abcdef",
                3_600_000L
        );

        assertThat(providerB.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_shouldFailWhenExpired() {
        JwtTokenProvider provider = buildProvider(
                "super-secret-key-32-bytes-minimum-123456",
                -1L // already expired
        );

        String token = provider.generateToken(7L, "expired@test.com");

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void getUserIdFromToken_shouldRaiseForMalformedToken() {
        JwtTokenProvider provider = buildProvider(
                "super-secret-key-32-bytes-minimum-123456",
                3_600_000L
        );

        assertThatThrownBy(() -> provider.getUserIdFromToken("not-a-jwt"))
                .isInstanceOf(com.helpdeskai.exception.AuthenticationException.class);
    }
}
