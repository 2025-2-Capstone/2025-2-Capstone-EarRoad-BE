package site.guiro.api.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.guiro.api.auth.dto.LoginRequest;
import site.guiro.api.auth.dto.LoginResponse;
import site.guiro.api.device.entity.Device;
import site.guiro.api.device.repository.DeviceRepository;
import site.guiro.api.device.service.InstallIdHasher;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final DeviceRepository deviceRepository;
    private final InstallIdHasher installIdHasher;
    private final JwtTokenService jwtTokenService;

    public LoginResponse login(LoginRequest request) {
        String hashedInstallId = installIdHasher.hash(request.getInstallId());
        Device device = deviceRepository.findByUuid(hashedInstallId)
                .map(existing -> {
                    existing.markLastSeen(request.getPlatform());
                    return existing;
                })
                .orElseGet(() -> deviceRepository.save(Device.create(hashedInstallId, request.getPlatform())));

        JwtTokenService.TokenPair tokenPair = jwtTokenService.generateTokens(device);

        return LoginResponse.builder()
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .expiresIn(Math.toIntExact(tokenPair.accessTokenExpiresInSeconds()))
                .refreshExpiresIn(Math.toIntExact(tokenPair.refreshTokenExpiresInSeconds()))
                .build();
    }
}