// AuthController.java
package com.example.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        try {
            AuthResponse res = authService.login(req.getUsername(), req.getPassword());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(java.util.Map.of("message", ex.getMessage()));
        }
    }
}
