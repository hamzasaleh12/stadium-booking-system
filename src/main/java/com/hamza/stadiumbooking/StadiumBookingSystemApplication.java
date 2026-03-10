package com.hamza.stadiumbooking;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
}
