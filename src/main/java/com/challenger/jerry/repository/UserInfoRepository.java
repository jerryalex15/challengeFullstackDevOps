package com.challenger.jerry.repository;

import com.challenger.jerry.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    boolean existsByEmail(String email);
    Optional<UserInfo> findByEmail(String email);
}
