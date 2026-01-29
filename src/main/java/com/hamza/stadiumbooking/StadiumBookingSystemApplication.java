package com.hamza.stadiumbooking;

import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@Slf4j
@EnableCaching
@EnableJpaAuditing
public class StadiumBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(StadiumBookingSystemApplication.class, args);
    }

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"));
        log.info("🚀 Application Started! Current TimeZone: Africa/Cairo | Time: {} ", java.time.LocalDateTime.now());
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CommandLineRunner runDatabaseSeeder(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            try {
                log.info("⏳ Waiting for database to stabilize (15s)...");
                Thread.sleep(15000);

                seedUserIfNotExist(userRepository, passwordEncoder, "Super Admin", "admin@gmail.com", "Admin@1234", "01012345678", Role.ROLE_ADMIN);
                seedUserIfNotExist(userRepository, passwordEncoder, "Manager User", "manager@gmail.com", "Manager@1234", "01022345678", Role.ROLE_MANAGER);
                seedUserIfNotExist(userRepository, passwordEncoder, "Player User", "player@gmail.com", "Player@1234", "01032345678", Role.ROLE_PLAYER);
                log.info("✅ Database Seeding process finished.");
            } catch (Exception e) {
                log.warn("⚠️ Seeder skipped: Tables might not be ready yet. Error: {}", e.getMessage());
            }
        };
    }

    private void seedUserIfNotExist(UserRepository repo, BCryptPasswordEncoder encoder,
                                    String name, String email, String password, String phone, Role role) {
        if (repo.findByEmailAndIsDeletedFalse(email).isEmpty()) {
            User user = User.builder()
                    .name(name)
                    .email(email)
                    .password(encoder.encode(password))
                    .phoneNumber(phone)
                    .role(role)
                    .dob(LocalDate.of(1995, 1, 1))
                    .isDeleted(false)
                    .build();

            repo.save(user);
            log.info("✅ {} Seeded: {} / {}", role, email, password);
        }
    }
}