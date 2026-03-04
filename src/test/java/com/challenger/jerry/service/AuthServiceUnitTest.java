package com.challenger.jerry.service;

import com.challenger.jerry.dto.*;
import com.challenger.jerry.entity.RefreshToken;
import com.challenger.jerry.entity.Role;
import com.challenger.jerry.entity.UserInfo;
import com.challenger.jerry.exception.InvalidRefreshTokenException;
import com.challenger.jerry.repository.RefreshTokenRepository;
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
    private RefreshTokenRepository refreshTokenRepository;

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

    @Test
    void shouldCreateRefreshToken() {
        // GIVEN
        String token = "refeshToken";
        UserInfo user = UserInfo.builder()
                .id(1L)
                .fullName("Name")
                .email("email@test.com")
                .password("encodedPassword")
                .createdAt(LocalDateTime.now())
                .roles(Set.of(new Role(1L,"USER_ROLE")))
                .build();
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token(token)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .userInfo(user)
                .build();

        Mockito.when(refreshTokenRepository.findByToken(Mockito.anyString()))
                .thenReturn(Optional.of(refreshToken));
        Mockito.when(jwtService.generateToken(Mockito.anyString()))
                .thenReturn("newAccessToken");

        // WHEN
        RefreshTokenResponse response = authService.refreshToken(token);

        // THEN
        Assertions.assertEquals("newAccessToken", response.getAccessToken());
        Mockito.verify(jwtService).generateToken(user.getEmail());
    }

    @Test
    void shouldReturnInvalidRefreshTokenWhenFailed(){
        // GIVEN
        String token = "refreshToken";
        Mockito.when(refreshTokenRepository.findByToken(Mockito.anyString()))
                .thenThrow(new InvalidRefreshTokenException("Invalid Refresh Token : " + token ));

        // WHEN + THEN
        InvalidRefreshTokenException exception = Assertions.assertThrows(
                InvalidRefreshTokenException.class,
                () -> {
                    this.authService.refreshToken(token);
                }
        );

        // THEN
        Assertions.assertEquals("Invalid Refresh Token : " + token, exception.getMessage());
        Mockito.verify(jwtService, Mockito.never()).generateToken(token);
    }

    @Test
    void shouldLogoutSuccessfully(){
        // GIVEN
        String token = "token";
        UserInfo user = UserInfo.builder()
                .id(1L)
                .fullName("Name")
                .email("email@test.com")
                .password("encodedPassword")
                .createdAt(LocalDateTime.now())
                .roles(Set.of(new Role(1L,"USER_ROLE")))
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token(token)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .userInfo(user)
                .build();
        Mockito.when(userInfoRepository.findByEmailWithRoles(Mockito.anyString()))
                .thenReturn(Optional.of(user));
        Mockito.when(refreshTokenRepository.findByUserInfo(Mockito.any(UserInfo.class)))
                .thenReturn(Optional.of(refreshToken));
        // WHEN
        this.authService.logout(user.getEmail());
        // THEN
        Mockito.verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    void shouldFailLogoutWhenRefreshTokenNotFound(){
        // GIVEN
        UserInfo user = UserInfo.builder()
                .id(1L)
                .fullName("Name")
                .email("email@test.com")
                .password("encodedPassword")
                .createdAt(LocalDateTime.now())
                .roles(Set.of(new Role(1L,"USER_ROLE")))
                .build();
        Mockito.when(userInfoRepository.findByEmailWithRoles(Mockito.anyString()))
                .thenReturn(Optional.of(user));
        Mockito.when(refreshTokenRepository.findByUserInfo(Mockito.any(UserInfo.class)))
                .thenThrow(new InvalidRefreshTokenException("Invalid Refresh Token"));

        // WHEN + THEN
        InvalidRefreshTokenException exception = Assertions.assertThrows(
                InvalidRefreshTokenException.class,
                () -> this.authService.logout(user.getEmail())
        );

        // THEN
        Assertions.assertEquals("Invalid Refresh Token" , exception.getMessage());
        Mockito.verify(refreshTokenRepository, Mockito.never()).delete(Mockito.any(RefreshToken.class));
    }
}
