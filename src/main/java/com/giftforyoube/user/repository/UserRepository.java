package com.giftforyoube.user.repository;

import com.giftforyoube.user.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByKakaoId(Long kakaoId);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByRefreshToken(String refreshToken);

    @Query("SELECT u FROM User u JOIN u.fundings f WHERE f.id = :fundingId")
    User findUserByFundingId(@Param("fundingId") Long fundingId);
}