package com.challenger.jerry.controller;

import com.challenger.jerry.dto.*;
import com.challenger.jerry.entity.UserInfo;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import com.challenger.jerry.repository.RefreshTokenRepository;
import com.challenger.jerry.service.AuthService;
import com.challenger.jerry.service.JwtService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Autowired
    public AuthController(AuthService authService, AuthenticationManager authenticationManager, JwtService jwtService, RefreshTokenRepository refreshTokenRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest){
        RegisterResponse registerResponse = this.authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(registerResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
            // Si pas d'exception, login OK
            return ResponseEntity.ok(authService.login(loginRequest));
        } catch (BadCredentialsException ex) {
            // Mauvais mot de passe ou email
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/refresh_token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        return refreshTokenRepository.findByToken(refreshToken)
                .filter(rt -> rt.getExpiryDate().isAfter(LocalDateTime.now()))
                .map(rt -> {
                    UserInfo userInfo = rt.getUserInfo();
                    String newAccessToken = jwtService.generateToken(userInfo.getEmail());
                    return ResponseEntity.ok().body(new RefreshTokenResponse(newAccessToken));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
