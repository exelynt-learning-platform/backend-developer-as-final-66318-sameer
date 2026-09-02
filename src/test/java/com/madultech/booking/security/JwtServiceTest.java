package com.madultech.booking.security;

import com.madultech.booking.entity.User;
import com.madultech.booking.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "VGhpc0lzQVRlc3RTZWNyZXRLZXlGb3JIUzI1NkFsZ29yaXRobUFuZFNob3VsZE5ldmVyQmVDb21taXR0ZWQhMTIz");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);
    }

    private User sampleUser(String username, Role role) {
        return User.builder()
                .id(1L)
                .username(username)
                .password("irrelevant")
                .email(username + "@test.com")
                .role(role)
                .enabled(true)
                .build();
    }

    @Test
    void generatesTokenContainingUsernameAndRole() {
        User user = sampleUser("alice", Role.USER);

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void validatesTokenForMatchingUser() {
        User user = sampleUser("bob", Role.ADMIN);
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void rejectsTokenForDifferentUser() {
        User bob = sampleUser("bob", Role.ADMIN);
        User carol = sampleUser("carol", Role.USER);
        String token = jwtService.generateToken(bob);

        assertThat(jwtService.isTokenValid(token, carol)).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        User user = sampleUser("dave", Role.USER);
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isFalse();
    }
}
