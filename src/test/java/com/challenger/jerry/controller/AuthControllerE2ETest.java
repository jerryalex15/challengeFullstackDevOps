package com.challenger.jerry.controller;

import com.challenger.jerry.DTO.*;
import com.challenger.jerry.DatabaseContainer.DatabaseInstanceTest;
import com.challenger.jerry.entity.RefreshToken;
import com.challenger.jerry.entity.UserInfo;
import com.challenger.jerry.repository.RefreshTokenRepository;
import com.challenger.jerry.repository.UserInfoRepository;
import com.challenger.jerry.service.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthControllerE2ETest extends DatabaseInstanceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private UserInfo user;

    @BeforeEach
    void setup() {
        refreshTokenRepository.deleteAll();
        userInfoRepository.deleteAll();

        user = new UserInfo();
        user.setEmail("test@gmail.com");
        user.setFullName("Test User");
        user.setRoles("ROLE_USER");
        user.setPassword(passwordEncoder.encode("password"));
        user.setCreatedAt(LocalDateTime.now());
        userInfoRepository.save(user);
    }
    @Test
    void RegisterSuccessful(){
        RegisterRequest registerRequest = new RegisterRequest("test1@gmail.com", "password","My Name");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<RegisterRequest> requestHttpEntity = new HttpEntity<>(registerRequest,httpHeaders);

        ResponseEntity<RegisterResponse> response =
                restTemplate.postForEntity("/api/auth/register", requestHttpEntity, RegisterResponse.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        RegisterResponse registerResponse = response.getBody();
        assert registerResponse != null;
        Assertions.assertEquals(registerRequest.getEmail(), registerResponse.getEmail());
    }

    @Test
    void loginSuccessfulE2ETest() {
        LoginRequest loginRequest = new LoginRequest("test@gmail.com", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        ResponseEntity<LoginResponse> response =
                restTemplate.postForEntity("/api/auth/login", request, LoginResponse.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(response.getBody().getAccessToken());
        Assertions.assertNotNull(response.getBody().getRefreshToken());
    }

    @Test
    void loginFailedWithWrongCredential(){
        // GIVEN
        LoginRequest loginRequest = new LoginRequest("test@gmail.com", "wrong password");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginRequest> loginRequestHttpEntity = new HttpEntity<>(loginRequest, httpHeaders);

        // WHEN
        ResponseEntity<LoginResponse> response =
                restTemplate.postForEntity("/api/auth/login", loginRequest, LoginResponse.class);

        // THEN
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void refreshTokenSuccessE2E() throws Exception {
        // GIVEN
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("valid-token");
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(1));
        refreshToken.setUserInfo(user);
        refreshTokenRepository.save(refreshToken);

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest("valid-token");

        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity<RefreshTokenRequest> requestHttpEntity = new HttpEntity<>(refreshTokenRequest, httpHeaders);

        // WHEN
        ResponseEntity<RefreshTokenResponse> response =
                restTemplate.postForEntity("/api/auth/refresh_token", requestHttpEntity, RefreshTokenResponse.class);

        // THEN
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        RefreshTokenResponse refreshTokenResponse = response.getBody();
        assert refreshTokenResponse != null;
        Assertions.assertNotNull(refreshTokenResponse.getAccessToken());
        // Optionnel : vérifier que le token contient bien l'email
        String decodedEmail = jwtService.extractUsername(refreshTokenResponse.getAccessToken());
        Assertions.assertEquals(user.getEmail(), decodedEmail);
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
        ResponseEntity<RefreshTokenResponse> response =
                restTemplate.postForEntity("/api/auth/refresh_token",
                        entity,
                        RefreshTokenResponse.class);

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
        ResponseEntity<RefreshTokenResponse> response =
                restTemplate.postForEntity("/api/auth/refresh_token",
                        entity,
                        RefreshTokenResponse.class);

        // THEN
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}