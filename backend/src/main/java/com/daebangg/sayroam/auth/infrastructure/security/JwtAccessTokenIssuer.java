package com.daebangg.sayroam.auth.infrastructure.security;

import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {
    private final JwtEncoder encoder;
    private final AccessTokenProperties properties;

    public JwtAccessTokenIssuer(JwtEncoder encoder, AccessTokenProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public IssuedAccessToken issue(long userId, long authVersion) {
        if (authVersion < 0) {
            throw new IllegalArgumentException("Authentication version must be nonnegative");
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.issuer())
                .audience(List.of(properties.audience())).subject(Long.toString(userId))
                .issuedAt(now).expiresAt(now.plusSeconds(properties.ttlSeconds()))
                .id(UUID.randomUUID().toString()).claim("token_use", "access")
                .claim("auth_ver", Long.toString(authVersion)).build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("at+jwt").build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(value, properties.ttlSeconds());
    }
}
