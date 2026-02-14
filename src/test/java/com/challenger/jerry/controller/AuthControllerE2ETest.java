package com.challenger.jerry.controller;

import com.challenger.jerry.DTO.LoginRequest;
import com.challenger.jerry.DTO.LoginResponse;
import com.challenger.jerry.DatabaseContainer.DatabaseInstanceTest;
import com.challenger.jerry.config.TestSecurityConfig;
import com.challenger.jerry.entity.UserInfo;
import com.challenger.jerry.repository.UserInfoRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class AuthControllerE2ETest extends DatabaseInstanceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserInfo user;

    @BeforeEach
    void setup() {
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
    void loginE2ETest() {
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
}