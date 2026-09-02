package com.madultech.booking.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madultech.booking.entity.Resource;
import com.madultech.booking.entity.User;
import com.madultech.booking.enums.Role;
import com.madultech.booking.repository.ReservationRepository;
import com.madultech.booking.repository.ResourceRepository;
import com.madultech.booking.repository.UserRepository;
import com.madultech.booking.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ResourceRepository resourceRepository;

    @Autowired
    protected ReservationRepository reservationRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    protected User adminUser;
    protected User regularUser;
    protected User secondRegularUser;
    protected Resource sampleResource;

    protected String adminToken;
    protected String userToken;
    protected String secondUserToken;

    @BeforeEach
    void seedBaseData() {
        // Reservations must go first — they hold FK references to users and resources.
        reservationRepository.deleteAll();
        userRepository.deleteAll();
        resourceRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin@123"))
                .email("admin@test.com")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        regularUser = userRepository.save(User.builder()
                .username("alice")
                .password(passwordEncoder.encode("Alice@123"))
                .email("alice@test.com")
                .role(Role.USER)
                .enabled(true)
                .build());

        secondRegularUser = userRepository.save(User.builder()
                .username("bob")
                .password(passwordEncoder.encode("Bob@123"))
                .email("bob@test.com")
                .role(Role.USER)
                .enabled(true)
                .build());

        sampleResource = resourceRepository.save(Resource.builder()
                .name("Test Room")
                .type("ROOM")
                .description("A room for testing")
                .location("Test Building")
                .available(true)
                .build());

        adminToken = jwtService.generateToken(adminUser);
        userToken = jwtService.generateToken(regularUser);
        secondUserToken = jwtService.generateToken(secondRegularUser);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
