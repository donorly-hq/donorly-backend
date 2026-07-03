package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.AppUser;
import org.donorly.donorly_backend.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * TEMPORARY — bootstraps the first admin account on a fresh database
 * where no users exist yet (so normal authenticated endpoints are
 * unreachable). Matches the Phase 4 pattern: use once, then DELETE
 * this file and redeploy. Never leave this permitAll endpoint live.
 */
@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
public class SetupController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/first-admin")
    public String createFirstAdmin(@RequestParam String email, @RequestParam String password) {
        if (appUserRepository.findByEmailAddress(email).isPresent()) {
            return "User already exists.";
        }
        AppUser admin = new AppUser();
        admin.setEmailAddress(email);
        admin.setFullName("Admin");
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setUserStatus("active");
        admin.setRole("ADMIN");
        appUserRepository.save(admin);
        return "Admin created: " + email;
    }
}
