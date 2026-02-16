package com.challenger.jerry.service;

import com.challenger.jerry.adapter.UserInfoDetails;
import com.challenger.jerry.entity.UserInfo;
import com.challenger.jerry.repository.RefreshTokenRepository;
import com.challenger.jerry.repository.UserInfoRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class JwtServiceUnitTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setup() {
        // Injecter manuellement tes valeurs TTL
        jwtService.jwtExpiration = 1000 * 60 * 60;        // 1h
        jwtService.jwtRefreshExpiration = 1000 * 60 * 60 * 24; // 1j
    }

    @Test
    void testGenerateTokenAndValidate() {
        UserInfo testUser = new UserInfo();
        testUser.setEmail("test@gmail.com");

        String token = jwtService.generateToken(testUser.getEmail());
        Assertions.assertNotNull(token);

        Assertions.assertTrue(jwtService.validateToken(token, new UserInfoDetails(testUser)));
        Assertions.assertEquals("test@gmail.com", jwtService.extractUsername(token));
    }

    @Test
    void testRefreshTokenGeneration() {
        UserInfo userInfoTest = new UserInfo();
        Mockito.when(refreshTokenRepository.findByUserInfo(userInfoTest))
                .thenReturn(Optional.empty());

        String refreshToken = jwtService.generateRefreshToken(userInfoTest);
        Assertions.assertNotNull(refreshToken);

        Mockito.verify(refreshTokenRepository).save(Mockito.any());
    }
}
