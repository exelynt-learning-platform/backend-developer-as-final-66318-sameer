package com.sameer.booking.security;

import com.sameer.booking.entity.User;
import com.sameer.booking.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "MyVeryStrongRandomSecretKeyForResourceBookingSystem2026";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {

        jwtService =new JwtService(TEST_SECRET, 3600000L);
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

        JwtService expiredJwtService = new JwtService(TEST_SECRET, -1000L);

        User user = sampleUser("dave", Role.USER);

        String token = expiredJwtService.generateToken(user);

        assertThat(expiredJwtService.isTokenValid(token, user)).isFalse();
    }
}