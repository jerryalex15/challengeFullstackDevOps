package com.challenger.jerry.filter;

import com.challenger.jerry.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    // ── shouldNotFilter ──────────────────────────────────────

    @Test
    void shouldNotFilter_optionsOrPublicAuthEndpoints() {
        request.setMethod("OPTIONS");
        assertTrue(jwtAuthFilter.shouldNotFilter(request));

        request.setMethod("GET");
        request.setServletPath("/api/auth/login");
        assertTrue(jwtAuthFilter.shouldNotFilter(request));

        request.setServletPath("/api/user/profile");
        assertFalse(jwtAuthFilter.shouldNotFilter(request));
    }

    // ── Pas de header Authorization ──────────────────────────

    @Test
    void doFilter_noOrInvalidAuthorizationHeader_continuesChain() throws ServletException, IOException {
        request.setServletPath("/api/user/profile");

        // Pas de header
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // Header non Bearer
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_invalidJwt_continuesChainWithoutAuth() throws ServletException, IOException {
        request.setServletPath("/api/user/profile");
        request.addHeader("Authorization", "Bearer token.invalide");

        when(jwtService.extractUsername("token.invalide"))
                .thenThrow(new JwtException("Token invalide"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ── Token valide ─────────────────────────────────────────

    @Test
    void doFilter_validJwt_setsAuthentication() throws ServletException, IOException {
        request.setServletPath("/api/user/profile");
        request.addHeader("Authorization", "Bearer token.valide");

        UserDetails userDetails = new User(
                "user@test.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(jwtService.extractUsername("token.valide")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user@test.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void doFilter_validJwt_authAlreadySet_doesNotReloadUser() throws ServletException, IOException {
        request.setServletPath("/api/user/profile");
        request.addHeader("Authorization", "Bearer token.valide");

        UserDetails userDetails = new User(
                "user@test.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        var existingAuth = new org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtService.extractUsername("token.valide")).thenReturn("user@test.com");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService); // pas rechargé
    }

    // ── Logout endpoint ──────────────────────────────────────

    @Test
    void doFilter_logoutEndpoint_withoutJwt_callsChain() throws ServletException, IOException {
        request.setServletPath("/api/auth/logout");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}