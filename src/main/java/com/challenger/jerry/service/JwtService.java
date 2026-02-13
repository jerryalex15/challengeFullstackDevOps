package com.challenger.jerry.service;

import com.challenger.jerry.entity.RefreshToken;
import com.challenger.jerry.entity.UserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.challenger.jerry.repository.RefreshTokenRepository;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@Service
public class JwtService {

    // Token Time To Live
    @Value("${JWT_ACCESS_EXPIRATION}")
    public long jwtExpiration;

    // Time to expire
    @Value("${JWT_REFRESH_EXPIRATION}")
    public long jwtRefresh_expiration;

    private final RefreshTokenRepository refreshTokenRepository;
    @Autowired
    public JwtService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    private PublicKey getPublicKey() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("keys/public_key.pem");
        if (is == null) {
            throw new FileNotFoundException("public_key.pem not found in resources/keys");
        }
        String publicKeyContent = new String(is.readAllBytes())
                .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(keySpec);
    }

    private PrivateKey getPrivateKey() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("keys/private_key_pkcs8.pem");
        if (is == null) {
            throw new FileNotFoundException("private_key_pkcs8.pem not found in resources/keys");
        }
        String key = new String(is.readAllBytes())
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
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
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT", e);
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
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public String extractUsername(String token) throws Exception { // username
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) throws Exception {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) throws Exception {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Boolean isTokenExpired(String token) throws Exception {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) throws Exception {
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
        tokenEntity.setExpiryDate(LocalDateTime.now().plusSeconds(jwtRefresh_expiration / 1000)); // ex : TTL 7

        refreshTokenRepository.save(tokenEntity);

        return refreshToken;
    }
}
