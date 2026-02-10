package com.challenger.jerry.service;

import com.challenger.jerry.DTO.LoginResponse;
import com.challenger.jerry.DTO.RegisterRequest;
import com.challenger.jerry.DTO.RegisterResponse;
import com.challenger.jerry.entity.UserInfo;
import com.challenger.jerry.mapper.UserMapper;
import com.challenger.jerry.repository.UserInfoRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public AuthService(
            UserInfoRepository userInfoRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ){
        this.userInfoRepository = userInfoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public RegisterResponse register(RegisterRequest registerRequest) {
        // Vérifie si l'email existe déjà
        if (userInfoRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email exists already!");
        }

        // Crée l'entité UserInfo via le builder
        UserInfo userInfo = UserInfo.builder()
                .email(registerRequest.getEmail())
                .fullName(registerRequest.getFullName())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .roles("ROLE_USER") // rôle par défaut
                .createdAt(LocalDateTime.now())
                .build();

        UserInfo savedUser = userInfoRepository.save(userInfo);

        // Retourne un DTO pour ne pas exposer le password
        return RegisterResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .roles(savedUser.getRoles())
                .build();
    }

    public LoginResponse login(String email, String password) {
        UserInfo user = userInfoRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        String accessToken = jwtService.generateToken(email);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new LoginResponse(accessToken, refreshToken);
    }
}
