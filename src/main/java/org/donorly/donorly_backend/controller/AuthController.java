package org.donorly.donorly_backend.controller;

import org.donorly.donorly_backend.model.Ambassador;
import org.donorly.donorly_backend.model.User;
import org.donorly.donorly_backend.repository.AmbassadorRepository;
import org.donorly.donorly_backend.repository.UserRepository;
import org.donorly.donorly_backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AmbassadorRepository ambassadorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    public record LoginRequest(String email, String password) {}

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest req) {
        Optional<User> userOpt = userRepository.findByEmailAndRole(req.email(), "ADMIN");
        if (userOpt.isEmpty() || !passwordEncoder.matches(req.password(), userOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid admin credentials"));
        }
        User user = userOpt.get();
        if (user.getActive() != null && !user.getActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Account is inactive"));
        }

        String token = jwtUtil.generateToken(user.getId(), "ADMIN", null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", user.getId(),
                "name", user.getName(),
                "role", "ADMIN"
        ));
    }

    @PostMapping("/ambassador/login")
    public ResponseEntity<?> ambassadorLogin(@RequestBody LoginRequest req) {
        Optional<User> userOpt = userRepository.findByEmailAndRole(req.email(), "AMBASSADOR");
        if (userOpt.isEmpty() || !passwordEncoder.matches(req.password(), userOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid ambassador credentials"));
        }
        User user = userOpt.get();
        if (user.getActive() != null && !user.getActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Account is inactive"));
        }

        // Single-session enforcement: block a second concurrent login rather
        // than silently kicking the first session out.
        if (user.getActiveSessionToken() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "This account is already logged in elsewhere. Log out there first."));
        }

        String token = jwtUtil.generateToken(user.getId(), "AMBASSADOR", user.getAmbassadorId());
        String jti = jwtUtil.getJti(token);

        user.setActiveSessionToken(jti);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String ambStatus = null;
        String ambCode = null;
        if (user.getAmbassadorId() != null) {
            Optional<Ambassador> amb = ambassadorRepository.findById(user.getAmbassadorId());
            if (amb.isPresent()) {
                ambStatus = amb.get().getStatus();
                ambCode = amb.get().getCode();
            }
        }

        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", user.getId(),
                "name", user.getName(),
                "role", "AMBASSADOR",
                "ambassadorId", user.getAmbassadorId() != null ? user.getAmbassadorId() : "",
                "ambassadorStatus", ambStatus != null ? ambStatus : "",
                "ambassadorCode", ambCode != null ? ambCode : ""
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing token"));
        }
        String token = authHeader.substring(7);
        String userId = jwtUtil.parseClaims(token).getSubject();

        userRepository.findById(userId).ifPresent(user -> {
            user.setActiveSessionToken(null);
            userRepository.save(user);
        });

        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
