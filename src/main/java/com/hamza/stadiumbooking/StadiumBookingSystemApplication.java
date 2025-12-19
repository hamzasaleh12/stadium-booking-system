package com.hamza.stadiumbooking;

import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;


@SpringBootApplication @EnableScheduling @EnableCaching
public class StadiumBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(StadiumBookingSystemApplication.class, args);
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    @Bean
    CommandLineRunner runDatabaseSeeder(
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (userRepository.findByEmailAndIsDeletedFalse("admin@gmail.com").isEmpty()) {
                User admin = new User();
                admin.setName("Super Admin");
                admin.setEmail("admin@gmail.com"); // ✅ Gmail
                admin.setPassword(passwordEncoder.encode("Admin@1234")); // ✅ Strong Password
                admin.setPhoneNumber("01012345678"); // ✅ Valid Phone
                admin.setRole(Role.ROLE_ADMIN);
                admin.setDob(LocalDate.of(1990, 1, 1));
                userRepository.save(admin);
                System.out.println("✅ Admin Seeded: admin@gmail.com / Admin@1234");
            }
        };
    }
}
