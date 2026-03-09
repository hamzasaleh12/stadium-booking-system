package com.hamza.stadiumbooking.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamza.stadiumbooking.booking.BookingRepository;
import com.hamza.stadiumbooking.stadium.StadiumRepository;
import com.hamza.stadiumbooking.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory;

    @Autowired
    protected StadiumRepository stadiumRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected BookingRepository bookingRepository;

    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.3")
            .withDatabaseName("testdb");

    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2")
            .withExposedPorts(6379);

    static {
        mysql.start();
        redis.start();
    }

    @BeforeEach
    void cleanup() {
        bookingRepository.deleteAll();
        stadiumRepository.deleteAll();
        userRepository.deleteAll();
        connectionFactory.getConnection().serverCommands().flushAll();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
