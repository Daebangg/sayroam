package com.daebangg.sayroam.user.domain.repository;

import com.daebangg.sayroam.user.domain.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndDeletedAtIsNull(Long userId);

    /** 비밀번호 변경·탈퇴와 같은 회원 행을 잠가 프로필 변경을 순서대로 처리한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId and u.deletedAt is null")
    Optional<User> findActiveByIdForUpdate(@Param("userId") Long userId);
}
