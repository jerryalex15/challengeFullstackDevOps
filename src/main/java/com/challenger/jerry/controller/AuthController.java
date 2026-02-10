package com.challenger.jerry.controller;

import com.challenger.jerry.DTO.LoginRequest;
import com.challenger.jerry.DTO.LoginResponse;
import com.challenger.jerry.DTO.RegisterRequest;
import com.challenger.jerry.DTO.RegisterResponse;
import com.challenger.jerry.entity.UserInfo;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import com.challenger.jerry.repository.RefreshTokenRepository;
import com.challenger.jerry.service.AuthService;
import com.challenger.jerry.service.JwtService;

import java.time.LocalDateTime;
import java.util.Map;

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
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) throws Exception {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),loginRequest.getPassword())
        );
        if (authentication.isAuthenticated()) {
            return ResponseEntity.ok().body(this.authService.login(loginRequest.getEmail(), loginRequest.getPassword()));
        } else {
            throw new UsernameNotFoundException("Invalid email request!");
        }
    }

    @PostMapping("/refresh_token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        return refreshTokenRepository.findByToken(refreshToken)
                .filter(rt -> rt.getExpiryDate().isAfter(LocalDateTime.now()))
                .map(rt -> {
                    UserInfo userInfo = rt.getUserInfo();
                    String newAccessToken = null;
                    try {
                        newAccessToken = jwtService.generateToken(userInfo.getEmail());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
