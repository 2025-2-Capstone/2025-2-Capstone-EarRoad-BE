package site.guiro.api.common.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/")
    public ResponseEntity<String> healthCheck() {
        // ALB Health Check용으로 아주 단순한 응답
        return ResponseEntity.ok("OK");
    }
}