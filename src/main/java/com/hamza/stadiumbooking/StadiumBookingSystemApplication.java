package com.hamza.stadiumbooking;

import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class StadiumBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(StadiumBookingSystemApplication.class, args);
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Database seeder is now conditional on the environment property:
     * app.seeder.enabled=true
     *
     * Set APP_SEEDER_ENABLED=false in production / constrained environments to skip seeding.
     */
    @Bean
    @ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
    CommandLineRunner runDatabaseSeeder(
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (userRepository.findByEmailAndIsDeletedFalse("admin@gmail.com").isEmpty()) {
                User admin = new User();
                admin.setName("Super Admin");
                admin.setEmail("admin@gmail.com");
                admin.setPassword(passwordEncoder.encode("Admin@1234"));
                admin.setPhoneNumber("01012345678");
                admin.setRole(Role.ROLE_ADMIN);
                admin.setDob(LocalDate.of(1990, 1, 1));
                userRepository.save(admin);
                System.out.println("✅ Admin Seeded: admin@gmail.com / Admin@1234");
            }

            if (userRepository.findByEmailAndIsDeletedFalse("manager@gmail.com").isEmpty()) {
                User manager = new User();
                manager.setName("Manager User");
                manager.setEmail("manager@gmail.com");
                manager.setPassword(passwordEncoder.encode("Manager@1234"));
                manager.setPhoneNumber("01022345678");
                manager.setRole(Role.ROLE_MANAGER);
                manager.setDob(LocalDate.of(1991, 2, 2));
                userRepository.save(manager);
                System.out.println("✅ Manager Seeded: manager@gmail.com / Manager@1234");
            }

            if (userRepository.findByEmailAndIsDeletedFalse("player@gmail.com").isEmpty()) {
                User player = new User();
                player.setName("Player User");
                player.setEmail("player@gmail.com");
                player.setPassword(passwordEncoder.encode("Player@1234"));
                player.setPhoneNumber("01032345678");
                player.setRole(Role.ROLE_PLAYER);
                player.setDob(LocalDate.of(1992, 3, 3));
                userRepository.save(player);
                System.out.println("✅ Player Seeded: player@gmail.com / Player@1234");
            }
        };
    }
}
