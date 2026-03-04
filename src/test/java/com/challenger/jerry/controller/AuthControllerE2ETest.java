package com.challenger.jerry.controller;

import com.challenger.jerry.dto.*;
import com.challenger.jerry.DatabaseContainer.DatabaseInstanceTest;
import com.challenger.jerry.entity.RefreshToken;
import com.challenger.jerry.entity.Role;
import com.challenger.jerry.entity.UserInfo;
import com.challenger.jerry.repository.RefreshTokenRepository;
import com.challenger.jerry.repository.RoleRepository;
import com.challenger.jerry.repository.UserInfoRepository;
import com.challenger.jerry.service.JwtService;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerE2ETest extends DatabaseInstanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private UserInfo user;

    @BeforeEach
    void setup() {
        HttpClient httpClient = HttpClientBuilder.create()
                .disableRedirectHandling()
                .build();

        restTemplate.getRestTemplate().setRequestFactory(
                new HttpComponentsClientHttpRequestFactory(httpClient)
        );

        // --- setup DB comme avant ---
        refreshTokenRepository.deleteAll();
        userInfoRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = Role.builder().name("ROLE_USER").build();
        role = roleRepository.save(role);

        user = UserInfo.builder()
                .email("test@gmail.com")
                .fullName("Test User")
                .password(passwordEncoder.encode("password"))
                .roles(Set.of(role))
                .createdAt(LocalDateTime.now())
                .build();
        userInfoRepository.save(user);
    }

    @Test
    void invalidRefreshTokenE2E() {
        // GIVEN
        RefreshTokenRequest request =
                new RefreshTokenRequest("fake-token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<RefreshTokenRequest> entity =
                new HttpEntity<>(request, headers);

        // WHEN
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/auth/refresh_token",
                        entity,
                        String.class);

        // THEN
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void expiredRefreshTokenE2E() {
        // GIVEN
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("expired-token");
        refreshToken.setUserInfo(user);
        refreshToken.setExpiryDate(LocalDateTime.now().minusDays(1)); // expiré
        refreshTokenRepository.save(refreshToken);

        RefreshTokenRequest request =
                new RefreshTokenRequest("expired-token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<RefreshTokenRequest> entity =
                new HttpEntity<>(request, headers);

        // WHEN
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/auth/refresh_token",
                        entity,
                        String.class);

        // THEN
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void loginSuccessfulE2ETest() {
        LoginRequest loginRequest = new LoginRequest("test@gmail.com", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        ResponseEntity<LoginResponse> response =
                restTemplate.postForEntity("/api/auth/login",
                        request, LoginResponse.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void loginFailedWithWrongCredential() {
        // GIVEN
        LoginRequest loginRequest = new LoginRequest("test@gmail.com", "wrong password");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginRequest> loginRequestHttpEntity = new HttpEntity<>(loginRequest, httpHeaders);

        // WHEN
        ResponseEntity<LoginResponse> response =
                restTemplate.postForEntity("/api/auth/login",
                        loginRequestHttpEntity, LoginResponse.class);

        // THEN
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void RegisterSuccessful() {
        RegisterRequest registerRequest = new RegisterRequest("test1@gmail.com", "password", "My Name");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<RegisterRequest> requestHttpEntity = new HttpEntity<>(registerRequest, httpHeaders);

        ResponseEntity<RegisterResponse> response =
                restTemplate.postForEntity("/api/auth/register",
                        requestHttpEntity, RegisterResponse.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void refreshTokenSuccessE2E() {
        // GIVEN
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("valid-token");
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(1));
        refreshToken.setUserInfo(user);
        refreshTokenRepository.save(refreshToken);

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest("valid-token");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth("valid-token");
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RefreshTokenRequest> requestHttpEntity = new HttpEntity<>(refreshTokenRequest, httpHeaders);

        // WHEN
        ResponseEntity<RefreshTokenResponse> response =
                restTemplate.postForEntity("/api/auth/refresh_token",
                        requestHttpEntity, RefreshTokenResponse.class);

        // THEN
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}