package site.guiro.api.device.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class InstallIdHasher {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder();

    public String hash(String installId) {
        String normalized = installId.trim();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return ENCODER.encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}