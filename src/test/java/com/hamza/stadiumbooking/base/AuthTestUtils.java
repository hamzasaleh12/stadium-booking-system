package com.hamza.stadiumbooking.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamza.stadiumbooking.security.auth.LoginRequest;
import com.hamza.stadiumbooking.stadium.StadiumRequest;
import com.hamza.stadiumbooking.stadium.Type;
import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import com.hamza.stadiumbooking.user.UserRequest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Component
@RequiredArgsConstructor
public class AuthTestUtils {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String API_V1_LOGIN = "/api/v1/auth/login";

    public User saveUser(String email, String rawPassword, String phoneNumber, Role role) {
        User user = User.builder()
                .name("Test User")
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .phoneNumber(phoneNumber)
                .dob(LocalDate.of(1995, 1, 1))
                .role(role)
                .isDeleted(false)
                .build();
        return userRepository.save(user);
    }

    public User savePlayer(String email, String rawPassword, String phoneNumber) {
        return saveUser(email, rawPassword, phoneNumber, Role.ROLE_PLAYER);
    }

    public UserRequest createUserRequest(String email, String phoneNumber) {
        return new UserRequest("hamza", email, phoneNumber, "Hamza@1234", LocalDate.of(2000, 1, 1));
    }

    public String obtainAccessToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post(API_V1_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");
    }

    public Cookie obtainRefreshToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post(API_V1_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andReturn();
        return Objects.requireNonNull(result.getResponse().getCookie("refresh_token"));
    }

    public StadiumRequest createStadiumRequest(String name) {
        return new StadiumRequest(
                name, "Cairo, Egypt", 250.0, 50,
                LocalTime.of(18, 0), LocalTime.of(23, 0),
                Set.of("WiFi", "Locker Room"), Type.SEVEN_A_SIDE, "https://photo.com"
        );
    }
}
