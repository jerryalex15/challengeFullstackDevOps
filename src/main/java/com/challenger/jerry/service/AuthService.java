package com.challenger.jerry.service;

import com.challenger.jerry.dto.*;
import com.challenger.jerry.entity.RefreshToken;
import com.challenger.jerry.entity.Role;
import com.challenger.jerry.entity.UserInfo;
import com.challenger.jerry.exception.InvalidRefreshTokenException;
import com.challenger.jerry.repository.RefreshTokenRepository;
import com.challenger.jerry.repository.RoleRepository;
import com.challenger.jerry.repository.UserInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthService {

    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Autowired
    public AuthService(
            UserInfoRepository userInfoRepository,
            RefreshTokenRepository refreshTokenRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ){
        this.userInfoRepository = userInfoRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.roleRepository = roleRepository;
    }

    public RegisterResponse register(RegisterRequest registerRequest) {
        // Vérifie si l'email existe déjà
        if (userInfoRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email exists already!");
        }

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    return roleRepository.save(role);
                });

        // Crée l'entité UserInfo via le builder
        UserInfo userInfo = UserInfo.builder()
                .email(registerRequest.getEmail())
                .fullName(registerRequest.getFullName())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .roles(Set.of(defaultRole)) // rôle par défaut
                .createdAt(LocalDateTime.now())
                .build();

        UserInfo savedUser = userInfoRepository.save(userInfo);

        // Retourne un dto pour ne pas exposer le password
        return mapToRegisterResponse(savedUser);
    }

    private RegisterResponse mapToRegisterResponse(UserInfo userInfo){
        return RegisterResponse.builder()
                .id(userInfo.getId())
                .email(userInfo.getEmail())
                .fullName(userInfo.getFullName())
                .roles(userInfo.getRoles().stream()
                        .map(Role::getName)
                        .toList())
                .build();
    }

    public LoginResponse login(LoginRequest loginRequest) {
        UserInfo user = userInfoRepository.findByEmailWithRoles(loginRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        String accessToken = jwtService.generateToken(loginRequest.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user);

        return new LoginResponse(accessToken, refreshToken);
    }

    @Transactional
    public RefreshTokenResponse refreshToken(String refreshToken) {

        RefreshToken rt = refreshTokenRepository.findByToken(refreshToken)
                .filter(refToken -> refToken.getExpiryDate().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid Refresh Token"));

        UserInfo userInfo = rt.getUserInfo();
        String newAccessToken = jwtService.generateToken(userInfo.getEmail());
        return new RefreshTokenResponse(newAccessToken);
    }
}
