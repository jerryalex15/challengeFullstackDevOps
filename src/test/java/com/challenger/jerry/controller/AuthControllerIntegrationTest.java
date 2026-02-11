package com.challenger.jerry.controller;

import com.challenger.jerry.DTO.LoginResponse;
import com.challenger.jerry.base.BaseIntegrationTest;
import com.challenger.jerry.entity.UserInfo;
import com.challenger.jerry.repository.UserInfoRepository;
import com.challenger.jerry.service.AuthService;
import com.challenger.jerry.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private PasswordEncoder encoder;

    @MockitoBean
    private AuthService authService; // Mock de ton service

    @MockitoBean
    private JwtService jwtService; // Mock du JwtService

    @BeforeEach
    void setup() {
        // Clean database to avoid unique constraint problem
        userInfoRepository.deleteAll();
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("test@gmail.com");
        userInfo.setFullName("Test user");
        userInfo.setRoles("ROLE_USER");
        userInfo.setPassword(encoder.encode("password"));
        userInfoRepository.save(userInfo);
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        // Configure mocked service configuration
        Mockito.when(authService.login("test@gmail.com", "password"))
                .thenReturn(new LoginResponse("fakeAccessToken", "fakeRefreshToken"));

        String body = """
                    {
                        "email": "test@gmail.com",
                        "password": "password"
                    }
                """;
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void shouldFailLoginWithWrongPassword() throws Exception {
        // Configure mocked service configuration
        Mockito.when(authService.login("test@gmail.com", "wrong"))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        String body = """
                    {
                          "email": "test@gmail.com",
                          "password": "wrong"
                    }
                """;
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRegisterSuccessfully() throws Exception {

        String body = """
                {
                    "email": "new@gmail.com",
                    "password": "password",
                    "fullName": "New User"
                }
                """;
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn401RefreshTokenInvalid() throws Exception {

        String body = """
                {
                    "refreshToken": "invalid-token"
                }
                """;
        mockMvc.perform(post("/api/auth/refresh_token").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }
}
