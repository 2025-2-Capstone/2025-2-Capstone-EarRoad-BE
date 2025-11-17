package site.guiro.api.config.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

    private final Resource privateKeyResource;
    private final Resource publicKeyResource;
    private final JwtProperties jwtProperties;

    public JwtConfig(
            @Value("${guiro.jwt.private-key-location}") Resource privateKeyResource,
            @Value("${guiro.jwt.public-key-location}") Resource publicKeyResource,
            JwtProperties jwtProperties
    ) {
        this.privateKeyResource = privateKeyResource;
        this.publicKeyResource = publicKeyResource;
        this.jwtProperties = jwtProperties;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        try {
            RSAKey rsaKey = new RSAKey.Builder(loadPublicKey())
                    .privateKey(loadPrivateKey())
                    .build();
            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
            return new NimbusJwtEncoder(jwkSource);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to initialize JWT encoder", e);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(loadPublicKey()).build();
            OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer());
            decoder.setJwtValidator(validator);
            return decoder;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to initialize JWT decoder", e);
        }
    }

    private RSAPublicKey loadPublicKey() throws IOException, GeneralSecurityException {
        String key = readKey(publicKeyResource, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    private RSAPrivateKey loadPrivateKey() throws IOException, GeneralSecurityException {
        String key = readKey(privateKeyResource, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    private String readKey(Resource resource, String beginMarker, String endMarker) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            String key = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return key
                    .replace(beginMarker, "")
                    .replace(endMarker, "")
                    .replaceAll("\\s", "");
        }
    }
}
