package com.hamza.stadiumbooking.security.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import com.hamza.stadiumbooking.security.jwt.JwtUtils;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        DecodedJWT decodedJWT = jwtUtils.decodedJWT(request.token());
        String email = decodedJWT.getSubject();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<String> roles = List.of(user.getRole().name());
        String newAccessToken = jwtUtils.createAccessToken(
                user.getEmail(),
                user.getId(),
                decodedJWT.getIssuer(),
                roles
        );
        return new AuthenticationResponse(newAccessToken, request.token());
    }
}
