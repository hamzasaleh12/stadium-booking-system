package com.hamza.stadiumbooking.security.config;

import com.hamza.stadiumbooking.security.jwt.CustomAuthenticationFilter;
import com.hamza.stadiumbooking.security.jwt.JwtAuthorizationFilter;
import com.hamza.stadiumbooking.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SecurityConfig{

    private final JwtUtils utils;
    private final HandlerExceptionResolver exceptionResolver;

    public SecurityConfig(JwtUtils utils,
                          @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.utils = utils;
        this.exceptionResolver = exceptionResolver;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // Link of front-end
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    @Bean
    protected AuthenticationManager auth(UserDetailsService userDetailsService, BCryptPasswordEncoder passwordEncoder){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
    @Bean
    protected SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http.cors(c -> c.configurationSource(corsConfigurationSource()));
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        CustomAuthenticationFilter customAuthenticationFilter = new CustomAuthenticationFilter(authenticationManager, utils);
        customAuthenticationFilter.setFilterProcessesUrl("/api/v1/login");
        http.authorizeHttpRequests(auth ->
                auth
                        .requestMatchers("/api/v1/login/**", "/api/v1/users/refresh-token/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/stadiums/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/v1/stadiums/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/stadiums/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/stadiums/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")

                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings/**").hasAuthority("ROLE_PLAYER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/my-bookings").hasAuthority("ROLE_PLAYER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/**").hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMIN")

                        .anyRequest().authenticated()
        );
        http.addFilter(customAuthenticationFilter);
        http.addFilterBefore(new JwtAuthorizationFilter(utils, exceptionResolver), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
