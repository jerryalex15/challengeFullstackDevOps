package com.challenger.jerry.repository;

import com.challenger.jerry.entity.RefreshToken;
import com.challenger.jerry.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserInfo(UserInfo userInfo);
    Optional<RefreshToken> findByUserInfo(UserInfo userInfo);
}
