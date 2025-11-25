package site.guiro.api.common.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HealthCheckController {

    @GetMapping("/")
    public ResponseEntity<String> healthCheck() {
        // ALB Health Check용으로 아주 단순한 응답
        log.info("[GET /] health check request");
        return ResponseEntity.ok("OK");
    }
}