package com.challenger.jerry.controller;

import com.challenger.jerry.DatabaseContainer.DatabaseInstanceTest;
import com.challenger.jerry.entity.RefreshToken;
import com.challenger.jerry.entity.Role;
import com.challenger.jerry.entity.UserInfo;
import com.challenger.jerry.repository.RefreshTokenRepository;
import com.challenger.jerry.repository.RoleRepository;
import com.challenger.jerry.repository.UserInfoRepository;
import com.challenger.jerry.service.AuthService;
import com.challenger.jerry.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@Transactional
class AuthControllerIntegrationTest extends DatabaseInstanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserInfo user;

    @BeforeEach
    void setup() {
        Role defaultRole =  new Role();
        defaultRole.setName("ROLE_USER");
        defaultRole = roleRepository.save(defaultRole);

        user = new UserInfo();
        user.setEmail("test@gmail.com");
        user.setFullName("Test User");
        user.setRoles(Set.of(defaultRole));
        user.setPassword(encoder.encode("password"));
        userInfoRepository.save(user);
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {

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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@gmail.com"))
                .andExpect(jsonPath("$.fullName").value("New User"))
                .andExpect(jsonPath("$.roles").value("ROLE_USER"));
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

    @Test
    void shouldReturn401WhenRefreshTokenExpired() throws Exception {
        // Créer un refresh token expiré
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("expired-token");
        refreshToken.setUserInfo(user);
        refreshToken.setExpiryDate(LocalDateTime.now().minusMinutes(5));
        refreshTokenRepository.save(refreshToken);

        String body = """
                {
                    "refreshToken": "expired-token"
                }
                """;

        mockMvc.perform(post("/api/auth/refresh_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLogoutSuccessfully() throws Exception {

        // 1. Login pour obtenir un vrai access token (crée automatiquement le refresh token)
        String loginBody = """
            {
                "email": "test@gmail.com",
                "password": "password"
            }
            """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = new ObjectMapper()
                .readTree(loginResponse)
                .get("accessToken")
                .asText();

        // 2. Logout avec le vrai token
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn401_whenAccessingProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());
    }
}