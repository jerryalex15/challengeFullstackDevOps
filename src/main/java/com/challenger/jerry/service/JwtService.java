package com.challenger.jerry.service;

import com.challenger.jerry.entity.RefreshToken;
import com.challenger.jerry.entity.UserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.challenger.jerry.repository.RefreshTokenRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${jwt.public-key-path}")
    private String publicKeyPath;

    @Value("${jwt.access-expiration}")
    public long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    public long jwtRefreshExpiration;

    @Value("classpath:keys/private_key_pkcs8.pem")
    public Resource privateKeyResource;

    @Value("classpath:keys/public_key.pem")
    public Resource publicKeyResource;

    private final RefreshTokenRepository refreshTokenRepository;
    @Autowired
    public JwtService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    private PublicKey getPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SecurityException {
        String keyContent;
        if (publicKeyPath != null && !publicKeyPath.isBlank() && Files.exists(Paths.get(publicKeyPath))) {
            keyContent = Files.readString(Paths.get(publicKeyPath)); // Jenkins
        } else {
            try (InputStream is = publicKeyResource.getInputStream()) {
                keyContent = new String(is.readAllBytes());
            }
        }
        keyContent = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(keyContent));
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private PrivateKey getPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SecurityException {
        String keyContent;
        if (privateKeyPath != null && !privateKeyPath.isBlank() && Files.exists(Paths.get(privateKeyPath))) {
            keyContent = Files.readString(Paths.get(privateKeyPath)); // Jenkins
        } else {
            // fallback local
            try (InputStream is = privateKeyResource.getInputStream()) {
                keyContent = new String(is.readAllBytes());
            }
        }
        keyContent = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyContent));
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    // Generate Token with email
    public String generateToken(String email) {
        try {
            return Jwts.builder()
                    .setSubject(email)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(getPrivateKey(), SignatureAlgorithm.RS256)
                    .compact();
        } catch (JwtException | IllegalArgumentException |
                 IOException | NoSuchAlgorithmException |
                 InvalidKeySpecException e) {
            throw new JwtException("Failed to generate JWT", e);
        }
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new JwtException("Invalid JWT token", e);
        }
    }

    public String extractUsername(String token) { // username
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String generateRefreshToken(UserInfo userInfo) {
        // supprimer ancien token s'il existe
        refreshTokenRepository.findByUserInfo(userInfo)
                .ifPresent(refreshTokenRepository::delete);

        String refreshToken = UUID.randomUUID().toString();

        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setToken(refreshToken);
        tokenEntity.setUserInfo(userInfo);
        tokenEntity.setExpiryDate(LocalDateTime.now().plusSeconds(jwtRefreshExpiration / 1000)); // ex : TTL 7

        refreshTokenRepository.save(tokenEntity);

        return refreshToken;
    }
}
