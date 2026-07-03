package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.AppUser;
import org.donorly.donorly_backend.repository.AmbassadorRepository;
import org.donorly.donorly_backend.repository.AppUserRepository;
import org.donorly.donorly_backend.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final AmbassadorRepository ambassadorRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        AppUser user = appUserRepository.findByEmailAddress(email).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not an admin account"));
        }
        if (!"active".equals(user.getUserStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Account is inactive"));
        }

        String token = jwtUtil.generateToken(user.getUserId().toString(), user.getRole(), null);
        user.setLastLoginAt(Instant.now());
        appUserRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", user.getRole(),
                "name", user.getFullName() == null ? "Admin" : user.getFullName()
        ));
    }

    @PostMapping("/ambassador/login")
    public ResponseEntity<?> ambassadorLogin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        AppUser user = appUserRepository.findByEmailAddress(email).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
        if (!"AMBASSADOR".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not an ambassador account"));
        }
        if (!"active".equals(user.getUserStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Account is inactive"));
        }

        String ambassadorIdStr = user.getAmbassadorId() == null ? null : user.getAmbassadorId().toString();
        String token = jwtUtil.generateToken(user.getUserId().toString(), user.getRole(), ambassadorIdStr);
        String jti = jwtUtil.extractJti(token);

        user.setActiveSessionToken(jti);
        user.setLastLoginAt(Instant.now());
        appUserRepository.save(user);

        String ambassadorCode = user.getAmbassadorId() != null
                ? ambassadorRepository.findById(user.getAmbassadorId()).map(a -> a.getAmbassadorCode()).orElse(null)
                : null;

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", user.getRole(),
                "ambassadorId", ambassadorIdStr == null ? "" : ambassadorIdStr,
                "name", user.getFullName() == null ? "Ambassador" : user.getFullName(),
                "ambassadorCode", ambassadorCode == null ? "" : ambassadorCode
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "No token provided"));
        }
        String token = authHeader.substring(7);
        try {
            UUID userId = UUID.fromString(jwtUtil.extractUserId(token));
            appUserRepository.findById(userId).ifPresent(user -> {
                user.setActiveSessionToken(null);
                appUserRepository.save(user);
            });
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
