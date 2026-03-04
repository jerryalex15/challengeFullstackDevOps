package com.challenger.jerry.service;

import com.challenger.jerry.dto.LoginRequest;
import com.challenger.jerry.dto.LoginResponse;
import com.challenger.jerry.dto.RegisterRequest;
import com.challenger.jerry.dto.RegisterResponse;
import com.challenger.jerry.entity.Role;
import com.challenger.jerry.entity.UserInfo;
import com.challenger.jerry.repository.RoleRepository;
import com.challenger.jerry.repository.UserInfoRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @Test
    void shouldRegisterSuccessfully(){
        // GIVEN
        String email = "test@gmail.com";
        String password = "password";
        String fullName = "Full Name";
        String roleName = "ROLE_USER";
        Role defaultRole = Role.builder().id(1L).name(roleName).build();

        RegisterRequest registerRequest = new RegisterRequest(email, password, fullName);
        Mockito.when(userInfoRepository.existsByEmail(Mockito.anyString()))
                        .thenReturn(false);
        Mockito.when(passwordEncoder.encode(Mockito.anyString()))
                .thenReturn("encodedPassword");
        Mockito.when(roleRepository.findByName(roleName))
                .thenReturn(Optional.of(defaultRole));

        UserInfo savedUser = UserInfo.builder()
                .id(1L)
                .email(email)
                .password("encodedPassword")
                .fullName(fullName)
                .roles(Set.of(defaultRole))
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(userInfoRepository.save(Mockito.any()))
                .thenReturn(savedUser);
        // WHEN
        RegisterResponse registerResponse = authService.register(registerRequest);

        String expectedEmail = registerResponse.getEmail();

        // THEN
        Assertions.assertNotNull(registerResponse.getRoles());
        Assertions.assertEquals(email, expectedEmail);
        Mockito.verify(userInfoRepository).save(Mockito.any());
    }

    @Test
    void shouldFailRegisterWithExistingEmail(){
        // GIVEN
        RegisterRequest registerRequest = new RegisterRequest("test@gmail.com", "password", "User Name");

        Mockito.when(userInfoRepository.existsByEmail(Mockito.anyString()))
                .thenReturn(true);

        // WHEN + THEN
        Assertions.assertThrowsExactly(
                IllegalArgumentException.class,
                ()-> authService.register(registerRequest)
        );

        // Verify that save is never called
        Mockito.verify(userInfoRepository, Mockito.never())
                .save(Mockito.any());
    }

    @Test
    void shouldLoginSuccessfully(){
        // GIVEN
        String email = "email@test.com";
        LoginRequest loginRequest = new LoginRequest(email, "password");

        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("test@gmail.com");
        userInfo.setPassword("encodedPassword");

        // WHEN
        Mockito.when(userInfoRepository.findByEmailWithRoles(email))
                        .thenReturn(Optional.of(userInfo));

        Mockito.when(jwtService.generateToken(email))
                .thenReturn("fakeAccessToken");

        Mockito.when(jwtService.generateRefreshToken(userInfo))
                .thenReturn("fakeRefreshToken");

        Mockito.when(passwordEncoder.matches(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);

        // WHEN
        LoginResponse loginResponse = authService.login(loginRequest);

        // THEN
        Assertions.assertNotNull(loginResponse.getAccessToken());
        Assertions.assertNotNull(loginResponse.getRefreshToken());

        // VERIFY
        Mockito.verify(userInfoRepository).findByEmailWithRoles(email);
        Mockito.verify(passwordEncoder).matches("password", "encodedPassword");
        Mockito.verify(jwtService).generateToken(email);
        Mockito.verify(jwtService).generateRefreshToken(userInfo);
    }
}
