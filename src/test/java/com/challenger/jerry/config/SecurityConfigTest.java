package com.challenger.jerry.config;

import com.challenger.jerry.filter.JwtAuthFilter;
import com.challenger.jerry.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtAuthFilter, customUserDetailsService);
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
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);

        CorsConfiguration config = source.getCorsConfiguration(
                new org.springframework.mock.web.MockHttpServletRequest()
        );
        assertNotNull(config);
        assertTrue(config.getAllowedMethods().containsAll(
                java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        ));
        assertTrue(config.getAllowCredentials());
    }

    @Test
    void authenticationProvider_shouldBeConfigured() {
        AuthenticationProvider provider = securityConfig.authenticationProvider();
        assertNotNull(provider);
    }
}