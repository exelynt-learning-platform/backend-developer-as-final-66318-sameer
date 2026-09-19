package com.sameer.booking.config;

import com.sameer.booking.entity.Resource;
import com.sameer.booking.entity.User;
import com.sameer.booking.enums.Role;
import com.sameer.booking.repository.ResourceRepository;
import com.sameer.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.admin-username:admin}")
    private String adminUsername;

    @Value("${app.seed.admin-password:Admin@123}")
    private String adminPassword;

    @Value("${app.seed.user-username:user}")
    private String userUsername;

    @Value("${app.seed.user-password:User@123}")
    private String userPassword;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        if (!userRepository.existsByUsername(adminUsername)) {
            userRepository.save(User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .email(adminUsername + "@bookingsystem.local")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build());
            log.info("Seeded ADMIN user '{}'", adminUsername);
        }

        if (!userRepository.existsByUsername(userUsername)) {
            userRepository.save(User.builder()
                    .username(userUsername)
                    .password(passwordEncoder.encode(userPassword))
                    .email(userUsername + "@bookingsystem.local")
                    .role(Role.USER)
                    .enabled(true)
                    .build());
            log.info("Seeded USER user '{}'", userUsername);
        }

        if (resourceRepository.count() == 0) {
            resourceRepository.save(Resource.builder()
                    .name("Conference Room A")
                    .type("ROOM")
                    .description("8-seat conference room with a projector")
                    .location("Building 1, Floor 2")
                    .available(true)
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Toyota Innova - MH12AB1234")
                    .type("VEHICLE")
                    .description("7-seater company vehicle")
                    .location("Basement Parking")
                    .available(true)
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Projector - Epson EB-X05")
                    .type("EQUIPMENT")
                    .description("Portable projector, HDMI + VGA")
                    .location("Equipment Room")
                    .available(true)
                    .build());

            log.info("Seeded 3 sample resources");
        }
    }
}
