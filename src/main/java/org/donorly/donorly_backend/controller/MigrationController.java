package org.donorly.donorly_backend.controller;

import org.donorly.donorly_backend.model.User;
import org.donorly.donorly_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ⚠️ TEMPORARY — DELETE THIS FILE AFTER RUNNING THE MIGRATION ONCE.
 *
 * Fixes up existing User documents created back in Phase 2, before real auth
 * existed:
 *   1. Normalizes the `role` field to exactly "ADMIN" or "AMBASSADOR"
 *      (handles lowercase/mixed-case values like "admin", "Admin", etc.)
 *   2. Hashes any password that isn't already a BCrypt hash, using the
 *      same PasswordEncoder bean the real login endpoints use — so there's
 *      no risk of a hashing mismatch.
 *
 * This is idempotent — safe to call more than once. Already-hashed passwords
 * and already-correct roles are left untouched.
 *
 * USAGE:
 *   1. Set MIGRATION_SECRET as an env var (Railway + local) to something
 *      random — do NOT leave it as the placeholder default.
 *   2. Deploy this, then call once:
 *        curl -X POST "https://<your-backend-url>/api/admin/migrate-users?secret=<MIGRATION_SECRET>"
 *   3. Check the response — it lists exactly what was changed per user.
 *   4. Delete this file, remove the matching line in SecurityConfig
 *      (search for "migrate-users"), and redeploy.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class MigrationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${migration.secret:CHANGE_ME}")
    private String migrationSecret;

    @PostMapping("/migrate-users")
    public ResponseEntity<?> migrateUsers(@RequestParam String secret) {
        if (!migrationSecret.equals(secret) || "CHANGE_ME".equals(migrationSecret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid or unset migration secret. Set MIGRATION_SECRET env var first."));
        }

        List<User> allUsers = userRepository.findAll();
        List<Map<String, String>> report = new ArrayList<>();

        for (User user : allUsers) {
            List<String> changes = new ArrayList<>();

            // --- Normalize role ---
            String role = user.getRole();
            String normalizedRole = null;
            if (role != null) {
                String lower = role.trim().toLowerCase();
                if (lower.equals("admin")) normalizedRole = "ADMIN";
                else if (lower.equals("ambassador")) normalizedRole = "AMBASSADOR";
            }
            if (normalizedRole == null) {
                // Couldn't confidently infer — skip role change, flag it instead.
                changes.add("role NOT changed (was: '" + role + "', not recognized — fix manually)");
            } else if (!normalizedRole.equals(role)) {
                user.setRole(normalizedRole);
                changes.add("role: '" + role + "' -> '" + normalizedRole + "'");
            }

            // --- Hash password if not already a BCrypt hash ---
            String pw = user.getPassword();
            boolean alreadyHashed = pw != null && (pw.startsWith("$2a$") || pw.startsWith("$2b$") || pw.startsWith("$2y$"));
            if (pw != null && !pw.isBlank() && !alreadyHashed) {
                user.setPassword(passwordEncoder.encode(pw));
                changes.add("password hashed");
            } else if (alreadyHashed) {
                changes.add("password already hashed, left as-is");
            } else {
                changes.add("password NOT changed (was empty/null)");
            }

            if (!changes.isEmpty()) {
                userRepository.save(user);
                report.add(Map.of("email", user.getEmail() != null ? user.getEmail() : user.getId(), "changes", String.join("; ", changes)));
            }
        }

        return ResponseEntity.ok(Map.of(
                "totalUsers", allUsers.size(),
                "changesApplied", report
        ));
    }
}
