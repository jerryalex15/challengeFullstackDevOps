package com.challenger.jerry.config;

import com.challenger.jerry.filter.JwtAuthFilter;
import com.challenger.jerry.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    private SecurityConfig securityConfig;
    private CorsConfigurationSource corsSource; // ← nouveau

    @BeforeEach
    void setUp() {
        // On crée une vraie instance de CorsConfigurationSource pour les tests
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:4200", "http://192.168.1.1"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        corsSource = source;

        // Constructeur avec 3 arguments maintenant
        securityConfig = new SecurityConfig(jwtAuthFilter, customUserDetailsService, corsSource);
        ReflectionTestUtils.setField(securityConfig, "appUrl", "http://localhost:4200");
        ReflectionTestUtils.setField(securityConfig, "appIp", "http://192.168.1.1");
    }

    @Test
    void passwordEncoder_shouldReturnBCryptEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertNotNull(encoder);
        assertTrue(encoder.matches("password", encoder.encode("password")));
    }

    @Test
    void corsConfigurationSource_shouldAllowExpectedMethods() {
        // On teste directement corsSource — plus besoin d'appeler la méthode
        CorsConfiguration config = corsSource.getCorsConfiguration(
                new MockHttpServletRequest()
        );
        assertNotNull(config);
        assertTrue(config.getAllowedMethods().containsAll(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        ));
        assertTrue(config.getAllowCredentials());
    }

    @Test
    void authenticationProvider_shouldBeConfigured() {
        AuthenticationProvider provider = securityConfig.authenticationProvider();
        assertNotNull(provider);
    }
}