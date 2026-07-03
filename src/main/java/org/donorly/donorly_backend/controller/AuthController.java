package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.User;
import org.donorly.donorly_backend.repository.UserRepository;
import org.donorly.donorly_backend.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not an admin account");
        }
        if (!user.isActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account is inactive");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole(), null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("token", token, "role", user.getRole()));
    }

    @PostMapping("/ambassador/login")
    public ResponseEntity<?> ambassadorLogin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
        if (!"AMBASSADOR".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not an ambassador account");
        }
        if (!user.isActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account is inactive");
        }
        if (user.getActiveSessionToken() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Already logged in elsewhere");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.getAmbassadorId());
        String jti = jwtUtil.extractJti(token);

        user.setActiveSessionToken(jti);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("token", token, "role", user.getRole(),
                "ambassadorId", user.getAmbassadorId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("No token provided");
        }
        String token = authHeader.substring(7);
        try {
            String userId = jwtUtil.extractUserId(token);
            userRepository.findById(userId).ifPresent(user -> {
                user.setActiveSessionToken(null);
                userRepository.save(user);
            });
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
