package com.daebangg.sayroam.auth.infrastructure.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Configuration
public class AccessTokenConfig {
    @Bean
    SecretKey accessTokenSigningKey(AccessTokenProperties properties) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(properties.secretBase64() == null ? "" : properties.secretBase64());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("app.auth.access-token.secret-base64 must be Base64-encoded random bytes");
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("app.auth.access-token.secret-base64 requires at least 32 random bytes; no default key is provided");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder accessTokenEncoder(SecretKey accessTokenSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(accessTokenSigningKey));
    }

    @Bean
    JwtDecoder accessTokenDecoder(SecretKey accessTokenSigningKey, AccessTokenProperties properties) {
        var decoder = NimbusJwtDecoder.withSecretKey(accessTokenSigningKey)
                .macAlgorithm(MacAlgorithm.HS256).validateType(false).build();
        OAuth2TokenValidator<Jwt> accessClaims = jwt -> {
            Instant issuedAt = jwt.getIssuedAt();
            Instant expiresAt = jwt.getExpiresAt();
            boolean valid = "at+jwt".equals(jwt.getHeaders().get("typ"))
                    && "access".equals(jwt.getClaimAsString("token_use"))
                    && jwt.getAudience() != null && jwt.getAudience().contains(properties.audience())
                    && jwt.getSubject() != null && jwt.getSubject().matches("[1-9][0-9]{0,18}")
                    && jwt.getId() != null && !jwt.getId().isBlank()
                    && jwt.getClaims().get("auth_ver") instanceof String version
                    && version.matches("0|[1-9][0-9]{0,18}")
                    && issuedAt != null && expiresAt != null && expiresAt.isAfter(issuedAt)
                    && !issuedAt.isAfter(Instant.now().plusSeconds(30));
            return valid ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid access token", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ofSeconds(30)),
                new JwtIssuerValidator(properties.issuer()), accessClaims));
        return decoder;
    }
}
