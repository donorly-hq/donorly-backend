package org.donorly.donorly_backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.AppUser;
import org.donorly.donorly_backend.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String PORTAL_URL = "https://donorly-hq.github.io/donorly-portal/";
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public List<AppUser> getAll() {
        return appUserRepository.findAll();
    }

    public Optional<AppUser> getById(UUID id) {
        return appUserRepository.findById(id);
    }

    public Optional<AppUser> getByEmail(String email) {
        return appUserRepository.findByEmailAddress(email);
    }

    public AppUser save(AppUser user) {
        return appUserRepository.save(user);
    }

    /**
     * Creates a new ambassador/admin login: generates a temporary
     * password, hashes it for storage, and emails the plaintext
     * version to the user as their welcome email. This is the method
     * to call when an admin adds a new ambassador in the UI.
     */
    public java.util.Map<String, Object> createWithWelcomeEmail(AppUser user) {
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setUserStatus("active");
        AppUser saved = appUserRepository.save(user);

        boolean emailSent = true;
        try {
            emailService.sendAmbassadorWelcomeEmail(
                    saved.getEmailAddress(),
                    saved.getFullName(),
                    saved.getEmailAddress(),
                    temporaryPassword,
                    PORTAL_URL
            );
        } catch (Exception e) {
            // Don't let an email failure block account creation — e.g.
            // Resend's sandbox mode only delivers to the verified
            // signup address until a domain is verified. Log and
            // continue; the account still works for login. Returning
            // the temp password below means the admin isn't locked
            // out of knowing it even when the email doesn't arrive.
            emailSent = false;
            System.err.println("Welcome email failed to send for " + saved.getEmailAddress() + ": " + e.getMessage());
        }

        return java.util.Map.of(
                "user", saved,
                "temporaryPassword", temporaryPassword,
                "emailSent", emailSent
        );
    }

    public AppUser update(UUID id, AppUser updated) {
        updated.setUserId(id);
        return appUserRepository.save(updated);
    }

    public void delete(UUID id) {
        appUserRepository.deleteById(id);
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
