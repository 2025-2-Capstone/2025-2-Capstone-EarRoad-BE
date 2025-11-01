package site.guiro.api.device.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String access_toke;
    private String refresh_token;
    private int expires_in; // 60분
    private int refresh_expires_in; //30일

}
