package com.challenger.jerry.service;

import com.challenger.jerry.dto.UserResponse;
import com.challenger.jerry.entity.Role;
import com.challenger.jerry.entity.UserInfo;
import com.challenger.jerry.repository.UserInfoRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class UserInfoServiceUnitTest {

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserInfoService userInfoService;

    @Test
    void shouldReturnCurrentUser(){
        // GIVEN
        String email = "test@example.com";

        UserInfo user = UserInfo.builder()
                .id(1L)
                .email(email)
                .fullName("Test User")
                .roles(Set.of(Role.builder().id(1L).name("ROLE_USER").build()))
                .createdAt(LocalDateTime.now())
                .build();

        // Mock du SecurityContext
        SecurityContextHolder.setContext(securityContext);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getName()).thenReturn(email);

        Mockito.when(this.userInfoRepository.findByEmailWithRoles(email))
                .thenReturn(Optional.of(user));
        // WHEN
        UserResponse userResponse = this.userInfoService.getCurrentUser();
        // THEN
        Assertions.assertNotNull(userResponse);
        Assertions.assertEquals(user.getId(), userResponse.id());
        Assertions.assertEquals(user.getEmail(), userResponse.email());
        Assertions.assertEquals(user.getFullName(), userResponse.fullName());

        // Clean up SecurityContext in order to avoid side effect (effet de bord)
        SecurityContextHolder.clearContext();
    }
    @Test
    void shouldReturnNoAuthenticatedUserWhenNotAuthenticated(){
        // GIVEN
        SecurityContextHolder.setContext(securityContext);
        Mockito.when(securityContext.getAuthentication())
                .thenReturn(authentication);
        Mockito.when(authentication.isAuthenticated())
                .thenReturn(false);

        // WHEN & THEN
        ResponseStatusException exception = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> userInfoService.getCurrentUser()
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        Assertions.assertTrue(exception.getReason().contains("User not authenticated"));
        // Clean up SecurityContext in order to avoid side effect (effet de bord)
        SecurityContextHolder.clearContext();
    }
    @Test
    void shouldReturnEmailNotFoundWhenFailed(){
        // GIVEN
        String email = "test@gmail.com";
        SecurityContextHolder.setContext(securityContext);
        Mockito.when(securityContext.getAuthentication())
                .thenReturn(authentication);
        Mockito.when(authentication.isAuthenticated())
                .thenReturn(true);
        Mockito.when(authentication.getName())
                .thenReturn(email);
        Mockito.when(userInfoRepository.findByEmailWithRoles(email)).thenReturn(Optional.empty());

        // WHEN & THEN
        ResponseStatusException exception = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> userInfoService.getCurrentUser()
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        Assertions.assertTrue(exception.getReason().contains("User with email " + email + " not found"));
        // Clean up SecurityContext in order to avoid side effect (effet de bord)
        SecurityContextHolder.clearContext();
    }
}
