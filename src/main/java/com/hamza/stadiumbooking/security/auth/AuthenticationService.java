package com.hamza.stadiumbooking.security.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import com.hamza.stadiumbooking.security.jwt.JwtProvider;
import com.hamza.stadiumbooking.security.service.CustomUserDetails;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    public AuthenticationResponse refreshToken(String refreshToken) {
        DecodedJWT decodedJWT = jwtProvider.decodedJWT(refreshToken, "REFRESH");

        String email = decodedJWT.getSubject();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtProvider.createAccessToken(
                user.getEmail(),
                user.getId(),
                user.isDeleted(),
                List.of(user.getRole().name())
        );

        return new AuthenticationResponse(newAccessToken);
    }


    public InternalAuthResult login(LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = jwtProvider.createAccessToken(
                user.getUsername(),
                user.getId(),
                !user.isEnabled(),
                roles
        );

        String refreshToken = jwtProvider.createRefreshToken(user.getUsername());

        return new InternalAuthResult(accessToken, refreshToken);
    }
}