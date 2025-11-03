package site.guiro.api.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import site.guiro.api.device.entity.Platform;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank
    private String installId;

    @NotBlank
    private Enum<Platform> platform;

    @NotBlank
    private String model;

    @NotNull
    private String vendorId;
}
