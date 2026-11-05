package com.daebangg.sayroam.user.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_credential")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCredential extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String passwordHash;

    private LocalDateTime passwordChangedAt;

    public void changePassword(String passwordHash, LocalDateTime changedAt) {
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.passwordChangedAt = Objects.requireNonNull(changedAt);
    }

    @Builder
    private UserCredential(User user, String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
    }

}
