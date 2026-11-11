package com.daebangg.sayroam.user.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_social_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_social_account_provider_user", columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(name = "uk_user_social_account_user_provider", columnNames = {"user_id", "provider"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(nullable = false, length = 100)
    private String providerUserId;

    // 공급자가 준 이메일 원본. 로그인 식별자가 아니라 UNIQUE를 걸지 않는다
    private String email;

    private LocalDateTime lastLoginAt;

    @Builder
    private UserSocialAccount(User user, SocialProvider provider, String providerUserId, String email) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
    }

}
