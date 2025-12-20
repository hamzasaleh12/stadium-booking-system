package com.hamza.stadiumbooking.welcome;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {

    @GetMapping("/")
    public String welcome() {
        return """
                Welcome to Stadium Booking API 🚀

                Health Check: /actuator/health
                API Docs: /swagger-ui/index.html
                """;
    }
}