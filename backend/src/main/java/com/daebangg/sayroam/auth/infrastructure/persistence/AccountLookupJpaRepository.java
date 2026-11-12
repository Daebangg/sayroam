package com.daebangg.sayroam.auth.infrastructure.persistence;

import com.daebangg.sayroam.auth.application.dto.SocialAccountResult;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.user.domain.SocialProvider;
import com.daebangg.sayroam.user.domain.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
public interface AccountLookupJpaRepository extends Repository<User, Long> {
    boolean existsByEmail(String email);

    @Query("""
            select new com.daebangg.sayroam.auth.application.port.out.AccountLookup$Credentials(
                u.id, c.passwordHash, u.authVersion)
            from UserCredential c join c.user u
            where u.email = :email and u.deletedAt is null and u.emailVerifiedAt is not null
            """)
    Optional<AccountLookup.Credentials> findActiveCredentials(@Param("email") String email);

    @Query("""
            select new com.daebangg.sayroam.auth.domain.AuthenticatedAccount(
                u.id, u.email, u.nickname, u.profileImageUrl, u.role, u.authVersion)
            from User u where u.id = :userId and u.deletedAt is null
            """)
    Optional<AuthenticatedAccount> findActiveAccount(@Param("userId") long userId);

    @Query("""
            select new com.daebangg.sayroam.auth.application.port.out.AccountLookup$VerifiedAccount(
                u.email, u.authVersion)
            from User u where u.id = :userId and u.deletedAt is null
                and u.emailVerifiedAt is not null and u.email is not null
            """)
    Optional<AccountLookup.VerifiedAccount> findActiveVerifiedAccount(@Param("userId") long userId);

    @Query("""
            select new com.daebangg.sayroam.auth.application.dto.SocialAccountResult(u.id, u.authVersion)
            from UserSocialAccount s join s.user u
            where s.provider = :provider and s.providerUserId = :subject
                and u.deletedAt is null and u.emailVerifiedAt is not null
            """)
    Optional<SocialAccountResult> findActiveSocialAccount(@Param("provider") SocialProvider provider,
                                                         @Param("subject") String providerUserId);

    @Query("""
            select new com.daebangg.sayroam.auth.application.dto.SocialAccountResult(u.id, u.authVersion)
            from User u where u.email = :email and u.deletedAt is null and u.emailVerifiedAt is not null
            """)
    Optional<SocialAccountResult> findActiveSocialAccountByEmail(@Param("email") String email);
}
