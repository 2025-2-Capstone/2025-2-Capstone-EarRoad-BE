package site.guiro.api.auth.service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import site.guiro.api.config.security.JwtProperties;
import site.guiro.api.device.entity.Device;

@Component
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public TokenPair generateTokens(Device device) {
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(jwtProperties.getAccessTokenTtl());
        Instant refreshExpiresAt = now.plus(jwtProperties.getRefreshTokenTtl());

        JwtClaimsSet accessClaims = baseClaims(device, now, accessExpiresAt)
                .claim("token_type", "access")
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(accessClaims)).getTokenValue();

        JwtClaimsSet refreshClaims = baseClaims(device, now, refreshExpiresAt)
                .claim("token_type", "refresh")
                .build();
        String refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(refreshClaims)).getTokenValue();

        return new TokenPair(accessToken, refreshToken, now, accessExpiresAt, refreshExpiresAt);
    }

    private JwtClaimsSet.Builder baseClaims(Device device, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject("device:" + device.getUuid())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("device_id", device.getUuid());

        if (device.getPlatform() != null) {
            builder.claim("platform", device.getPlatform().name());
        }

        return builder;
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            Instant issuedAt,
            Instant accessTokenExpiresAt,
            Instant refreshTokenExpiresAt
    ) {
        public long accessTokenExpiresInSeconds() {
            return Math.max(0, java.time.Duration.between(issuedAt, accessTokenExpiresAt).getSeconds());
        }

        public long refreshTokenExpiresInSeconds() {
            return Math.max(0, java.time.Duration.between(issuedAt, refreshTokenExpiresAt).getSeconds());
        }
    }
}