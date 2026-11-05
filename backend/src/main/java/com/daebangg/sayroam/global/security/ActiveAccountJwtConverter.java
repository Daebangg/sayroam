package com.daebangg.sayroam.global.security;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ActiveAccountJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final AccountLookup accounts;

    public ActiveAccountJwtConverter(AccountLookup accounts) {
        this.accounts = accounts;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        long userId;
        long authVersion;
        try {
            userId = Long.parseLong(jwt.getSubject());
            Object claim = jwt.getClaims().get("auth_ver");
            if (!(claim instanceof String version)) {
                throw new BadCredentialsException("Invalid access token");
            }
            authVersion = Long.parseLong(version);
        } catch (NumberFormatException exception) {
            throw new BadCredentialsException("Invalid access token");
        }
        try {
            var account = accounts.findActiveAccount(userId)
                    .orElseThrow(() -> new BadCredentialsException("Authentication is required"));
            if (authVersion < 0 || account.authVersion() != authVersion) {
                throw new BadCredentialsException("Authentication is required");
            }
            // 토큰의 권한 클레임을 신뢰하지 않고 DB의 현재 회원 권한을 사용한다.
            return UsernamePasswordAuthenticationToken.authenticated(account, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name())));
        } catch (AuthException exception) {
            if (exception.getErrorCode() == AuthErrorCode.AUTHENTICATION_UNAVAILABLE) {
                throw new AuthenticationServiceException("Authentication is temporarily unavailable", exception);
            }
            throw exception;
        }
    }
}
