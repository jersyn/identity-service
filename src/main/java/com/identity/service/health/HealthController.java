package com.identity.service.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {

    @GetMapping("/health")
    ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
